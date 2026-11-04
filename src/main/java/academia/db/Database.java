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
            statement.executeUpdate(schema);
        }
    }
}
