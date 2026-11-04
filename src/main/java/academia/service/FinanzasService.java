package academia.service;

import java.sql.SQLException;

public final class FinanzasService {
    private final AccesoService.Sesion sesion;

    public FinanzasService(AccesoService.Sesion sesion) {
        this.sesion = java.util.Objects.requireNonNull(sesion);
    }

    public long abrirCaja(String responsable, long saldo) throws SQLException {
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "finanzas.abrirCaja", c -> Sql.id(c, """
                INSERT INTO cajas(responsable,saldo_inicial_centimos) VALUES(?,?) RETURNING id
                """, Sql.required(responsable, 100), saldo));
    }

    public void cerrarCaja(long caja, long contado) throws SQLException {
        Sql.write(sesion, AccesoService.Rol.ADMIN, "finanzas.cerrarCaja", c -> {
            Sql.change(c, """
                    UPDATE cajas SET cierre=CURRENT_TIMESTAMP,saldo_contado_centimos=?
                    WHERE id=? AND cierre IS NULL
                    """, contado, caja);
            return caja;
        });
    }

    public long cobrar(long caja, long matricula, String concepto, long importe, String metodo, String referencia,
                       java.util.UUID claveSolicitud)
            throws SQLException {
        String operacion = switch (metodo) {
            case "yape" -> Sql.required(referencia, 100);
            case "efectivo" -> null;
            default -> throw new IllegalArgumentException("Método de pago inválido");
        };
        String clave = java.util.Objects.requireNonNull(claveSolicitud, "Clave de solicitud obligatoria").toString();
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "finanzas.cobrar", c -> {
            try (var consulta = Sql.prepare(c, """
                    SELECT p.id, (p.matricula_id=? AND p.concepto=? AND p.monto_centimos=?
                                  AND p.metodo=? AND p.referencia IS ? AND m.caja_id=?) AS coincide
                    FROM solicitudes_cobro s JOIN pagos p ON p.id=s.pago_id
                    JOIN movimientos_caja m ON m.pago_id=p.id WHERE s.clave=?
                    """, matricula, concepto, importe, metodo, operacion, caja, clave);
                 var previo = consulta.executeQuery()) {
                if (previo.next()) {
                    if (previo.getInt("coincide") != 1) {
                        throw new IllegalStateException("La clave de solicitud pertenece a otro cobro");
                    }
                    return previo.getLong("id");
                }
            }
            long pago = Sql.id(c, """
                    INSERT INTO pagos(matricula_id,concepto,monto_centimos,metodo,referencia)
                    SELECT ?,?,?,?,? FROM cajas WHERE id=? AND cierre IS NULL RETURNING id
                    """, matricula, concepto, importe, metodo, operacion, caja);
            Sql.change(c, """
                    INSERT INTO movimientos_caja(caja_id,tipo,concepto,monto_centimos,metodo,pago_id)
                    VALUES(?,'ingreso',?,?,?,?)
                    """, caja, concepto, importe, metodo, pago);
            Sql.change(c, "INSERT INTO solicitudes_cobro(clave,pago_id) VALUES(?,?)", clave, pago);
            return pago;
        });
    }

    public void devolver(long caja, long pago) throws SQLException {
        Sql.write(sesion, AccesoService.Rol.ADMIN, "finanzas.devolver", c -> {
            Sql.change(c, """
                    INSERT INTO movimientos_caja(caja_id,tipo,concepto,monto_centimos,metodo)
                    SELECT c.id,'egreso','Devolución del pago ' || p.id,p.monto_centimos,p.metodo
                    FROM pagos p JOIN movimientos_caja m ON m.pago_id=p.id
                    JOIN cajas c ON c.id=? AND c.cierre IS NULL
                    WHERE p.id=? AND p.estado='confirmado'
                    """,
                    caja, pago);
            Sql.change(c, "UPDATE pagos SET estado='anulado' WHERE id=?", pago);
            try (var statement = Sql.prepare(c, "UPDATE comprobantes SET estado='anulado' WHERE pago_id=?", pago)) {
                statement.executeUpdate();
            }
            return pago;
        });
    }

    public long emitirComprobante(long pago, String receptor, String documento) throws SQLException {
        return Sql.write(sesion, AccesoService.Rol.OPERADOR, "finanzas.emitirComprobante", c -> Sql.id(c, """
                INSERT INTO comprobantes(pago_id,numero,receptor_nombre,receptor_documento,total_centimos)
                SELECT p.id,(SELECT coalesce(max(numero),0)+1 FROM comprobantes WHERE serie='LOCAL'),
                       ?,?,p.monto_centimos
                FROM pagos p WHERE p.id=? AND p.estado='confirmado' RETURNING id
                """, Sql.required(receptor, 150), documento == null ? null : Sql.required(documento, 20), pago));
    }

    public void pagarRemuneracion(long caja, long remuneracion, String metodo) throws SQLException {
        Sql.write(sesion, AccesoService.Rol.ADMIN, "finanzas.pagarRemuneracion", c -> {
            Sql.change(c, """
                    INSERT INTO movimientos_caja(caja_id,tipo,concepto,monto_centimos,metodo,remuneracion_id)
                    SELECT c.id,'egreso','Remuneración',r.monto_centimos,?,r.id
                    FROM remuneraciones r JOIN cajas c ON c.id=? AND c.cierre IS NULL
                    WHERE r.id=? AND r.estado='pendiente'
                    """, metodo, caja, remuneracion);
            Sql.change(c, "UPDATE remuneraciones SET estado='pagada' WHERE id=?", remuneracion);
            return remuneracion;
        });
    }
}
