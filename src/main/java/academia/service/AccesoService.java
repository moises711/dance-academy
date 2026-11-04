package academia.service;

import academia.db.Database;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class AccesoService {
    public enum Rol { ADMIN, OPERADOR, CONSULTA }

    public static final class Sesion implements AutoCloseable {
        private final long usuario;
        private final Instant vence = Instant.now().plusSeconds(8 * 60 * 60);
        private volatile boolean cerrada;

        private Sesion(long usuario) {
            this.usuario = usuario;
        }

        long verificar(Connection connection, Rol minimo) throws SQLException {
            if (cerrada || !Instant.now().isBefore(vence)) {
                throw new SecurityException("Sesión cerrada o vencida");
            }
            try (var statement = Sql.prepare(connection,
                    "SELECT rol FROM usuarios WHERE id=? AND activo=1", usuario);
                 var row = statement.executeQuery()) {
                if (!row.next() || Rol.valueOf(row.getString(1)).ordinal() > minimo.ordinal()) {
                    throw new SecurityException("Permisos insuficientes");
                }
                return usuario;
            }
        }

        @Override
        public void close() {
            cerrada = true;
        }
    }

    public long crearPrimerAdministrador(String nombre, char[] clave) throws SQLException {
        Credencial credencial = crearCredencial(clave);
        return Sql.transaction(c -> {
            long id = Sql.id(c, """
                    INSERT INTO usuarios(nombre,sal,clave_hash,rol)
                    SELECT ?,?,?,'ADMIN' WHERE NOT EXISTS (SELECT 1 FROM usuarios) RETURNING id
                    """, nombreUsuario(nombre), credencial.sal(), credencial.hash());
            Sql.auditar(c, id, "acceso.inicializar", id);
            return id;
        });
    }

    public Sesion autenticar(String nombre, char[] clave) throws SQLException {
        return autenticarLocal(nombre, clave);
    }

    private static synchronized Sesion autenticarLocal(String nombre, char[] clave) throws SQLException {
        String usuario = nombreUsuario(nombre);
        if (clave == null || clave.length > 128) {
            throw new SecurityException("Credenciales inválidas");
        }
        try (var connection = Database.open();
             var statement = Sql.prepare(connection,
                     "SELECT id,sal,clave_hash,activo FROM usuarios WHERE nombre=?", usuario);
             var row = statement.executeQuery()) {
            boolean existe = row.next();
            byte[] sal = existe ? HexFormat.of().parseHex(row.getString("sal")) : new byte[16];
            byte[] esperado = existe ? HexFormat.of().parseHex(row.getString("clave_hash")) : new byte[32];
            byte[] calculado = derivar(clave, sal);
            boolean coincide = MessageDigest.isEqual(esperado, calculado);
            Arrays.fill(calculado, (byte) 0);
            if (!coincide || !existe || row.getInt("activo") != 1) {
                esperarTrasFallo();
                if (existe) {
                    Sql.auditar(connection, row.getLong("id"), "acceso.autenticarFallido", null);
                }
                throw new SecurityException("Credenciales inválidas");
            }
            return new Sesion(row.getLong("id"));
        }
    }

    private static void esperarTrasFallo() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public long crearUsuario(Sesion sesion, String nombre, char[] clave, Rol rol) throws SQLException {
        var credencial = crearCredencial(clave);
        return Sql.write(sesion, Rol.ADMIN, "acceso.crearUsuario", c -> Sql.id(c, """
                INSERT INTO usuarios(nombre,sal,clave_hash,rol) VALUES(?,?,?,?) RETURNING id
                """, nombreUsuario(nombre), credencial.sal(), credencial.hash(), Objects.requireNonNull(rol).name()));
    }

    public void desactivar(Sesion sesion, long id) throws SQLException {
        Sql.write(sesion, Rol.ADMIN, "acceso.desactivar", c -> {
            Sql.change(c, """
                    UPDATE usuarios SET activo=0 WHERE id=? AND activo=1 AND
                      (rol<>'ADMIN' OR EXISTS(SELECT 1 FROM usuarios WHERE rol='ADMIN' AND activo=1 AND id<>?))
                    """, id, id);
            return id;
        });
    }

    public java.util.List<java.util.Map<String, Object>> auditoria(Sesion sesion, long antesDe) throws SQLException {
        return Sql.read(sesion, Rol.ADMIN, """
                SELECT a.id, u.nombre AS usuario, a.operacion, a.entidad_id, a.fecha
                FROM auditoria a JOIN usuarios u ON u.id=a.usuario_id
                WHERE a.id < ? ORDER BY a.id DESC LIMIT 200
                """, antesDe);
    }

    private record Credencial(String sal, String hash) {}

    private static Credencial crearCredencial(char[] clave) {
        if (clave == null || clave.length < 12 || clave.length > 128) {
            throw new IllegalArgumentException("La contraseña debe tener entre 12 y 128 caracteres");
        }
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        byte[] hash = derivar(clave, sal);
        try {
            return new Credencial(HexFormat.of().formatHex(sal), HexFormat.of().formatHex(hash));
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    private static byte[] derivar(char[] clave, byte[] sal) {
        var spec = new PBEKeySpec(clave, sal, 600_000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("No se pudo proteger la contraseña", failure);
        } finally {
            spec.clearPassword();
        }
    }

    private static String nombreUsuario(String nombre) {
        String normalizado = Sql.required(nombre, 40).toLowerCase(Locale.ROOT);
        if (!normalizado.matches("[a-z0-9][a-z0-9._-]{2,39}")) {
            throw new IllegalArgumentException("Nombre de usuario inválido");
        }
        return normalizado;
    }

    public static void main(String[] args) throws Exception {
        var consola = System.console();
        if (consola == null || args.length != 1) {
            throw new IllegalArgumentException("Ejecuta AccesoService USUARIO desde una terminal");
        }
        char[] clave = consola.readPassword("Contraseña inicial: ");
        if (clave == null) {
            return;
        }
        try {
            Database.initialize();
            new AccesoService().crearPrimerAdministrador(args[0], clave);
            consola.printf("Administrador creado.%n");
        } finally {
            Arrays.fill(clave, '\0');
        }
    }
}
