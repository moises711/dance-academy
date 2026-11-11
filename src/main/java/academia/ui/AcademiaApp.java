package academia.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Interfaz de demostración: no abre conexiones ni escribe en la base de datos. */
public final class AcademiaApp extends Application {
    private final List<PreviewData.Module> modules = PreviewData.modules();
    private final Map<String, Button> navigation = new LinkedHashMap<>();
    private final BorderPane root = new BorderPane();
    private final BorderPane workspace = new BorderPane();
    private DashboardView dashboardView;
    private final Label breadcrumb = new Label();
    private final Label feedback = new Label("Los cambios de esta sesión son de demostración y se descartan al cerrar.");
    private Stage stage;
    private String currentModule;
    private TableView<ObservableList<String>> currentTable;
    private TextField currentSearch;

    @Override public void start(Stage stage) {
        this.stage = stage;
        root.getStyleClass().add("app");
        root.setLeft(sidebar());
        root.setCenter(workspace);
        var globalSearch = new TextField(); globalSearch.setPromptText("Buscar alumnos, pagos o módulos…");
        globalSearch.getStyleClass().add("global-search"); globalSearch.setMinWidth(100); globalSearch.setPrefWidth(250);
        var shortcut=label("Ctrl + K", "shortcut");
        var searchBox=new HBox(8,Icons.of("search",20),globalSearch,shortcut); searchBox.setAlignment(Pos.CENTER_LEFT); searchBox.getStyleClass().add("global-search-box");
        globalSearch.textProperty().addListener((o,a,b)-> {
            if("Dashboard".equals(currentModule) && dashboardView!=null) dashboardView.search(b);
            else if(currentSearch!=null) currentSearch.setText(b);
        });
        globalSearch.setOnAction(e -> modules.stream().filter(m->navigation.containsKey(m.name())).filter(m->m.name().toLowerCase(Locale.ROOT).contains(globalSearch.getText().toLowerCase(Locale.ROOT))).findFirst().ifPresent(m->{showModule(m);globalSearch.clear();}));
        var date = new DatePicker(LocalDate.now(ZoneId.of("America/Lima"))); date.setEditable(false); date.getStyleClass().add("header-date"); date.setPrefWidth(150);
        var notifications=button("", "notification-button",()->message("No hay avisos."));
        var bellBox=new StackPane(Icons.of("bell",23));notifications.setGraphic(bellBox);
        notifications.setTooltip(new Tooltip("Avisos de la academia"));
        var profile=new MenuButton("Administración  ⌄",label("AD","avatar"));
        profile.getItems().addAll(new MenuItem("Academia de baile · Vista previa"),new MenuItem("Configuración"));
        profile.getItems().get(1).setOnAction(e->showModule(modules.getLast())); profile.getStyleClass().add("profile-menu");
        var top = new HBox(22, searchBox, spacer(), date, notifications, profile);
        top.getStyleClass().add("topbar"); top.setAlignment(Pos.CENTER_LEFT);
        workspace.setTop(top);
        feedback.getStyleClass().add("footer"); feedback.setMaxWidth(Double.MAX_VALUE);
        root.setBottom(feedback);
        var scene = new Scene(root, 1360, 880);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/academia/ui.css")).toExternalForm());
        stage.setTitle("Ritmo · Administración de academia — Vista previa");
        stage.setMinWidth(1200); stage.setMinHeight(740); stage.setScene(scene);
        showModule(modules.getFirst());
        scene.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.K,javafx.scene.input.KeyCombination.CONTROL_DOWN),globalSearch::requestFocus);
        stage.show();
        if (getParameters().getRaw().contains("--test-views")) Platform.runLater(this::testViews);
    }

    private Node sidebar() {
        var brand = label("Ritmo", "brand"); brand.setGraphic(Icons.of("music",30)); brand.setGraphicTextGap(12);
        var subtitle = label("El movimiento nos une", "brand-subtitle");
        var box = new VBox(4, brand, subtitle);
        box.setPadding(new Insets(28, 16, 20, 16)); box.setPrefWidth(244);
        box.getStyleClass().add("sidebar");
        for (var module : modules) {
            if (module.name().equals("Peso")) continue;
            var button = new Button(module.name(),Icons.of(module.name(),21)); button.setGraphicTextGap(16);
            button.setId("nav-"+module.name()); button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("nav-button");
            button.setOnAction(e -> showModule(module));
            navigation.put(module.name(),button); box.getChildren().add(button);
        }
        var grow = new Region(); VBox.setVgrow(grow,Priority.ALWAYS);
        var tip=new VBox(9,label("Cada paso cuenta.","sidebar-tip-title"),label("Acompaña el progreso\nde tu comunidad.","sidebar-tip-copy"),button("Ver alumnos     →","sidebar-tip-button",()->showModule(modules.get(1))));
        tip.getStyleClass().add("sidebar-tip"); tip.setMinHeight(Region.USE_PREF_SIZE);
        box.getChildren().addAll(grow,tip,label("Vista previa · Sin conexión", "sidebar-note"));
        var scroll = new ScrollPane(box); scroll.setFitToWidth(true); scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); scroll.setPrefWidth(244);
        scroll.getStyleClass().add("sidebar-scroll");
        return scroll;
    }

    private void showModule(PreviewData.Module module) {
        currentModule=module.name(); currentTable=null; currentSearch=null;
        breadcrumb.setText("Academia   /   "+module.name());
        navigation.forEach((name,button) -> { button.getStyleClass().remove("selected"); if(name.equals(module.name())) button.getStyleClass().add("selected"); });
        var heading = new VBox(6, label(module.name(), "page-title"), label(module.name().equals("Dashboard")?"Resumen de las operaciones de tu academia":module.description(), "subtitle"));
        var intro = new HBox(heading, spacer(), label("Vista previa", "demo-pill")); intro.setAlignment(Pos.CENTER_LEFT);
        var content = new VBox(22,intro); content.setPadding(new Insets(28,32,24,32));
        if (module.name().equals("Reportes")) {
            intro.getChildren().add(button("Descargar Excel completo", "primary", this::exportWorkbook));
        }
        if(module.sections().isEmpty()) {
            dashboardView=new DashboardView(modules, name->modules.stream().filter(m->m.name().equals(name)).findFirst().ifPresent(this::showModule),this::export);
            intro.getChildren().removeLast(); intro.getChildren().add(dashboardView.periodControl());
            content.getChildren().add(dashboardView); VBox.setVgrow(dashboardView,Priority.ALWAYS);
        }
        else {
            if(module.name().equals("Finanzas")) content.getChildren().add(financeMetrics(module.sections().getFirst()));
            var tabs = new TabPane(); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            for(var section:module.sections()) {
                var tab = new Tab(section.name()); tab.setContent(sectionPanel(module,section)); tabs.getTabs().add(tab);
            }
            tabs.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> {
                if(b!=null) { currentTable=findTable(b.getContent()); currentSearch=(TextField)b.getContent().lookup("#search"); }
            });
            content.getChildren().add(tabs); VBox.setVgrow(tabs,Priority.ALWAYS);
            currentTable=findTable(tabs.getTabs().getFirst().getContent());
            currentSearch=(TextField)tabs.getTabs().getFirst().getContent().lookup("#search");
        }
        var scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setFitToHeight(true); scroll.getStyleClass().add("main-scroll");
        workspace.setCenter(scroll);
    }
    @SuppressWarnings("unchecked") private TableView<ObservableList<String>> findTable(Node node) {
        return (TableView<ObservableList<String>>)node.lookup(".table-view");
    }

    private Node sectionPanel(PreviewData.Module module, PreviewData.Section section) {
        boolean students = section.name().equals("Alumnos");
        var search = new TextField(); search.setId("search");
        search.setPromptText(students ? "Buscar por nombre, DNI o código…" : "Buscar en "+section.name().toLowerCase(Locale.ROOT)+"…");
        search.setPrefWidth(260);
        var state = new ComboBox<String>(); state.getItems().add("Todos los estados");
        int stateIndex=section.columns().indexOf("Estado");
        if (students) state.getItems().addAll("Activa","Pausada","Vencida","Pendiente","Sin matrícula");
        else if(stateIndex>=0) section.rows().stream().map(r->r.get(stateIndex)).distinct().sorted().forEach(state.getItems()::add);
        state.getSelectionModel().selectFirst(); state.setVisible(stateIndex>=0); state.setManaged(stateIndex>=0);
        var filtered = new FilteredList<>(section.rows(),r->true);
        Runnable filter=()->filtered.setPredicate(row -> row.stream().anyMatch(v->v.toLowerCase(Locale.ROOT).contains(search.getText().trim().toLowerCase(Locale.ROOT))) && (stateIndex<0 || state.getSelectionModel().getSelectedIndex()==0 || (students ? studentStatus(row) : row.get(stateIndex)).equals(state.getValue())));
        search.textProperty().addListener((o,a,b)->filter.run()); state.valueProperty().addListener((o,a,b)->filter.run());
        var table = new TableView<ObservableList<String>>(filtered); table.setId("table-"+section.name()); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new VBox(10,label("Sin resultados", "empty-title"),label("Prueba otra búsqueda o agrega un registro.","muted")));
        table.setMinHeight(220); table.setPrefHeight(420);
        for(int i=0;i<section.columns().size();i++) {
            if (students && (i==2 || i==4)) continue;
            int index=i; var column=new TableColumn<ObservableList<String>,String>(section.columns().get(i));
            column.setCellValueFactory(cell->new ReadOnlyStringWrapper(students && index==stateIndex ? studentStatus(cell.getValue()) : cell.getValue().get(index)));
            column.setMinWidth(students ? 55 : 85);
            if (students && index==stateIndex) column.setText("Matrícula");
            if(i==0) column.setPrefWidth(students ? 66 : 160);
            if(students && index==1) {
                column.setText("Alumno / Identificación");
                column.setPrefWidth(190);
                column.setCellFactory(c -> new TableCell<>() {
                    @Override protected void updateItem(String name, boolean empty) {
                        super.updateItem(name,empty);
                        setGraphic(null);
                        setText(null);
                        if (!empty && getTableRow().getItem()!=null) {
                            var student = getTableRow().getItem();
                            var details = new VBox(3,label(name,"student-field-value"),label("DNI "+(student.get(2).isBlank()?"—":student.get(2)),"muted"));
                            setGraphic(details);
                        }
                    }
                });
            }
            if(students && index==stateIndex) column.setMinWidth(108);
            if(i==stateIndex || section.columns().get(i).equals("Elegibilidad")) column.setCellFactory(c->new TableCell<>() {
                @Override protected void updateItem(String text, boolean empty) {
                    super.updateItem(text,empty); setText(null); setGraphic(null);
                    if(!empty && text!=null) setGraphic(studentBadge(text));
                }
            });
            table.getColumns().add(column);
        }
        var drawer = new VBox(12);
        drawer.getStyleClass().addAll("card","student-drawer");
        drawer.setPrefWidth(295);
        drawer.setMinWidth(275);
        drawer.setVisible(false);
        drawer.setManaged(false);
        if (students) {
            var expiration = new TableColumn<ObservableList<String>,String>("Vencimiento");
            expiration.setMinWidth(95);
            expiration.setCellValueFactory(cell -> new ReadOnlyStringWrapper(studentExpiration(cell.getValue())));
            var profile = new TableColumn<ObservableList<String>,ObservableList<String>>("Acción");
            profile.setMinWidth(105);
            profile.setSortable(false);
            profile.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cell.getValue()));
            profile.setCellFactory(column -> new TableCell<>() {
                @Override protected void updateItem(ObservableList<String> student, boolean empty) {
                    super.updateItem(student,empty);
                    if (empty || student==null) {
                        setGraphic(null);
                        return;
                    }
                    var view = button("Ver perfil", "student-link", () -> {
                        table.getSelectionModel().select(student);
                        showStudentDrawer(drawer, table, student, filter);
                    });
                    view.getStyleClass().add("view-student");
                    setGraphic(view);
                }
            });
            table.getColumns().add(expiration);
            table.getColumns().add(profile);
            table.setPlaceholder(label("Aún no hay alumnos. Usa Nuevo alumno para comenzar.", "muted"));
            filtered.addListener((javafx.collections.ListChangeListener<ObservableList<String>>) change -> {
                if (drawer.isVisible() && !filtered.contains(table.getSelectionModel().getSelectedItem())) closeStudentDrawer(drawer);
            });
            table.getSelectionModel().selectedItemProperty().addListener((obs,old,student) -> {
                if (drawer.isVisible() && student!=null) showStudentDrawer(drawer, table, student, filter);
            });
            search.textProperty().addListener((obs,old,value) -> table.setPlaceholder(
                label(section.rows().isEmpty() ? "Aún no hay alumnos. Usa Nuevo alumno para comenzar."
                    : "No hay coincidencias con los filtros actuales.", "muted")));
        }
        var add=button(students?"Nuevo alumno":"Nuevo registro","primary",()->editRow(section,table,false)); add.setId("new-record");
        var edit=button("Editar","secondary",()->editRow(section,table,true)); edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        var export=button("Exportar CSV","secondary",()->export(section,table));
        var toolbar = new HBox(10,search,state,spacer(),export,edit,add); toolbar.setAlignment(Pos.CENTER_LEFT);
        if (students) {
            toolbar.getChildren().remove(edit);
            toolbar.getChildren().add(2, button("Limpiar filtros", "student-link", () -> {
                search.clear();
                state.getSelectionModel().selectFirst();
            }));
        }
        var actions = new HBox(10);
        if(module.name().equals("Asistencias")) {
            var code = new TextField(); code.setPromptText("Código de 4 dígitos"); code.setPrefWidth(180);
            code.setTextFormatter(new TextFormatter<String>(c->c.getControlNewText().matches("[0-9]{0,4}")?c:null));
            actions.getChildren().addAll(code,button("Registrar ingreso","primary",()->{
                var student=modules.get(1).sections().getFirst().rows().stream().filter(r->r.getFirst().equals(code.getText())).findFirst();
                if(student.isEmpty()) { message("Ingresa un código de alumno válido de cuatro dígitos."); return; }
                if(!student.get().getLast().equals("Activo")) { message("La membresía de este alumno está pausada."); return; }
                section.rows().add(FXCollections.observableArrayList(code.getText(),student.get().get(1),LocalDate.now(ZoneId.of("America/Lima")).toString(),java.time.LocalTime.now(ZoneId.of("America/Lima")).format(DateTimeFormatter.ofPattern("HH:mm")),"","En clase"));
                feedback.setText("Ingreso registrado en la vista previa. No se guardó en la base de datos."); code.clear();
            }));
        }
        actions.setAlignment(Pos.CENTER_LEFT);
        var count=label("", "muted"); count.textProperty().bind(javafx.beans.binding.Bindings.size(filtered).asString("%d registros"));
        var panel=new VBox(18,toolbar); panel.getStyleClass().add("table-panel");
        if(!actions.getChildren().isEmpty()) panel.getChildren().add(actions);
        if (students) {
            var body = new HBox(14, table, drawer);
            HBox.setHgrow(table,Priority.ALWAYS);
            panel.getChildren().addAll(body,count);
            VBox.setVgrow(body,Priority.ALWAYS);
        } else {
            panel.getChildren().addAll(table,count);
            VBox.setVgrow(table,Priority.ALWAYS);
        }
        return panel;
    }

    private PreviewData.Section profileSection(String module, String section) {
        return modules.stream().filter(item -> item.name().equals(module)).findFirst().orElseThrow()
            .sections().stream().filter(item -> item.name().equals(section)).findFirst().orElseThrow();
    }

    private List<ObservableList<String>> studentRecords(PreviewData.Section section, ObservableList<String> student) {
        if (section.columns().getFirst().equals("Código")) {
            return section.rows().stream().filter(row -> row.getFirst().equals(student.getFirst())).toList();
        }
        String name = student.get(1);
        // Las tablas de esta vista previa solo relacionan algunos registros por nombre.
        var students = profileSection("Alumnos", "Alumnos").rows();
        if (students.stream().filter(row -> row.get(1).equalsIgnoreCase(name)).count() != 1) {
            return List.of();
        }
        return section.rows().stream().filter(row -> row.getFirst().equalsIgnoreCase(name)).toList();
    }

    private Optional<ObservableList<String>> latestEnrollment(ObservableList<String> student) {
        return studentRecords(profileSection("Matrículas","Matrículas"),student).stream()
            .max(Comparator.comparing(row -> row.get(1)));
    }

    private String studentStatus(ObservableList<String> student) {
        return latestEnrollment(student).map(row -> {
            if ("Pausado".equalsIgnoreCase(student.getLast()) || "Pausada".equalsIgnoreCase(row.getLast())) return "Pausada";
            if (!row.get(2).isBlank() && LocalDate.parse(row.get(2)).isBefore(LocalDate.now(ZoneId.of("America/Lima")))) return "Vencida";
            return "Activo".equalsIgnoreCase(row.getLast()) ? "Activa" : row.getLast();
        }).orElse("Sin matrícula");
    }

    private String studentExpiration(ObservableList<String> student) {
        return latestEnrollment(student).map(row -> displayDate(row.get(2))).orElse("—");
    }

    private String displayDate(String value) {
        if (value.isBlank()) return "—";
        try {
            return LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        } catch (java.time.format.DateTimeParseException ex) {
            return value;
        }
    }

    private Label studentBadge(String state) {
        var badge = label(state, "status");
        if (List.of("Pausado","Pausada","Pendiente").contains(state)) badge.getStyleClass().add("status-warn");
        if (state.equals("Vencida")) badge.getStyleClass().add("student-danger");
        if (state.equals("Sin matrícula")) badge.getStyleClass().add("student-neutral");
        return badge;
    }

    private Label studentAvatar(ObservableList<String> student) {
        String initials = Arrays.stream(student.get(1).strip().split("\\s+"))
            .filter(word -> !word.isEmpty()).limit(2)
            .map(word -> word.substring(0,1).toUpperCase(Locale.ROOT)).reduce("",String::concat);
        return label(initials,"student-avatar");
    }

    private VBox studentCard(String title, Node... items) {
        var card = new VBox(12, label(title,"card-title"));
        card.getStyleClass().add("card");
        card.getChildren().addAll(items);
        return card;
    }

    private VBox studentField(String title, String value) {
        var field = new VBox(4,label(title,"student-field-title"),label(value.isBlank()?"—":value,"student-field-value"));
        field.getStyleClass().add("student-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private void closeStudentDrawer(VBox drawer) {
        drawer.setVisible(false);
        drawer.setManaged(false);
    }

    private void showStudentDrawer(VBox drawer, TableView<ObservableList<String>> table,
                                   ObservableList<String> student, Runnable filter) {
        var heading = new HBox(8,label("Ficha operativa","card-title"),spacer(),
            button("Cerrar","student-link",() -> closeStudentDrawer(drawer)));
        var identity = new HBox(10,studentAvatar(student),new VBox(4,label(student.get(1),"card-title"),
            label("Código " + student.getFirst(),"muted"),studentBadge(studentStatus(student))));
        identity.setAlignment(Pos.CENTER_LEFT);
        var open = button("Abrir perfil","primary",() -> {
            Node listing = workspace.getCenter();
            showStudentProfile(student, () -> {
                workspace.setCenter(listing);
                breadcrumb.setText("Academia   /   Alumnos");
                filter.run();
                table.refresh();
                if (table.getItems().contains(student)) showStudentDrawer(drawer,table,student,filter);
                else closeStudentDrawer(drawer);
            },0);
        });
        open.setId("open-student-profile");
        drawer.getChildren().setAll(heading,identity,
            studentField("VENCIMIENTO",studentExpiration(student)),
            studentField("TELÉFONO",student.get(3)),studentField("CORREO",student.get(4)),
            studentField("ASISTENCIAS REGISTRADAS",Integer.toString(studentRecords(profileSection("Asistencias","Ingresos"),student).size())),
            open);
        drawer.setManaged(true);
        drawer.setVisible(true);
    }

    private void showStudentProfile(ObservableList<String> student, Runnable back, int selectedTab) {
        breadcrumb.setText("Academia   /   Alumnos   /   Perfil");
        var members = profileSection("Matrículas","Matrículas");
        var pauses = profileSection("Alumnos","Congelaciones");
        var notes = profileSection("Alumnos","Historial");
        var attendance = profileSection("Asistencias","Ingresos");
        var payments = profileSection("Pagos","Pagos");
        var measurements = profileSection("Peso","Mediciones");

        var edit = button("Editar datos", "secondary", () -> {
            if (editRow(profileSection("Alumnos","Alumnos"), student, null)) {
                showStudentProfile(student, back, 0);
            }
        });
        var identity = new VBox(5,label(student.get(1),"student-profile-name"),
            label("Código " + student.getFirst(),"muted"));
        var header = new HBox(14,studentAvatar(student),identity,spacer(),studentBadge(studentStatus(student)));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("student-profile-header");

        var tabs = new TabPane();
        tabs.setId("student-profile-tabs");
        tabs.getStyleClass().add("student-profile-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        var details = new HBox(14,
            studentCard("Datos personales y contacto",studentField("DNI",student.get(2)),
                studentField("TELÉFONO",student.get(3)),studentField("CORREO",student.get(4)),edit),
            studentCard("Notas e historial", profileTable(notes,student,"Aún no hay notas ni cambios registrados."),
                profileAction("Agregar nota",notes,student,back,0)));
        details.setFillHeight(false);
        for (Node card : details.getChildren()) HBox.setHgrow(card,Priority.ALWAYS);
        var enrollment = latestEnrollment(student);
        var membership = studentCard("Vigencia de matrícula",
            studentField("INICIO",enrollment.map(row -> displayDate(row.get(1))).orElse("—")),
            studentField("VENCIMIENTO",studentExpiration(student)),
            profileAction("Registrar matrícula",members,student,back,1));
        if (enrollment.isPresent() && studentStatus(student).equals("Activa")) {
            membership.getChildren().add(button("Pausar membresía","secondary",() -> {
                if (editRow(pauses,null,student.get(1))) {
                    student.set(student.size()-1,"Pausado");
                    enrollment.get().set(enrollment.get().size()-1,"Pausada");
                    showStudentProfile(student,back,1);
                }
            }));
        }
        if (enrollment.isPresent() && studentStatus(student).equals("Pausada")) {
            membership.getChildren().add(button("Reactivar membresía","secondary",() -> {
                var confirm = new Alert(Alert.AlertType.CONFIRMATION,"¿Reactivar la membresía de "+student.get(1)+"?");
                confirm.initOwner(stage);
                if (confirm.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK) return;
                student.set(student.size()-1,"Activo");
                enrollment.get().set(enrollment.get().size()-1,"Activa");
                showStudentProfile(student,back,1);
            }));
        }
        var histories = new HBox(14, studentCard("Historial de matrículas",profileTable(members,student,"Aún no hay matrículas.")),
            studentCard("Pausas y congelaciones",profileTable(pauses,student,"Aún no hay pausas registradas.")));
        for (Node card : histories.getChildren()) HBox.setHgrow(card,Priority.ALWAYS);

        var weightRecords = studentRecords(measurements,student);
        var latest = weightRecords.stream().max(Comparator.comparing(row -> row.get(1)));
        var weight = studentCard("Seguimiento",
            studentField("PRÓXIMO CONTROL",latest.map(row -> displayDate(row.get(3))).orElse("—")),
            profileAction("Registrar medición",measurements,student,back,4));
        var weightPanel = new VBox(14,new HBox(14,weight,weightChart(weightRecords)),
            studentCard("Historial de mediciones",profileTable(measurements,student,"Aún no hay mediciones registradas.")));
        tabs.getTabs().addAll(new Tab("Resumen",details),new Tab("Matrículas",new VBox(14,membership,histories)),
            new Tab("Asistencias",studentCard("Registro de entradas",studentField("ASISTENCIAS REGISTRADAS",
                Integer.toString(studentRecords(attendance,student).size())),profileTable(attendance,student,"Aún no hay asistencias."))),
            new Tab("Pagos",studentCard("Historial de cobros",profileTable(payments,student,"Aún no hay pagos."),profileAction("Registrar pago",payments,student,back,3))),
            new Tab("Control físico / Peso",weightPanel));
        tabs.getSelectionModel().select(selectedTab);
        var backButton = button("‹ Volver a alumnos","student-link",back);
        backButton.setId("back-to-students");
        var content = new VBox(15,backButton,header,tabs);
        content.setPadding(new Insets(22,28,24,28));
        content.getStyleClass().add("student-profile");
        var scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        workspace.setCenter(scroll);
    }

    private Button profileAction(String text, PreviewData.Section section, ObservableList<String> student,
                                 Runnable back, int tab) {
        return button(text,"primary",() -> {
            if (editRow(section,null,student.get(1))) showStudentProfile(student,back,tab);
        });
    }

    private TableView<ObservableList<String>> profileTable(PreviewData.Section section,
                                                          ObservableList<String> student, String emptyText) {
        var table = new TableView<>(FXCollections.observableArrayList(studentRecords(section,student)));
        table.setPlaceholder(label(emptyText,"muted"));
        table.setPrefHeight(200);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        int first = List.of("Alumno", "Código").contains(section.columns().getFirst()) ? 1 : 0;
        if (section.columns().getFirst().equals("Código") && section.columns().get(1).equals("Alumno")) first = 2;
        for (int i=first;i<section.columns().size();i++) {
            int index = i;
            String title = section.columns().get(i);
            var column = new TableColumn<ObservableList<String>,String>(title);
            column.setCellValueFactory(cell -> {
                String value = cell.getValue().get(index);
                boolean date = List.of("Fecha","Inicio","Fin","Vencimiento","Próxima medición").contains(title);
                return new ReadOnlyStringWrapper(date ? displayDate(value) : value);
            });
            table.getColumns().add(column);
        }
        return table;
    }

    private Node weightChart(List<ObservableList<String>> records) {
        var card = studentCard("Evolución del peso");
        HBox.setHgrow(card,Priority.ALWAYS);
        var x = new javafx.scene.chart.CategoryAxis();
        var y = new javafx.scene.chart.NumberAxis();
        y.setForceZeroInRange(false);
        y.setLabel("kg");
        var series = new javafx.scene.chart.XYChart.Series<String,Number>();
        records.stream().filter(row -> !row.get(1).isBlank() && !row.get(2).isBlank())
            .sorted(Comparator.comparing(row -> row.get(1)))
            .forEach(row -> series.getData().add(new javafx.scene.chart.XYChart.Data<>(
                displayDate(row.get(1)),Double.parseDouble(row.get(2)))));
        if (series.getData().isEmpty()) {
            card.getChildren().add(label("Registra una medición para ver la evolución.","muted"));
        } else {
            var chart = new javafx.scene.chart.LineChart<>(x,y);
            chart.setLegendVisible(false);
            chart.setAnimated(false);
            chart.setPrefHeight(190);
            chart.getData().add(series);
            card.getChildren().add(chart);
        }
        return card;
    }

    private void editRow(PreviewData.Section section, TableView<ObservableList<String>> table, boolean edit) {
        var selected=table.getSelectionModel().getSelectedItem();
        if (!edit || selected!=null) editRow(section, edit ? selected : null, null);
    }
    private boolean editRow(PreviewData.Section section, ObservableList<String> selected, String studentName) {
        var result = RecordDialog.show(stage, section, selected, studentName);
        result.ifPresent(row -> {
            if (selected == null) section.rows().add(row);
            else if (section.name().equals("Alumnos")) selected.setAll(row);
            else section.rows().set(section.rows().indexOf(selected), row);
            feedback.setText("Registro guardado en " + section.name() + " durante esta sesión.");
        });
        return result.isPresent();
    }
    private void changeState(TableView<ObservableList<String>> table,String state) {
        var selected=table.getSelectionModel().getSelectedItem(); if(selected==null) { message("Selecciona un alumno en la tabla."); return; }
        selected.set(selected.size()-1,state); table.refresh();
        feedback.setText(selected.get(1)+": membresía "+(state.equals("Activo")?"reactivada":"pausada")+" en la vista previa.");
    }
    private void exportWorkbook() {
        var chooser = new FileChooser();
        chooser.setTitle("Descargar Excel de la sesión");
        chooser.setInitialFileName("Academia - Reporte completo.xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel", "*.xlsx"));
        var destination = chooser.showSaveDialog(stage);
        if (destination == null) {
            return;
        }
        try {
            ExcelExport.write(destination.toPath(), modules, dashboardView.movements());
            feedback.setText("Excel guardado en " + destination.getName());
        } catch (java.io.IOException ex) {
            message("No se pudo guardar el archivo Excel. " + ex.getMessage());
        }
    }

    private void export(PreviewData.Section section,TableView<ObservableList<String>> table) {
        var chooser=new FileChooser(); chooser.setTitle("Exportar CSV"); chooser.setInitialFileName(section.name()+".csv"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv"));
        var file=chooser.showSaveDialog(stage); if(file==null) return;
        var lines=new ArrayList<String>(); lines.add(csv(section.columns())); table.getItems().forEach(row->lines.add(csv(row)));
        try { Files.writeString(file.toPath(),"\uFEFF"+String.join("\r\n",lines),StandardCharsets.UTF_8); feedback.setText("Vista exportada: "+file.getName()); }
        catch(java.io.IOException ex) { message("No se pudo exportar el archivo: "+ex.getMessage()); }
    }
    private String csv(List<String> row) { return row.stream().map(v->"\""+v.replace("\"","\"\"")+"\"").collect(java.util.stream.Collectors.joining(";")); }

    private HBox financeMetrics(PreviewData.Section movements) {
        var income = movementTotal(movements, "Ingreso");
        var expenses = movementTotal(movements, "Egreso");
        return metrics(new String[][] {
            {"Ingresos", formatMoney(income), "Ingresos de la sesión"},
            {"Egresos", formatMoney(expenses), "Egresos de la sesión"},
            {"Balance", formatMoney(income.subtract(expenses)), "Balance de la sesión"}
        });
    }
    private java.math.BigDecimal movementTotal(PreviewData.Section movements, String type) {
        return movements.rows().stream()
            .filter(row -> row.get(2).equalsIgnoreCase(type))
            .map(row -> row.get(3))
            .filter(amount -> !amount.isBlank())
            .map(java.math.BigDecimal::new)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
    private String formatMoney(java.math.BigDecimal amount) {
        return "S/ " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
    private HBox metrics(String[][] values) { var row=new HBox(16); for(var v:values) { var card=card(label(v[0],"metric-label"),label(v[1],"metric-value"),label(v[2],"muted")); card.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(card,Priority.ALWAYS); row.getChildren().add(card); } return row; }
    private VBox card(Node...nodes) { var box=new VBox(12,nodes); box.getStyleClass().add("card"); return box; }
    private static Label label(String text,String style) { var label=new Label(text); label.getStyleClass().add(style); return label; }
    private static Region spacer() { var region=new Region(); HBox.setHgrow(region,Priority.ALWAYS); return region; }
    private static Button button(String text,String style,Runnable action) {
        var b=new Button(text); b.getStyleClass().add(style); b.setOnAction(e->action.run());
        String icon=switch(text) {
            case "Nuevo registro", "Nuevo alumno"->"plus";case "Editar"->"edit";case "Exportar CSV", "Descargar Excel completo"->"export";
            case "Pausar membresía"->"pause";case "Reactivar membresía"->"play";case "Registrar ingreso"->"Asistencias";
            default->null;
        };
        if(icon!=null){b.setGraphic(Icons.of(icon,15));b.setGraphicTextGap(7);}return b;
    }
    private void message(String text) { var alert=new Alert(Alert.AlertType.INFORMATION,text,ButtonType.OK); alert.initOwner(stage); alert.setHeaderText("Vista previa"); alert.showAndWait(); }

    private void saveSnapshot(javafx.scene.Parent node,java.nio.file.Path path) throws java.io.IOException {
        var image=node.snapshot(null,null); var pixels=image.getPixelReader();
        var buffer=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<buffer.getHeight();y++) for(int x=0;x<buffer.getWidth();x++) buffer.setRGB(x,y,pixels.getArgb(x,y));
        javax.imageio.ImageIO.write(buffer,"png",path.toFile());
    }

    /** Prueba del menú real y capturas de las doce vistas, sin acceso a SQLite. */
    private void testViews() {
        try {
            var directory=java.nio.file.Path.of("target","ui-preview"); Files.createDirectories(directory);
            var report=new ArrayList<String>();
            for(var module:modules) {
                var nav = navigation.get(module.name());
                if (nav == null) showModule(module); else nav.fire();
                root.applyCss(); root.layout();
                if(!module.name().equals(currentModule)) throw new AssertionError("Navegación incorrecta: "+module.name());
                if(!module.sections().isEmpty()) {
                    if(currentTable==null) throw new AssertionError("Tabla no visible: "+module.name());
                    currentSearch.setText("__sin_coincidencias__");
                    if(!currentTable.getItems().isEmpty()) throw new AssertionError("Filtro no aplicado");
                    currentSearch.clear();
                    if(currentTable.getItems().size()!=module.sections().getFirst().rows().size()) throw new AssertionError("Filtro no restablecido");
                    var tabs=(TabPane)root.lookup(".tab-pane");
                    for(var tab:tabs.getTabs()) { tabs.getSelectionModel().select(tab); root.applyCss(); root.layout(); var tabTable=findTable(tab.getContent()); int expected = tab.getText().equals("Alumnos") ? 1 : 0; if(tabTable==null || tabTable.getItems().size()!=expected) throw new AssertionError("Cantidad inicial inesperada: "+tab.getText()); }
                    tabs.getSelectionModel().selectFirst(); root.applyCss(); root.layout();
                }
                var image=root.snapshot(null,null); var pixels=image.getPixelReader();
                var buffered=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<buffered.getHeight();y++) for(int x=0;x<buffered.getWidth();x++) buffered.setRGB(x,y,pixels.getArgb(x,y));
                javax.imageio.ImageIO.write(buffered,"png",directory.resolve(module.name()+".png").toFile());
                report.add("OK · "+module.name()+" · navegación, paneles y búsqueda");
            }
            showModule(modules.getFirst()); root.applyCss(); root.layout();
            dashboardView.verifyInteractions();
            report.add("OK · Dashboard: búsqueda, estado, periodo y paginación");
            saveSnapshot(root,directory.resolve("Dashboard.png"));
            root.resize(1200,740); root.applyCss(); root.layout();
            saveSnapshot(root,directory.resolve("Dashboard-compacto.png"));
            root.resize(1360,880);
            showModule(modules.get(1)); root.applyCss(); root.layout();
            var before=modules.get(1).sections().getFirst().rows().size();
            Platform.runLater(()-> {
                try {
                    var window=javafx.stage.Window.getWindows().stream().filter(w->w!=stage && w.isShowing()).findFirst().orElseThrow();
                    var pane=(DialogPane)window.getScene().getRoot();
                    pane.applyCss(); pane.layout();
                    saveSnapshot(pane,directory.resolve("Formulario-alumno.png"));
                    String[] values={"123","Alumno de prueba","12345678","999111222","prueba@example.com"};
                    for(int i=0;i<values.length;i++) ((TextField)pane.lookup("#record-field-"+i)).setText(values[i]);
                    var save=(Button)pane.lookupButton(pane.getButtonTypes().stream()
                        .filter(type -> type.getButtonData()==ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow());
                    save.fire();
                    if(!window.isShowing()) throw new AssertionError("Formulario aceptó código inválido");
                    ((TextField)pane.lookup("#record-field-0")).setText("0039");
                    save.fire();
                } catch(Throwable ex) { ex.printStackTrace(); System.exit(1); }
            });
            var activeTabs=(TabPane)root.lookup(".tab-pane");
            ((Button)activeTabs.getSelectionModel().getSelectedItem().getContent().lookup("#new-record")).fire();
            if(modules.get(1).sections().getFirst().rows().size()!=before+1) throw new AssertionError("Formulario no agregó el alumno");
            report.add("OK · Formulario de alumno: rechaza código inválido y agrega registro válido");
            var studentRows = profileSection("Alumnos","Alumnos").rows();
            var addedStudent = studentRows.getLast();
            currentTable.getSelectionModel().select(addedStudent);
            changeState(currentTable,"Pausado");
            if(!addedStudent.getLast().equals("Pausado")) throw new AssertionError("Pausa fallida");
            changeState(currentTable,"Activo");
            if(!addedStudent.getLast().equals("Activo")) throw new AssertionError("Reactivación fallida");
            report.add("OK · Pausar y reactivar alumno en memoria");
            studentRows.remove(addedStudent);
            currentSearch.setText("0007");
            root.applyCss(); root.layout();
            ((Button) root.lookup(".view-student")).fire();
            root.applyCss(); root.layout();
            saveSnapshot(root,directory.resolve("Alumnos-ficha-lateral.png"));
            ((Button) root.lookup("#open-student-profile")).fire();
            root.applyCss(); root.layout();
            var profileTabs = (TabPane) root.lookup("#student-profile-tabs");
            if(profileTabs==null || profileTabs.getTabs().size()!=5) throw new AssertionError("Perfil incompleto");
            for (int i=0;i<profileTabs.getTabs().size();i++) {
                profileTabs.getSelectionModel().select(i);
                root.applyCss(); root.layout();
                saveSnapshot(root,directory.resolve("Alumno-perfil-"+(i+1)+".png"));
            }
            var measurements = profileSection("Peso","Mediciones").rows();
            var measurementDate = LocalDate.of(2026,11,10);
            Platform.runLater(() -> {
                try {
                    var modal = javafx.stage.Window.getWindows().stream()
                        .filter(window -> window!=stage && window.isShowing()).findFirst().orElseThrow();
                    var pane = (DialogPane)modal.getScene().getRoot();
                    pane.applyCss(); pane.layout();
                    saveSnapshot(pane,directory.resolve("Formulario-medicion.png"));
                    ((DatePicker)pane.lookup("#record-field-1")).setValue(measurementDate);
                    ((TextField)pane.lookup("#record-field-2")).setText("56,4");
                    var save = pane.getButtonTypes().stream().filter(type ->
                        type.getButtonData()==ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow();
                    ((Button)pane.lookupButton(save)).fire();
                    if (modal.isShowing()) throw new AssertionError("El formulario rechazó la fecha o el peso válido");
                } catch (Throwable ex) { ex.printStackTrace(); System.exit(1); }
            });
            var weightButton = profileTabs.getSelectionModel().getSelectedItem().getContent().lookupAll(".button").stream()
                .filter(node -> node instanceof Button button && button.getText().equals("Registrar medición"))
                .map(node -> (Button)node).findFirst().orElseThrow();
            weightButton.fire();
            if (measurements.size()!=1 || !measurements.getFirst().get(2).equals("56.4")
                || !measurements.getFirst().get(3).equals(measurementDate.plusDays(15).toString())) {
                throw new AssertionError("La medición debe guardarse con peso normalizado y próximo control");
            }
            measurements.clear();
            report.add("OK · Modal de medición: calendario, peso decimal y próximo control");
            root.applyCss(); root.layout();
            ((Button) root.lookup("#back-to-students")).fire();
            if(!currentSearch.getText().equals("0007") || currentTable.getItems().size()!=1) {
                throw new AssertionError("La búsqueda del listado debe mantenerse al regresar");
            }
            report.add("OK · Alumno 0007: ficha lateral, cinco pestañas y retorno con búsqueda");
            showModule(modules.get(1)); root.resize(1200,740); root.applyCss(); root.layout();
            saveSnapshot(root,directory.resolve("Menu-compacto.png"));
            report.add("OK · Vista compacta 1200 × 740");
            Files.write(directory.resolve("resultados.txt"),report,StandardCharsets.UTF_8);
            System.out.println(String.join("\n",report)); Platform.exit();
        } catch(Throwable ex) { ex.printStackTrace(); Platform.exit(); System.exit(1); }
    }
}
