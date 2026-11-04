package academia.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public final class MatriculaService {
    private final AccesoService.Sesion sesion;

    public MatriculaService(AccesoService.Sesion sesion) {
        this.sesion = java.util.Objects.requireNonNull(sesion);
    }

    public long crearPlan(String nombre, int dias, Integer sesiones, long precio) throws SQLException {
        return Sql.write(sesion, AccesoService.Rol.ADMIN, "matricula.crearPlan", c -> Sql.id(c, """
                INSERT INTO planes(nombre, modalidad, duracion_dias, sesiones, precio_centimos)
                VALUES(?,?,?,?,?) RETURNING id
                """, Sql.required(nombre, 100), sesiones == null ? "mensualidad" : "sesiones",
                dias, sesiones, precio));
    }

    public void activarPlan(long plan, boolean activo) throws SQLException {
        Sql.write(sesion, AccesoService.Rol.ADMIN, "matricula.activarPlan", c -> {
            Sql.change(c, "UPDATE planes SET activo=? WHERE id=?", activo ? 1 : 0, plan);
            return plan;
        });
    }

    public long matricular(long alumno, long plan, LocalDate inicio, Long renovacion) throws SQLException {
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "matricula.matricular", c -> Sql.id(c, """
                INSERT INTO matriculas(alumno_id,plan_id,fecha_inicio,fecha_vencimiento,
                                       precio_centimos,sesiones_contratadas,renovacion_de)
                SELECT a.estudiante_id,p.id,?,date(?, '+' || (p.duracion_dias-1) || ' days'),
                       p.precio_centimos,p.sesiones,?
                FROM alumnos a JOIN planes p ON p.id=?
                WHERE a.estudiante_id=? AND a.estado='activo' AND p.activo=1
                  AND (? IS NULL OR EXISTS (
                    SELECT 1 FROM matriculas m WHERE m.id=? AND m.alumno_id=a.estudiante_id
                      AND m.estado IN ('activa','vencida') AND m.fecha_vencimiento < ?
                  ))
                RETURNING id
                """, inicio.toString(), inicio.toString(), renovacion, plan, alumno,
                renovacion, renovacion, inicio.toString()));
    }

    public void congelar(long matricula, LocalDate desde, String motivo) throws SQLException {
        Sql.write(sesion, AccesoService.Rol.OPERADOR, "matricula.congelar", c -> {
            Sql.change(c, """
                    UPDATE matriculas SET estado='pausada' WHERE id=? AND estado='activa'
                      AND ? BETWEEN fecha_inicio AND fecha_vencimiento
                    """, matricula, desde.toString());
            Sql.change(c, "INSERT INTO congelaciones(matricula_id,fecha_inicio,motivo) VALUES(?,?,?)",
                    matricula, desde.toString(), Sql.required(motivo, 250));
            return matricula;
        });
    }

    public void reactivar(long matricula, LocalDate hasta) throws SQLException {
        Sql.write(sesion, AccesoService.Rol.OPERADOR, "matricula.reactivar", c -> {
            var fecha = hasta.toString();
            long dias = Sql.id(c, """
                    UPDATE congelaciones SET fecha_fin=?,
                        dias_prorrogados=CAST(julianday(?) - julianday(fecha_inicio) AS INTEGER)
                    WHERE matricula_id=? AND fecha_fin IS NULL AND fecha_inicio<=?
                    RETURNING dias_prorrogados
                    """, fecha, fecha, matricula, fecha);
            Sql.change(c, """
                    UPDATE matriculas SET estado='activa', fecha_vencimiento=date(fecha_vencimiento, '+' || ? || ' days')
                    WHERE id=? AND estado='pausada'
                    """, dias, matricula);
            return matricula;
        });
    }

    public long registrarIngreso(long matricula, String codigo, Long horario, LocalDate fecha, LocalTime hora)
            throws SQLException {
        String dia = fecha.toString();
        String ingreso = hora.truncatedTo(ChronoUnit.SECONDS)
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (codigo == null || !codigo.matches("[0-9]{4}")) {
            throw new IllegalArgumentException("Código inválido");
        }
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "matricula.registrarIngreso", c -> {
            try (var statement = Sql.prepare(c, """
                    SELECT x.id FROM asistencias x JOIN matriculas m ON m.id=x.matricula_id
                    JOIN alumnos a ON a.estudiante_id=m.alumno_id
                    WHERE x.matricula_id=? AND a.codigo=? AND x.fecha=? AND x.hora_ingreso=? AND x.horario_id IS ?
                    """, matricula, codigo, dia, ingreso, horario); var result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getLong(1);
                }
            }
            return Sql.id(c, """
                    INSERT INTO asistencias(matricula_id,horario_id,fecha,hora_ingreso)
                    SELECT m.id,?,?,? FROM matriculas m JOIN alumnos a ON a.estudiante_id=m.alumno_id
                    WHERE m.id=? AND a.codigo=? AND a.estado='activo' AND m.estado='activa'
                      AND ? BETWEEN m.fecha_inicio AND m.fecha_vencimiento
                      AND NOT EXISTS (SELECT 1 FROM congelaciones f WHERE f.matricula_id=m.id
                        AND ? >= f.fecha_inicio AND (f.fecha_fin IS NULL OR ? < f.fecha_fin))
                      AND (m.sesiones_contratadas IS NULL OR m.sesiones_contratadas >
                        (SELECT count(*) FROM asistencias x WHERE x.matricula_id=m.id))
                      AND (? IS NULL OR EXISTS (SELECT 1 FROM horarios h WHERE h.id=? AND h.activo=1))
                    RETURNING id
                    """, horario, dia, ingreso, matricula, codigo, dia, dia, dia, horario, horario);
        });
    }
}
