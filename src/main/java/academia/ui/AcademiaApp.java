package academia.ui;

import academia.db.Database;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public final class AcademiaApp extends Application {
    @Override
    public void start(Stage stage) {
        try {
            Database.initialize();
        } catch (IOException | SQLException exception) {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("No se pudo iniciar la base de datos local");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
            return;
        }

        var databasePath = new Label(Database.FILE.toString());
        databasePath.setWrapText(true);
        var content = new VBox(12, new Label("Administración de la academia"),
                new Label("Base de datos local lista"), databasePath);
        content.setPadding(new Insets(24));
        stage.setTitle("Academia");
        stage.setScene(new Scene(content, 520, 220));
        stage.show();
    }
}
