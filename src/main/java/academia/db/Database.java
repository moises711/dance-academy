package academia.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    public static final Path FILE = Path.of("data", "academia.db").toAbsolutePath().normalize();

    private Database() {
    }

    public static void initialize() throws IOException, SQLException {
        Files.createDirectories(FILE.getParent());
        String schema;
        try (var input = Database.class.getResourceAsStream("/academia/schema.sql")) {
            if (input == null) {
                throw new IOException("Falta el recurso academia/schema.sql");
            }
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + FILE);
             var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            statement.execute("PRAGMA busy_timeout = 5000");
            boolean migrate = false;
            try (var columns = statement.executeQuery("PRAGMA table_info(alumnos)")) {
                while (columns.next()) {
                    migrate |= "codigo_qr".equals(columns.getString("name"));
                }
            }
            if (migrate) {
                try (var input = Database.class.getResourceAsStream("/academia/migration-v2.sql")) {
                    if (input == null) {
                        throw new IOException("Falta el recurso academia/migration-v2.sql");
                    }
                    schema = new String(input.readAllBytes(), StandardCharsets.UTF_8) + "\n" + schema;
                }
            }
            connection.setAutoCommit(false);
            try {
                // El esquema contiene sentencias simples, sin triggers ni puntos y coma en textos.
                for (String sql : schema.split(";")) {
                    if (!sql.isBlank()) {
                        statement.executeUpdate(sql);
                    }
                }
                try (var violations = statement.executeQuery("PRAGMA foreign_key_check")) {
                    if (violations.next()) {
                        throw new SQLException("Relaciones invalidas en la migracion");
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
            connection.setAutoCommit(true);
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }
}
