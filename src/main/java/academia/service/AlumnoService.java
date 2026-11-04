package academia.service;


import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public final class AlumnoService {
    private final AccesoService.Sesion sesion;

    public AlumnoService(AccesoService.Sesion sesion) {
        this.sesion = java.util.Objects.requireNonNull(sesion);
    }

    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final Pattern TELEFONO = Pattern.compile("\\+?[0-9]{7,15}");
    private static final Pattern DNI = Pattern.compile("[0-9]{8}");

    public long registrar(String nombre, String email, String telefono, String dni) throws SQLException {
        nombre = normalizar(nombre);
        email = normalizar(email);
        telefono = normalizar(telefono);
        dni = normalizar(dni);
        if (nombre == null || nombre.length() > 150) {
            throw new IllegalArgumentException("El nombre debe tener entre 1 y 150 caracteres");
        }
        validarFormato(email, EMAIL, 254, "Correo inválido");
        validarFormato(telefono, TELEFONO, 16, "Teléfono inválido");
        validarFormato(dni, DNI, 8, "El DNI debe contener ocho dígitos");

        String nombreValido = nombre;
        String correo = email;
        String numero = telefono;
        String documento = dni;
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "alumno.registrar", connection -> {
            long id = Sql.id(connection, """
                    INSERT INTO estudiantes(nombre,email,telefono) VALUES (?,?,?) RETURNING id
                    """, nombreValido, correo, numero);
            int inicio = ThreadLocalRandom.current().nextInt(10_000);
            return Sql.id(connection, """
                    WITH RECURSIVE candidatos(paso) AS (
                        SELECT 0 UNION ALL SELECT paso+1 FROM candidatos WHERE paso<9999
                    )
                    INSERT INTO alumnos(estudiante_id,dni,codigo)
                    SELECT ?,?,printf('%04d',(?+paso)%10000) FROM candidatos
                    WHERE NOT EXISTS (
                        SELECT 1 FROM alumnos WHERE codigo=printf('%04d',(?+paso)%10000)
                    )
                    LIMIT 1 RETURNING estudiante_id
                    """, id, documento, inicio, inicio);
        });
    }

    public java.util.List<java.util.Map<String, Object>> buscar(String texto) throws SQLException {
        var consulta = texto == null ? "" : texto.strip();
        return Sql.read(sesion, AccesoService.Rol.OPERADOR, """
                SELECT e.*, a.dni, a.estado, a.codigo
                FROM estudiantes e JOIN alumnos a ON a.estudiante_id=e.id
                WHERE ? = '' OR instr(lower(e.nombre), lower(?)) > 0 OR ? IN (a.dni, a.codigo)
                ORDER BY e.nombre, e.id LIMIT 200
                """, consulta, consulta, consulta);
    }

    public void editar(long id, String nombre, String email, String telefono, String dni) throws SQLException {
        var nombreValido = Sql.required(nombre, 150);
        var correo = normalizar(email);
        var numero = normalizar(telefono);
        var documento = normalizar(dni);
        validarFormato(correo, EMAIL, 254, "Correo inválido");
        validarFormato(numero, TELEFONO, 16, "Teléfono inválido");
        validarFormato(documento, DNI, 8, "El DNI debe contener ocho dígitos");
        Sql.write(sesion, AccesoService.Rol.OPERADOR, "alumno.editar", connection -> {
            Sql.change(connection, "UPDATE estudiantes SET nombre=?, email=?, telefono=? WHERE id=?",
                    nombreValido, correo, numero, id);
            Sql.change(connection, "UPDATE alumnos SET dni=? WHERE estudiante_id=?", documento, id);
            Sql.change(connection, "INSERT INTO historial_alumnos(alumno_id,evento) VALUES(?,'edicion')", id);
            return id;
        });
    }

    public void cambiarEstado(long id, String estado) throws SQLException {
        var nuevo = Sql.required(estado, 8);
        if (!java.util.Set.of("activo", "inactivo", "retirado").contains(nuevo)) {
            throw new IllegalArgumentException("Estado inválido");
        }
        Sql.write(sesion, AccesoService.Rol.OPERADOR, "alumno.cambiarEstado", connection -> {
            Sql.change(connection, "UPDATE alumnos SET estado=? WHERE estudiante_id=? AND estado<>?",
                    nuevo, id, nuevo);
            Sql.change(connection, "INSERT INTO historial_alumnos(alumno_id,evento,detalle) VALUES(?,'estado',?)",
                    id, nuevo);
            return id;
        });
    }

    public java.util.List<java.util.Map<String, Object>> historial(long id) throws SQLException {
        return Sql.read(sesion, AccesoService.Rol.OPERADOR, "SELECT * FROM historial_alumnos WHERE alumno_id=? ORDER BY id DESC", id);
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private static void validarFormato(String valor, Pattern patron, int maximo, String mensaje) {
        if (valor != null && (valor.length() > maximo || !patron.matcher(valor).matches())) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
