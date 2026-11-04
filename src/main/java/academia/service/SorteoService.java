package academia.service;

import java.sql.SQLException;
import java.time.LocalDate;

public final class SorteoService {
    private final AccesoService.Sesion sesion;

    public SorteoService(AccesoService.Sesion sesion) {
        this.sesion = java.util.Objects.requireNonNull(sesion);
    }

    public long crear(String nombre, String premio, LocalDate sorteo, LocalDate desde, LocalDate hasta,
                      int minimo, boolean requiereMatricula) throws SQLException {
        if (sorteo.isBefore(hasta)) {
            throw new IllegalArgumentException("El sorteo debe realizarse después del período de participación");
        }
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "sorteo.crear", c -> Sql.id(c, """
                INSERT INTO sorteos(nombre,premio,fecha_sorteo,minimo_asistencias,
                                    periodo_inicio,periodo_fin,requiere_matricula_vigente)
                VALUES(?,?,?,?,?,?,?) RETURNING id
                """, Sql.required(nombre, 100), Sql.required(premio, 150), sorteo.toString(),
                minimo, desde.toString(), hasta.toString(), requiereMatricula ? 1 : 0));
    }

    public void inscribir(long sorteo, long alumno) throws SQLException {
        Sql.write(sesion, AccesoService.Rol.OPERADOR, "sorteo.inscribir", c -> {
            Sql.change(c, """
                    INSERT INTO participantes_sorteo(sorteo_id,alumno_id)
                    SELECT sorteo_id,alumno_id FROM v_elegibles_sorteo WHERE sorteo_id=? AND alumno_id=?
                    """, sorteo, alumno);
            return sorteo;
        });
    }

    public java.util.List<Long> elegir(long sorteo, int puestos) throws SQLException {
        if (puestos < 1) {
            throw new IllegalArgumentException("Se requiere al menos un puesto");
        }
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "sorteo.elegir", c -> {
            var ganadores = new java.util.ArrayList<Long>();
            try (var statement = Sql.prepare(c, """
                    SELECT p.alumno_id FROM participantes_sorteo p
                    JOIN v_elegibles_sorteo e USING(sorteo_id,alumno_id)
                    JOIN sorteos s ON s.id=p.sorteo_id
                    WHERE p.sorteo_id=? AND s.fecha_sorteo<=date('now','-5 hours')
                    ORDER BY random() LIMIT ?
                    """, sorteo, puestos); var result = statement.executeQuery()) {
                while (result.next()) {
                    ganadores.add(result.getLong(1));
                }
            }
            if (ganadores.size() != puestos) {
                throw new IllegalStateException("No hay suficientes participantes elegibles");
            }
            for (int i = 0; i < puestos; i++) {
                Sql.change(c, "INSERT INTO ganadores_sorteo(sorteo_id,alumno_id,puesto) VALUES(?,?,?)",
                        sorteo, ganadores.get(i), i + 1);
            }
            Sql.change(c, "UPDATE sorteos SET estado='realizado' WHERE id=? AND estado='abierto'", sorteo);
            return java.util.List.copyOf(ganadores);
        });
    }
}
