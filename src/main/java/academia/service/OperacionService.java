package academia.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class OperacionService {
    private final AccesoService.Sesion sesion;

    public OperacionService(AccesoService.Sesion sesion) {
        this.sesion = java.util.Objects.requireNonNull(sesion);
    }

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public long crearInstructor(String nombre, String dni, long tarifa) throws SQLException {
        if (dni != null && !dni.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("DNI inválido");
        }
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "operacion.crearInstructor", c -> Sql.id(c, """
                INSERT INTO instructores(nombre,dni,tarifa_hora_centimos) VALUES(?,?,?) RETURNING id
                """, Sql.required(nombre, 150), dni, tarifa));
    }

    public long crearHorario(long instructor, String clase, int dia, LocalTime inicio, LocalTime fin, Integer capacidad)
            throws SQLException {
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "operacion.crearHorario", c -> Sql.id(c, """
                INSERT INTO horarios(instructor_id,nombre_clase,dia_semana,hora_inicio,hora_fin,capacidad)
                SELECT id,?,?,?,?,? FROM instructores WHERE id=? AND activo=1 RETURNING id
                """, Sql.required(clase, 100), dia, inicio.format(HORA), fin.format(HORA), capacidad, instructor));
    }

    public long registrarAsistenciaInstructor(long instructor, LocalDate dia, LocalTime entrada, LocalTime salida)
            throws SQLException {
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "operacion.registrarAsistenciaInstructor", c -> Sql.id(c, """
                INSERT INTO asistencias_instructores(instructor_id,fecha,hora_ingreso,hora_salida)
                SELECT id,?,?,? FROM instructores WHERE id=? AND activo=1 RETURNING id
                """, dia.toString(), entrada.format(HORA), salida == null ? null : salida.format(HORA), instructor));
    }

    public long crearRemuneracion(long instructor, LocalDate desde, LocalDate hasta, long importe) throws SQLException {
        return Sql.write(sesion, AccesoService.Rol.ADMIN, "operacion.crearRemuneracion", c -> Sql.id(c, """
                INSERT INTO remuneraciones(instructor_id,periodo_inicio,periodo_fin,monto_centimos)
                VALUES(?,?,?,?) RETURNING id
                """, instructor, desde.toString(), hasta.toString(), importe));
    }

    public long registrarPeso(long alumno, LocalDate fecha, double kilos) throws SQLException {
        if (!Double.isFinite(kilos) || kilos <= 0 || kilos >= 700) {
            throw new IllegalArgumentException("Peso inválido");
        }
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "operacion.registrarPeso", c -> Sql.id(c, """
                INSERT INTO mediciones_peso(alumno_id,fecha,peso_kg)
                SELECT estudiante_id,?,? FROM alumnos WHERE estudiante_id=? AND estado='activo'
                RETURNING id
                """, fecha.toString(), kilos, alumno));
    }

    public void configurarIntervalos(int pesoDias, int avisoDias) throws SQLException {
        if (pesoDias < 1 || pesoDias > 365 || avisoDias < 0 || avisoDias > 365) {
            throw new IllegalArgumentException("Intervalo fuera de rango");
        }
        Sql.write(sesion, AccesoService.Rol.ADMIN, "operacion.configurarIntervalos", c -> {
            Sql.change(c, "UPDATE parametros SET valor=? WHERE clave='intervalo_peso_dias'", pesoDias);
            Sql.change(c, "UPDATE parametros SET valor=? WHERE clave='alerta_vencimiento_dias'", avisoDias);
            return null;
        });
    }

    public enum Reporte {
        DASHBOARD("v_dashboard"), ALERTAS("v_alertas_vencimientos"), FINANZAS("v_finanzas_diarias"),
        CAJAS("v_balance_cajas"), PESO("v_control_peso"), MATRICULAS("v_matriculas");

        private final String vista;

        Reporte(String vista) {
            this.vista = vista;
        }
    }

    public List<Map<String, Object>> consultar(Reporte reporte) throws SQLException {
        return Sql.read(sesion, switch (reporte) {
            case DASHBOARD -> AccesoService.Rol.CONSULTA;
            case FINANZAS, CAJAS -> AccesoService.Rol.ADMIN;
            default -> AccesoService.Rol.OPERADOR;
        }, "SELECT * FROM " + reporte.vista);
    }
}
