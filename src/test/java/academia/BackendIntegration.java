package academia;

import academia.db.Database;
import academia.service.AccesoService;
import academia.service.AlumnoService;
import academia.service.FinanzasService;
import academia.service.MatriculaService;
import academia.service.OperacionService;
import academia.service.SorteoService;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public final class BackendIntegration {
    public static void main(String[] args) throws Exception {
        if (Files.exists(Database.FILE)) {
            throw new IllegalStateException("Ejecuta la prueba desde una carpeta temporal vacía");
        }
        Database.initialize();
        if (Files.getFileStore(Database.FILE).supportsFileAttributeView("posix")) {
            comprobar(java.nio.file.attribute.PosixFilePermissions.toString(
                    Files.getPosixFilePermissions(Database.FILE)).equals("rw-------"));
            comprobar(java.nio.file.attribute.PosixFilePermissions.toString(
                    Files.getPosixFilePermissions(Database.FILE.getParent())).equals("rwx------"));
        }
        var acceso = new AccesoService();
        char[] clave = "contraseña-prueba-larga".toCharArray();
        acceso.crearPrimerAdministrador("admin", clave);
        long inicioFallo = System.nanoTime();
        try {
            acceso.autenticar("admin", "incorrecta".toCharArray());
            throw new AssertionError("Se aceptó una contraseña incorrecta");
        } catch (SecurityException expected) {
            // La autenticación rechaza credenciales incorrectas.
        }
        comprobar(System.nanoTime() - inicioFallo >= 1_000_000_000L);
        var sesion = acceso.autenticar("admin", clave);
        comprobar(acceso.auditoria(sesion, Long.MAX_VALUE).stream()
                .anyMatch(fila -> "acceso.autenticarFallido".equals(fila.get("operacion"))));
        long usuario = acceso.crearUsuario(sesion, "consulta", clave, AccesoService.Rol.CONSULTA);
        var lectura = acceso.autenticar("consulta", clave);
        new OperacionService(lectura).consultar(OperacionService.Reporte.DASHBOARD);
        try {
            new AlumnoService(lectura).registrar("Denegado", null, null, null);
            throw new AssertionError("Consulta pudo escribir");
        } catch (SecurityException expected) {
            // El rol de consulta solo tiene acceso al resumen.
        }
        acceso.desactivar(sesion, usuario);
        try {
            new OperacionService(lectura).consultar(OperacionService.Reporte.DASHBOARD);
            throw new AssertionError("Se aceptó la sesión de un usuario desactivado");
        } catch (SecurityException expected) {
            // Los permisos se verifican en cada operación.
        }
        var alumnos = new AlumnoService(sesion);
        long alumno = alumnos.registrar("Ana", null, null, "01234567");
        alumnos.editar(alumno, "Ana Pérez", "ana@example.com", null, "01234567");
        comprobar(alumnos.historial(alumno).size() == 1);
        String codigo = (String) alumnos.buscar("Ana").getFirst().get("codigo");
        try {
            alumnos.registrar("Duplicado", null, null, "01234567");
            throw new AssertionError("Se aceptó el DNI duplicado");
        } catch (SQLException expected) {
            comprobar(alumnos.buscar("").size() == 1);
            try (var c = Database.open(); var s = c.createStatement();
                 var rows = s.executeQuery("SELECT count(*) FROM estudiantes")) {
                comprobar(rows.next() && rows.getInt(1) == 1);
            }
        }

        var matriculas = new MatriculaService(sesion);
        var operacion = new OperacionService(sesion);
        LocalDate dia = LocalDate.of(2026, 1, 1);
        long plan = matriculas.crearPlan("Sesiones", 30, 1, 12000);
        long instructor = operacion.crearInstructor("Luis", null, 2000);
        long horario = operacion.crearHorario(instructor, "Baile", 1, LocalTime.of(10, 0), LocalTime.of(11, 0), 20);
        long matricula = matriculas.matricular(alumno, plan, dia, null, horario);
        matriculas.congelar(matricula, dia, "Viaje");
        matriculas.reactivar(matricula, dia.plusDays(1));
        var hora = LocalTime.of(10, 0);
        long asistencia = matriculas.registrarIngreso(matricula, codigo, null, dia.plusDays(1), hora);
        comprobar(new MatriculaService(sesion).matriculasSinClase().isEmpty());
        comprobar(asistencia == matriculas.registrarIngreso(matricula, codigo, null, dia.plusDays(1), hora));
        try {
            matriculas.registrarIngreso(matricula, codigo, null, dia.plusDays(1), hora.plusHours(1));
            throw new AssertionError("Se excedió el número de sesiones");
        } catch (IllegalStateException expected) {
            // El único ingreso del plan ya se consumió.
        }

        var finanzas = new FinanzasService(sesion);
        long caja = finanzas.abrirCaja("Administración", 1000);
        var solicitud = java.util.UUID.randomUUID();
        long pago;
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Long> cobrar =
                    () -> finanzas.cobrar(caja, matricula, "sesiones", 12000, "efectivo", null, solicitud);
            var primero = executor.submit(cobrar);
            var segundo = executor.submit(cobrar);
            pago = primero.get();
            comprobar(pago == segundo.get());
        }
        try {
            finanzas.cobrar(caja, matricula, "sesiones", 13000, "efectivo", null, solicitud);
            throw new AssertionError("Se reutilizó la clave con otro importe");
        } catch (IllegalStateException expected) {
            // Una clave corresponde a una solicitud inmutable.
        }
        try (var c = Database.open(); var statement = c.createStatement();
             var rows = statement.executeQuery("SELECT (SELECT count(*) FROM pagos),"
                     + "(SELECT count(*) FROM movimientos_caja)")) {
            comprobar(rows.next() && rows.getInt(1) == 1 && rows.getInt(2) == 1);
        }
        finanzas.emitirComprobante(pago, "Ana", null);
        finanzas.devolver(caja, pago);
        var balance = operacion.consultar(OperacionService.Reporte.CAJAS).getFirst();
        comprobar(((Number) balance.get("efectivo_esperado_centimos")).longValue() == 1000);
        operacion.registrarPeso(alumno, dia, 62.5);
        operacion.crearHorario(instructor, "Baile", 1, hora, hora.plusHours(1), 20);
        operacion.registrarAsistenciaInstructor(instructor, dia, hora, hora.plusHours(1));
        long remuneracion = operacion.crearRemuneracion(instructor, dia, dia.plusDays(5), 2000);
        finanzas.pagarRemuneracion(caja, remuneracion, "yape");
        finanzas.cerrarCaja(caja, 1000);
        try {
            finanzas.cobrar(caja, matricula, "sesiones", 12000, "efectivo", null, java.util.UUID.randomUUID());
            throw new AssertionError("Se cobró en una caja cerrada");
        } catch (IllegalStateException expected) {
            // Una caja cerrada no admite nuevos movimientos.
        }

        var sorteos = new SorteoService(sesion);
        long sorteo = sorteos.crear("Sorteo", "Premio", dia.plusDays(3), dia, dia.plusDays(2), 1, true);
        sorteos.inscribir(sorteo, alumno);
        comprobar(sorteos.elegir(sorteo, 1).getFirst() == alumno);
        comprobar(acceso.auditoria(sesion, Long.MAX_VALUE).stream().anyMatch(row ->
                "admin".equals(row.get("usuario")) && "finanzas.cobrar".equals(row.get("operacion"))));
        sesion.close();
        System.out.println("OK: servicios Java con SQLite, transacciones y reintento de asistencia");
    }

    private static void comprobar(boolean condicion) {
        if (!condicion) {
            throw new AssertionError("El resultado del servicio no coincide con lo esperado");
        }
    }
}
