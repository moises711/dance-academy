package academia.ui;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;

/** Iconos vectoriales propios con cuadrícula de 24 puntos; no requieren fuentes ni dependencias. */
final class Icons {
    private Icons() {}
    static Pane of(String name,double size) {
        String data=switch(name) {
            case "Dashboard" -> "M3 3H10V10H3Z M14 3H21V10H14Z M3 14H10V21H3Z M14 14H21V21H14Z";
            case "Alumnos" -> "M9 11A4 4 0 1 0 9 3A4 4 0 1 0 9 11 M2 21V18C2 14 16 14 16 18V21 M17 4C22 4 22 10 18 11 M19 15C22 15 23 17 23 20";
            case "Matrículas" -> "M8 4H5V22H21V4H18 M9 2H17V7H9Z M8 12H17 M8 17H14";
            case "Asistencias" -> "M8 3H3V8 M16 3H21V8 M3 16V21H8 M21 16V21H16 M7 12L10 15L17 8";
            case "Peso" -> "M6 4H18L22 21H2Z M8 9A4 4 0 0 1 16 9 M12 9L14 7 M7 17H17";
            case "Pagos" -> "M3 7H20V21H3Z M3 7V4L17 2V7 M16 12H22V17H16Z M18 14.5H19";
            case "Comprobantes" -> "M5 2H19V22L16 20L13 22L10 20L7 22L5 20Z M8 7H16 M8 11H16 M8 15H12";
            case "Instructores" -> "M12 2L2 7L12 12L22 7Z M6 10V15C9 18 15 18 18 15V10 M22 7V15 M6 22C6 17 18 17 18 22";
            case "Sorteos" -> "M8 3H16V9C16 15 8 15 8 9Z M8 5H3V8C3 11 5 12 9 12 M16 5H21V8C21 11 19 12 15 12 M12 14V20 M7 21H17";
            case "Finanzas" -> "M4 21V14H8V21 M10 21V9H14V21 M16 21V3H20V21 M2 21H22";
            case "Reportes" -> "M5 2H14L20 8V22H5Z M14 2V8H20 M9 17V13 M13 17V11 M17 17V14";
            case "Configuración" -> "M9 3H15L16 6L19 7L22 12L20 15L19 18L14 21L11 20L8 20L3 15L4 12L3 9L8 4Z M16 12A4 4 0 1 0 8 12A4 4 0 1 0 16 12";
            case "search" -> "M17 10A7 7 0 1 0 3 10A7 7 0 1 0 17 10 M15 15L22 22";
            case "bell" -> "M5 17H19L17 14V9C17 2 7 2 7 9V14Z M10 21H14 M12 2V3";
            case "music" -> "M9 18V5L21 2V15 M9 5V9L21 6 M9 18A3 3 0 1 0 3 18A3 3 0 1 0 9 18 M21 15A3 3 0 1 0 15 15A3 3 0 1 0 21 15";
            case "export" -> "M12 2V15 M7 10L12 15L17 10 M3 15V22H21V15";
            case "edit" -> "M15 4L20 9 M4 15L16 3C18 1 23 6 21 8L9 20L3 21Z";
            case "plus" -> "M12 4V20 M4 12H20";
            case "pause" -> "M7 4H9V20H7Z M15 4H17V20H15Z";
            case "play" -> "M7 3L21 12L7 21Z";
            case "list" -> "M8 5H21 M8 12H21 M8 19H21 M3 5H4 M3 12H4 M3 19H4";
            case "clock" -> "M21 12A9 9 0 1 0 3 12A9 9 0 1 0 21 12 M12 7V12L16 14";
            case "money" -> "M2 5H22V19H2Z M15 12A3 3 0 1 0 9 12A3 3 0 1 0 15 12 M5 9V15 M19 9V15";
            case "more" -> "M12 4H12.1 M12 12H12.1 M12 20H12.1";
            default -> throw new IllegalArgumentException("Icono desconocido: "+name);
        };
        var path=new SVGPath();path.setContent(data);path.setFill(null);path.setStroke(Color.web("#344c76"));
        path.setStrokeWidth(name.equals("more")?3.5:1.8);path.setStrokeLineCap(StrokeLineCap.ROUND);path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        path.getStyleClass().add("vector-path");path.getTransforms().add(new Scale(size/24,size/24));
        var view=new Pane(path);view.setMinSize(size,size);view.setPrefSize(size,size);view.setMaxSize(size,size);view.setMouseTransparent(true);
        view.getStyleClass().add("vector-icon");return view;
    }
}
