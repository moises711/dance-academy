package academia.ui;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Formulario de edición para las tablas que funcionan en memoria. */
final class RecordDialog {
    private record Input(Node control, Supplier<String> value, ObservableValue<?> changes) {}
    private record Field(Input input, Label error) {}

    private RecordDialog() {}

    static Optional<ObservableList<String>> show(Window owner, PreviewData.Section section,
                                                 ObservableList<String> selected, String studentName) {
        boolean editing = selected != null;
        var dialog = new Dialog<ObservableList<String>>();
        dialog.initOwner(owner);
        dialog.setTitle(title(section.name(), editing));

        var pane = dialog.getDialogPane();
        pane.getStylesheets().add(RecordDialog.class.getResource("/academia/ui.css").toExternalForm());
        pane.getStyleClass().add("record-dialog");
        pane.setPrefWidth(630);
        var save = new ButtonType(editing ? "Guardar cambios" : "Guardar registro", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, save);
        pane.lookupButton(save).getStyleClass().add("primary");
        pane.lookupButton(ButtonType.CANCEL).getStyleClass().add("secondary");

        var caption = styled("GESTIÓN DE LA ACADEMIA", "record-kicker");
        var heading = styled(dialog.getTitle(), "record-title");
        var subtitle = styled(studentName == null ? "Completa los campos marcados con *." : "Registro para " + studentName, "record-description");
        var header = new VBox(5, caption, heading, subtitle);
        header.getStyleClass().add("record-header");
        pane.setHeader(header);

        var body = new VBox(16);
        body.getStyleClass().add("record-body");
        var grid = grid();
        body.getChildren().addAll(styled(section.name().equals("Alumnos") ? "Identificación" : "Datos del registro", "record-group"), grid);
        var fields = new ArrayList<Field>(java.util.Collections.nCopies(section.columns().size(), null));
        var order = section.name().equals("Alumnos") ? List.of(1,0,2,3,4,5)
            : java.util.stream.IntStream.range(0, section.columns().size()).boxed().toList();
        int slot = 0;
        for (int i : order) {
            String name = section.columns().get(i);
            String initial = editing ? selected.get(i) : defaultValue(section.name(), name);
            boolean hiddenStudent = i == 0 && studentName != null;
            if (hiddenStudent) initial = studentName;
            var input = input(section.name(), name, initial);
            input.control().setId("record-field-" + i);
            var error = styled("", "record-field-error");
            error.setWrapText(true);
            error.managedProperty().bind(error.visibleProperty());
            error.setVisible(false);
            input.changes().addListener((obs, old, value) -> setError(input.control(), error, null));
            fields.set(i, new Field(input, error));
            if (hiddenStudent || (!editing && section.name().equals("Mediciones") && name.equals("Próxima medición"))) continue;

            if (section.name().equals("Alumnos") && i == 3) {
                grid = grid();
                body.getChildren().addAll(new Separator(), styled("Contacto y estado", "record-group"), grid);
                slot = 0;
            }
            var nameLabel = styled(name + (required(section, i) ? " *" : ""), "record-field-label");
            nameLabel.setLabelFor(input.control());
            var field = new VBox(6, nameLabel, input.control(), error);
            boolean wide = input.control() instanceof TextArea || name.equals("Nombre");
            if (wide && slot % 2 != 0) slot++;
            grid.add(field, slot % 2, slot / 2, wide ? 2 : 1, 1);
            slot += wide ? 2 : 1;
        }
        var scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(425);
        scroll.getStyleClass().add("record-scroll");
        pane.setContent(scroll);

        pane.lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            var values = values(fields);
            Node firstInvalid = null;
            for (int i = 0; i < fields.size(); i++) {
                String problem = validate(section, values, i, selected);
                var field = fields.get(i);
                setError(field.input().control(), field.error(), problem);
                if (problem != null && firstInvalid == null) firstInvalid = field.input().control();
            }
            if (firstInvalid != null) {
                event.consume();
                firstInvalid.requestFocus();
            }
        });
        dialog.setResultConverter(button -> {
            if (button != save) return null;
            var row = FXCollections.observableArrayList(values(fields));
            if (section.name().equals("Mediciones") && row.get(3).isBlank()) {
                row.set(3, LocalDate.parse(row.get(1)).plusDays(15).toString());
            }
            return row;
        });
        return dialog.showAndWait();
    }

    private static GridPane grid() {
        var grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(13);
        for (int i = 0; i < 2; i++) {
            var column = new ColumnConstraints();
            column.setPercentWidth(50);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    private static Input input(String section, String name, String initial) {
        if (dateField(section, name)) {
            var date = new DatePicker();
            date.setConverter(new javafx.util.StringConverter<LocalDate>() {
                @Override public String toString(LocalDate value) {
                    return value == null ? "" : value.toString();
                }

                @Override public LocalDate fromString(String value) {
                    return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
                }
            });
            if (!initial.isBlank()) {
                try { date.setValue(LocalDate.parse(initial)); }
                catch (java.time.format.DateTimeParseException ex) { date.getEditor().setText(initial); }
            }
            date.setPromptText("AAAA-MM-DD");
            date.setMaxWidth(Double.MAX_VALUE);
            return new Input(date, () -> date.getEditor().getText().trim(), date.getEditor().textProperty());
        }

        List<String> choices = switch (name) {
            case "Método" -> section.equals("Pagos") ? List.of("Efectivo", "Yape") : List.of();
            case "Estado" -> switch (section) {
                case "Alumnos" -> List.of("Activo", "Pausado", "Inactivo");
                case "Matrículas", "Renovaciones" -> List.of("Activa", "Pendiente", "Pausada", "Vencida");
                case "Pagos" -> List.of("Pendiente", "Confirmado", "Anulado");
                case "Congelaciones" -> List.of("En curso", "Concluida");
                default -> List.of();
            };
            default -> List.of();
        };
        if (!choices.isEmpty()) {
            var combo = new ComboBox<>(FXCollections.observableArrayList(choices));
            if (!initial.isBlank() && !combo.getItems().contains(initial)) combo.getItems().add(initial);
            if (!initial.isBlank()) combo.setValue(initial);
            combo.setPromptText("Selecciona una opción");
            combo.setMaxWidth(Double.MAX_VALUE);
            return new Input(combo, () -> combo.getValue() == null ? "" : combo.getValue(), combo.valueProperty());
        }

        TextInputControl field;
        if (List.of("Observaciones", "Detalle", "Motivo").contains(name)) {
            var area = new TextArea(initial);
            area.setPrefRowCount(2);
            area.setWrapText(true);
            field = area;
        } else {
            field = new TextField(initial);
        }
        field.setMaxWidth(Double.MAX_VALUE);
        field.setPromptText(switch (name) {
            case "Código" -> "4 dígitos";
            case "DNI" -> "8 dígitos";
            case "Nombre" -> "Nombres y apellidos";
            case "Teléfono" -> "Número de contacto";
            case "Correo" -> "nombre@correo.com";
            case "Peso (kg)" -> "Ej. 56,4";
            default -> name.contains("(S/)") ? "Ej. 120,00" : "";
        });
        return new Input(field, () -> {
            String text = field.getText().trim();
            return numericField(name) ? text.replace(',', '.') : text;
        }, field.textProperty());
    }

    private static List<String> values(List<Field> fields) {
        return fields.stream().map(field -> field.input().value().get()).toList();
    }

    private static boolean dateField(String section, String field) {
        return List.of("Fecha","Vencimiento","Desde","Hasta","Próxima medición").contains(field)
            || field.equals("Inicio") && List.of("Matrículas","Renovaciones","Congelaciones").contains(section)
            || field.equals("Fin") && section.equals("Congelaciones");
    }

    private static boolean numericField(String field) {
        return field.contains("(S/)") || field.equals("Peso (kg)");
    }

    private static boolean required(PreviewData.Section section, int i) {
        String field = section.columns().get(i);
        return i == 0 || field.equals("Estado")
            || section.name().equals("Alumnos") && field.equals("Nombre")
            || section.name().equals("Congelaciones") && List.of("Inicio","Motivo").contains(field)
            || section.name().equals("Mediciones") && List.of("Fecha","Peso (kg)").contains(field);
    }

    private static String validate(PreviewData.Section section, List<String> values, int i, ObservableList<String> selected) {
        String field = section.columns().get(i);
        String value = values.get(i);
        if (value.isBlank()) return required(section, i) ? "Completa este campo." : null;
        if (field.equals("Código")) {
            if (!value.matches("[0-9]{4}")) return "El código debe tener cuatro dígitos.";
            if (section.name().equals("Alumnos") && section.rows().stream().anyMatch(row -> row != selected && row.getFirst().equals(value))) return "Este código ya está en uso.";
        }
        if (field.equals("DNI") && !value.matches("[0-9]{8}")) return "El DNI debe tener ocho dígitos.";
        if (numericField(field)) {
            try {
                var number = new BigDecimal(value);
                if (number.signum() < 0 || (field.equals("Peso (kg)") && number.signum() == 0)) return "Ingresa un valor válido.";
            } catch (NumberFormatException ex) {
                return "Ingresa un número, por ejemplo 56,4.";
            }
        }
        if (dateField(section.name(), field)) {
            try { LocalDate.parse(value); }
            catch (java.time.DateTimeException ex) { return "Usa una fecha válida (AAAA-MM-DD)."; }
        }
        return null;
    }

    private static String defaultValue(String section, String field) {
        if (!field.equals("Estado")) return "";
        return switch (section) {
            case "Alumnos" -> "Activo";
            case "Matrículas", "Renovaciones" -> "Activa";
            case "Congelaciones" -> "En curso";
            case "Pagos" -> "Pendiente";
            default -> "";
        };
    }

    private static String title(String section, boolean editing) {
        return switch (section) {
            case "Alumnos" -> editing ? "Editar datos del alumno" : "Nuevo alumno";
            case "Pagos" -> editing ? "Editar pago" : "Registrar pago";
            case "Matrículas" -> editing ? "Editar matrícula" : "Registrar matrícula";
            case "Mediciones" -> editing ? "Editar medición" : "Registrar medición";
            case "Congelaciones" -> editing ? "Editar pausa" : "Registrar pausa";
            case "Historial" -> editing ? "Editar nota" : "Agregar nota";
            default -> (editing ? "Editar registro · " : "Nuevo registro · ") + section;
        };
    }

    private static Label styled(String text, String style) {
        var label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }

    private static void setError(Node input, Label error, String message) {
        input.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("invalid"), message != null);
        error.setText(message == null ? "" : message);
        error.setVisible(message != null);
    }
}
