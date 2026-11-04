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
        globalSearch.setOnAction(e -> modules.stream().filter(m->m.name().toLowerCase(Locale.ROOT).contains(globalSearch.getText().toLowerCase(Locale.ROOT))).findFirst().ifPresent(m->{showModule(m);globalSearch.clear();}));
        var date = new DatePicker(LocalDate.now(ZoneId.of("America/Lima"))); date.setEditable(false); date.getStyleClass().add("header-date"); date.setPrefWidth(150);
        var notifications=button("", "notification-button",()->message("Avisos de ejemplo: una membresía pausada y próximos controles de peso."));
        var dot=new javafx.scene.shape.Circle(3.5,javafx.scene.paint.Color.web("#e85a71"));
        var bellBox=new StackPane(Icons.of("bell",23),dot);StackPane.setAlignment(dot,Pos.TOP_RIGHT);notifications.setGraphic(bellBox);
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
            var button = new Button(module.name(),Icons.of(module.name(),21)); button.setGraphicTextGap(16);
            button.setId("nav-"+module.name()); button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("nav-button");
            button.setOnAction(e -> showModule(module));
            navigation.put(module.name(),button); box.getChildren().add(button);
        }
        var grow = new Region(); VBox.setVgrow(grow,Priority.ALWAYS);
        var tip=new VBox(9,label("Cada paso cuenta.","sidebar-tip-title"),label("Acompaña el progreso\nde tu comunidad.","sidebar-tip-copy"),button("Ver alumnos     →","sidebar-tip-button",()->showModule(modules.get(1))));
        tip.getStyleClass().add("sidebar-tip"); tip.setMinHeight(Region.USE_PREF_SIZE);
        box.getChildren().addAll(grow,tip,label("Datos de demostración · Sin conexión", "sidebar-note"));
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
        var intro = new HBox(heading, spacer(), label("●  Demostración", "demo-pill")); intro.setAlignment(Pos.CENTER_LEFT);
        var content = new VBox(22,intro); content.setPadding(new Insets(28,32,24,32));
        if(module.sections().isEmpty()) {
            dashboardView=new DashboardView(modules, name->modules.stream().filter(m->m.name().equals(name)).findFirst().ifPresent(this::showModule),this::export);
            intro.getChildren().removeLast(); intro.getChildren().add(dashboardView.periodControl());
            content.getChildren().add(dashboardView); VBox.setVgrow(dashboardView,Priority.ALWAYS);
        }
        else {
            if(module.name().equals("Finanzas")) content.getChildren().add(metrics(new String[][]{{"Ingresos","S/ 210.00","Yape y efectivo"},{"Egresos","S/ 20.00","Gastos registrados"},{"Balance","S/ 190.00","Periodo de ejemplo"}}));
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
        var search = new TextField(); search.setId("search"); search.setPromptText("Buscar en "+section.name().toLowerCase(Locale.ROOT)+"…"); search.setPrefWidth(260);
        var state = new ComboBox<String>(); state.getItems().add("Todos los estados");
        int stateIndex=section.columns().indexOf("Estado");
        if(stateIndex>=0) section.rows().stream().map(r->r.get(stateIndex)).distinct().sorted().forEach(state.getItems()::add);
        state.getSelectionModel().selectFirst(); state.setVisible(stateIndex>=0); state.setManaged(stateIndex>=0);
        var filtered = new FilteredList<>(section.rows(),r->true);
        Runnable filter=()->filtered.setPredicate(row -> row.stream().anyMatch(v->v.toLowerCase(Locale.ROOT).contains(search.getText().trim().toLowerCase(Locale.ROOT))) && (stateIndex<0 || state.getSelectionModel().getSelectedIndex()==0 || row.get(stateIndex).equals(state.getValue())));
        search.textProperty().addListener((o,a,b)->filter.run()); state.valueProperty().addListener((o,a,b)->filter.run());
        var table = new TableView<ObservableList<String>>(filtered); table.setId("table-"+section.name()); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new VBox(10,label("Sin resultados", "empty-title"),label("Prueba otra búsqueda o agrega un registro de ejemplo.","muted")));
        table.setMinHeight(220); table.setPrefHeight(420);
        for(int i=0;i<section.columns().size();i++) {
            int index=i; var column=new TableColumn<ObservableList<String>,String>(section.columns().get(i));
            column.setCellValueFactory(cell->new ReadOnlyStringWrapper(cell.getValue().get(index))); column.setMinWidth(85);
            if(i==0) column.setPrefWidth(160);
            if(i==stateIndex || section.columns().get(i).equals("Elegibilidad")) column.setCellFactory(c->new TableCell<>() {
                @Override protected void updateItem(String text, boolean empty) {
                    super.updateItem(text,empty); setText(null); setGraphic(null);
                    if(!empty && text!=null) { var badge=label(text,"status"); if(List.of("Pausado","Pausada","Pendiente").contains(text)) badge.getStyleClass().add("status-warn"); setGraphic(badge); }
                }
            });
            table.getColumns().add(column);
        }
        var add=button("Nuevo registro","primary",()->editRow(section,table,false)); add.setId("new-record");
        var edit=button("Editar","secondary",()->editRow(section,table,true)); edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        var export=button("Exportar CSV","secondary",()->export(section,table));
        var toolbar = new HBox(10,search,state,spacer(),export,edit,add); toolbar.setAlignment(Pos.CENTER_LEFT);
        var actions = new HBox(10);
        if(module.name().equals("Alumnos") && section.name().equals("Alumnos")) {
            actions.getChildren().addAll(button("Pausar membresía","secondary",()->changeState(table,"Pausado")),button("Reactivar membresía","secondary",()->changeState(table,"Activo")),label("Selecciona un alumno para cambiar su estado de ejemplo.","muted"));
        }
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
        var count=label("", "muted"); count.textProperty().bind(javafx.beans.binding.Bindings.size(filtered).asString("%d registros · datos de ejemplo"));
        var panel=new VBox(18,toolbar); panel.getStyleClass().add("table-panel");
        if(!actions.getChildren().isEmpty()) panel.getChildren().add(actions);
        panel.getChildren().addAll(table,count); VBox.setVgrow(table,Priority.ALWAYS);
        return panel;
    }

    private void editRow(PreviewData.Section section, TableView<ObservableList<String>> table, boolean edit) {
        var selected=table.getSelectionModel().getSelectedItem(); if(edit && selected==null) return;
        var dialog=new Dialog<ObservableList<String>>(); dialog.initOwner(stage); dialog.setTitle((edit?"Editar · ":"Nuevo · ")+section.name());
        var save=new ButtonType("Guardar en vista previa",ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(save,ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/academia/ui.css").toExternalForm());
        var form=new GridPane(); form.setHgap(18); form.setVgap(12); form.setPadding(new Insets(20));
        var fields=new ArrayList<TextField>();
        for(int i=0;i<section.columns().size();i++) { var field=new TextField(edit?selected.get(i):""); field.setPrefWidth(290); fields.add(field); form.add(new Label(section.columns().get(i)),0,i); form.add(field,1,i); }
        var error=label("Los cambios se conservan solo durante esta sesión.","muted"); error.setWrapText(true); error.setMaxWidth(450);
        fields.forEach(field -> field.textProperty().addListener((o,a,b) -> error.setText("Los cambios se conservan solo durante esta sesión.")));
        var body=new VBox(form,error); dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION,e->{
            String problem=validate(section,fields,selected,edit);
            if(problem!=null) { error.setText(problem); e.consume(); }
        });
        dialog.setResultConverter(b->b==save?FXCollections.observableArrayList(fields.stream().map(f->f.getText().trim()).toList()):null);
        dialog.showAndWait().ifPresent(row->{
            if(edit) section.rows().set(section.rows().indexOf(selected),row); else section.rows().add(row);
            feedback.setText("Registro actualizado en la vista previa de "+section.name()+".");
        });
    }
    private String validate(PreviewData.Section section,List<TextField> fields, ObservableList<String> selected, boolean edit) {
        if(fields.getFirst().getText().isBlank()) return "Completa el primer campo para continuar.";
        for(int i=0;i<fields.size();i++) {
            String name=section.columns().get(i), value=fields.get(i).getText().trim();
            if(name.equals("Código")) {
                if(!value.matches("[0-9]{4}")) return "El código debe tener exactamente cuatro dígitos.";
                if(section.name().equals("Alumnos") && section.rows().stream().anyMatch(r->r!=selected && r.getFirst().equals(value))) return "Ese código ya está en uso.";
            }
            if(name.equals("DNI") && !value.isEmpty() && !value.matches("[0-9]{8}")) return "El DNI debe tener ocho dígitos.";
            if((name.contains("(S/)")||name.equals("Peso (kg)")) && !value.isEmpty()) {
                try { if(new java.math.BigDecimal(value).signum()<0 || (name.equals("Peso (kg)") && new java.math.BigDecimal(value).signum()==0)) return "Ingresa un importe o peso válido."; }
                catch(NumberFormatException ex) { return "Usa un número válido, por ejemplo 120.00."; }
            }
            if(List.of("Fecha","Inicio","Vencimiento","Desde","Hasta","Próxima medición").contains(name) && !value.isEmpty()) {
                try { LocalDate.parse(value); } catch(java.time.DateTimeException ex) { return "Usa fechas válidas con formato AAAA-MM-DD."; }
            }
            if(name.equals("Estado") && value.isEmpty()) return "Completa el estado.";
        }
        return null;
    }
    private void changeState(TableView<ObservableList<String>> table,String state) {
        var selected=table.getSelectionModel().getSelectedItem(); if(selected==null) { message("Selecciona un alumno en la tabla."); return; }
        selected.set(selected.size()-1,state); table.refresh();
        feedback.setText(selected.get(1)+": membresía "+(state.equals("Activo")?"reactivada":"pausada")+" en la vista previa.");
    }
    private void export(PreviewData.Section section,TableView<ObservableList<String>> table) {
        var chooser=new FileChooser(); chooser.setTitle("Exportar vista de ejemplo"); chooser.setInitialFileName(section.name()+".csv"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv"));
        var file=chooser.showSaveDialog(stage); if(file==null) return;
        var lines=new ArrayList<String>(); lines.add(csv(section.columns())); table.getItems().forEach(row->lines.add(csv(row)));
        try { Files.writeString(file.toPath(),"\uFEFF"+String.join("\r\n",lines),StandardCharsets.UTF_8); feedback.setText("Vista exportada: "+file.getName()); }
        catch(java.io.IOException ex) { message("No se pudo exportar el archivo: "+ex.getMessage()); }
    }
    private String csv(List<String> row) { return row.stream().map(v->"\""+v.replace("\"","\"\"")+"\"").collect(java.util.stream.Collectors.joining(";")); }

    private HBox metrics(String[][] values) { var row=new HBox(16); for(var v:values) { var card=card(label(v[0],"metric-label"),label(v[1],"metric-value"),label(v[2],"muted")); card.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(card,Priority.ALWAYS); row.getChildren().add(card); } return row; }
    private VBox card(Node...nodes) { var box=new VBox(12,nodes); box.getStyleClass().add("card"); return box; }
    private static Label label(String text,String style) { var label=new Label(text); label.getStyleClass().add(style); return label; }
    private static Region spacer() { var region=new Region(); HBox.setHgrow(region,Priority.ALWAYS); return region; }
    private static Button button(String text,String style,Runnable action) {
        var b=new Button(text); b.getStyleClass().add(style); b.setOnAction(e->action.run());
        String icon=switch(text) {
            case "Nuevo registro"->"plus";case "Editar"->"edit";case "Exportar CSV"->"export";
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
                navigation.get(module.name()).fire(); root.applyCss(); root.layout();
                if(!module.name().equals(currentModule)) throw new AssertionError("Navegación incorrecta: "+module.name());
                if(!module.sections().isEmpty()) {
                    if(currentTable==null) throw new AssertionError("Tabla no visible: "+module.name());
                    currentSearch.setText("__sin_coincidencias__");
                    if(!currentTable.getItems().isEmpty()) throw new AssertionError("Filtro no aplicado");
                    currentSearch.clear();
                    if(currentTable.getItems().size()!=module.sections().getFirst().rows().size()) throw new AssertionError("Filtro no restablecido");
                    var tabs=(TabPane)root.lookup(".tab-pane");
                    for(var tab:tabs.getTabs()) { tabs.getSelectionModel().select(tab); root.applyCss(); root.layout(); if(findTable(tab.getContent())==null) throw new AssertionError("Subpanel sin tabla"); }
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
            currentTable.getSelectionModel().selectFirst(); changeState(currentTable,"Pausado");
            if(!currentTable.getItems().getFirst().getLast().equals("Pausado")) throw new AssertionError("Pausa fallida");
            changeState(currentTable,"Activo");
            if(!currentTable.getItems().getFirst().getLast().equals("Activo")) throw new AssertionError("Reactivación fallida");
            report.add("OK · Pausar y reactivar alumno en memoria");
            var before=modules.get(1).sections().getFirst().rows().size();
            Platform.runLater(()-> {
                try {
                    var window=javafx.stage.Window.getWindows().stream().filter(w->w!=stage && w.isShowing()).findFirst().orElseThrow();
                    var pane=(DialogPane)window.getScene().getRoot();
                    var grid=(GridPane)((VBox)pane.getContent()).getChildren().getFirst();
                    var fields=grid.getChildren().stream().filter(n->n instanceof TextField).map(n->(TextField)n).toList();
                    String[] values={"123","Alumno de prueba","12345678","999111222","prueba@example.com","Activo"};
                    for(int i=0;i<values.length;i++) fields.get(i).setText(values[i]);
                    var save=(Button)pane.lookupButton(pane.getButtonTypes().getFirst()); save.fire();
                    if(!window.isShowing()) throw new AssertionError("Formulario aceptó código inválido");
                    fields.getFirst().setText("0039");
                    pane.applyCss(); pane.layout();
                    saveSnapshot(pane,directory.resolve("Formulario-alumno.png"));
                    save.fire();
                } catch(Throwable ex) { ex.printStackTrace(); System.exit(1); }
            });
            var activeTabs=(TabPane)root.lookup(".tab-pane");
            ((Button)activeTabs.getSelectionModel().getSelectedItem().getContent().lookup("#new-record")).fire();
            if(modules.get(1).sections().getFirst().rows().size()!=before+1) throw new AssertionError("Formulario no agregó el alumno");
            report.add("OK · Formulario de alumno: rechaza código inválido y agrega registro válido");
            showModule(modules.get(1)); root.resize(1200,740); root.applyCss(); root.layout();
            saveSnapshot(root,directory.resolve("Menu-compacto.png"));
            report.add("OK · Vista compacta 1200 × 740");
            Files.write(directory.resolve("resultados.txt"),report,StandardCharsets.UTF_8);
            System.out.println(String.join("\n",report)); Platform.exit();
        } catch(Throwable ex) { ex.printStackTrace(); Platform.exit(); System.exit(1); }
    }
}
