package academia.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    public static final Path FILE = Path.of("data", "academia.db").toAbsolutePath().normalize();
    private static final int VERSION = 4;

    private Database() {
    }

    public static Connection open() throws SQLException {
        try {
            protectLocalData();
        } catch (IOException failure) {
            throw new SQLException("No se pudo proteger la base local", failure);
        }
        var connection = DriverManager.getConnection("jdbc:sqlite:" + FILE);
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA foreign_keys = ON");
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                exception.addSuppressed(closeFailure);
            }
            throw exception;
        }
    }

    private static void protectLocalData() throws IOException {
        Path directory = FILE.getParent();
        Files.createDirectories(directory);
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("La carpeta de datos debe ser un directorio local");
        }
        boolean posix = Files.getFileStore(directory).supportsFileAttributeView("posix");
        if (posix) {
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
        }
        if (Files.exists(FILE, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(FILE, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("La base de datos debe ser un archivo regular");
            }
            if (posix) {
                Files.setPosixFilePermissions(FILE, PosixFilePermissions.fromString("rw-------"));
            }
        } else if (posix) {
            try {
                Files.createFile(FILE, PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString("rw-------")));
            } catch (java.nio.file.FileAlreadyExistsException concurrentOpen) {
                // Another connection may have just created the private database.
            }
        }
    }

    public static void initialize() throws IOException, SQLException {
        try (var connection = open(); var statement = connection.createStatement()) {
            // SQLite exige desactivar las referencias antes de reconstruir alumnos.
            statement.execute("PRAGMA foreign_keys = OFF");
            statement.execute("BEGIN IMMEDIATE");
            try {
                int version;
                try (var result = statement.executeQuery("PRAGMA user_version")) {
                    version = result.getInt(1);
                }
                if (version > VERSION) {
                    throw new SQLException("La base de datos requiere una versión más reciente de la aplicación");
                }
                if (version < 3) {
                    if (hasLegacyCode(statement)) {
                        runScript(statement, "migration-v2.sql", ";");
                    }
                    runScript(statement, "schema.sql", ";");
                    runScript(statement, "backfill-alumnos.sql", ";");
                    runScript(statement, "migration-v3.sql", "(?m)^-- siguiente sentencia$");
                }
                if (version < VERSION) {
                    runScript(statement, "migration-v4.sql", ";");
                    statement.execute("PRAGMA user_version = " + VERSION);
                }
                try (var result = statement.executeQuery("PRAGMA foreign_key_check")) {
                    if (result.next()) {
                        throw new SQLException("La base de datos contiene relaciones inválidas");
                    }
                }
                statement.execute("COMMIT");
            } catch (IOException | SQLException | RuntimeException exception) {
                try {
                    statement.execute("ROLLBACK");
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        }
    }

    private static boolean hasLegacyCode(Statement statement) throws SQLException {
        try (var columns = statement.executeQuery("PRAGMA table_info(alumnos)")) {
            while (columns.next()) {
                if ("codigo_qr".equals(columns.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void runScript(Statement statement, String file, String separator)
            throws IOException, SQLException {
        try (var input = Database.class.getResourceAsStream("/academia/" + file)) {
            if (input == null) {
                throw new IOException("Falta el recurso academia/" + file);
            }
            var script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String sql : script.split(separator)) {
                if (!sql.isBlank()) {
                    statement.executeUpdate(sql);
                }
            }
        }
    }
}
