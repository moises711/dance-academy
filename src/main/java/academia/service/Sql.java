package academia.service;

import academia.db.Database;
import java.sql.*;
import java.util.*;

final class Sql {
    @FunctionalInterface
    interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    static <T> T write(AccesoService.Sesion sesion, AccesoService.Rol minimo,
                       String operacion, Work<T> work) throws SQLException {
        return transaction(connection -> {
            long usuario = sesion.verificar(connection, minimo);
            T resultado = work.run(connection);
            auditar(connection, usuario, operacion, resultado instanceof Long id ? id : null);
            return resultado;
        });
    }

    static void auditar(Connection connection, long usuario, String operacion, Long entidad) throws SQLException {
        change(connection, "INSERT INTO auditoria(usuario_id,operacion,entidad_id) VALUES(?,?,?)",
                usuario, operacion, entidad);
    }

    static <T> T transaction(Work<T> work) throws SQLException {
        try (var connection = Database.open(); var transaction = connection.createStatement()) {
            transaction.execute("BEGIN IMMEDIATE");
            try {
                T result = work.run(connection);
                transaction.execute("COMMIT");
                return result;
            } catch (SQLException | RuntimeException failure) {
                try {
                    transaction.execute("ROLLBACK");
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    static PreparedStatement prepare(Connection connection, String sql, Object... args) throws SQLException {
        var statement = connection.prepareStatement(sql);
        try {
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        } catch (SQLException failure) {
            try {
                statement.close();
            } catch (SQLException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }

    static void change(Connection connection, String sql, Object... args) throws SQLException {
        try (var statement = prepare(connection, sql, args)) {
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("La operación no es válida en el estado actual");
            }
        }
    }

    static long id(Connection connection, String sql, Object... args) throws SQLException {
        try (var statement = prepare(connection, sql, args); var result = statement.executeQuery()) {
            if (!result.next()) {
                throw new IllegalStateException("La operación no es válida en el estado actual");
            }
            return result.getLong(1);
        }
    }

    static List<Map<String, Object>> read(AccesoService.Sesion sesion, AccesoService.Rol minimo,
                                          String sql, Object... args) throws SQLException {
        try (var connection = Database.open()) {
            sesion.verificar(connection, minimo);
            return rows(connection, sql, args);
        }
    }

    private static List<Map<String, Object>> rows(Connection connection, String sql, Object... args)
            throws SQLException {
        try (var statement = prepare(connection, sql, args);
             var result = statement.executeQuery()) {
            var rows = new ArrayList<Map<String, Object>>();
            var metadata = result.getMetaData();
            while (result.next()) {
                var row = new LinkedHashMap<String, Object>();
                for (int column = 1; column <= metadata.getColumnCount(); column++) {
                    row.put(metadata.getColumnLabel(column), result.getObject(column));
                }
                rows.add(Collections.unmodifiableMap(row));
            }
            return List.copyOf(rows);
        }
    }

    static String required(String value, int limit) {
        if (value == null || value.isBlank() || value.strip().length() > limit) {
            throw new IllegalArgumentException("Texto obligatorio o demasiado largo");
        }
        return value.strip();
    }
}
