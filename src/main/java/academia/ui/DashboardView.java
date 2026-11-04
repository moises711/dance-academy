package academia.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Dashboard de presentación. Todo el estado se conserva exclusivamente en memoria. */
final class DashboardView extends VBox {
    private final LocalDate today=LocalDate.now(ZoneId.of("America/Lima"));
    private final ComboBox<String> period=new ComboBox<>();
    private final ComboBox<String> status=new ComboBox<>();
    private final ComboBox<Integer> pageSize=new ComboBox<>();
    private final HBox pages=new HBox(6);
    private int pageIndex;
    private int pageCount=1;
    private final TableView<ObservableList<String>> table=new TableView<>();
    private final Label count=new Label();
    private final HBox indicators=new HBox(18);
    private final PreviewData.Section section;
    private final FilteredList<ObservableList<String>> filtered;
    private final List<PreviewData.Module> modules;
    private String query="";

    DashboardView(List<PreviewData.Module> modules, Consumer<String> navigate,
                  BiConsumer<PreviewData.Section,TableView<ObservableList<String>>> export) {
        this.modules=modules; setSpacing(24);
        section=PreviewData.section("Movimientos recientes","Código|Alumno|Concepto|Método|Estado|Fecha",
            "0007|Ana Torres|Mensualidad|Yape|Confirmado|"+today,
            "0024|Luis Mendoza|8 sesiones|Efectivo|Confirmado|"+today,
            "0108|María Flores|Congelación|—|Pausada|"+today.minusDays(1),
            "0007|Ana Torres|Próxima renovación|—|Pendiente|"+today.minusDays(1),
            "0024|Luis Mendoza|Reserva de clase|—|Cancelado|"+today.minusDays(2),
            "0007|Ana Torres|Control de peso|—|Registrado|"+today.minusDays(3),
            "0024|Luis Mendoza|Ingreso a clase|Código|Registrado|"+today.minusDays(4),
            "0108|María Flores|Matrícula|—|Registrado|"+today.minusDays(8));
        filtered=new FilteredList<>(section.rows(),r->true);
        period.getItems().addAll("Últimos 7 días","Hoy","Últimos 30 días","Todo el historial"); period.getSelectionModel().selectFirst();
        period.getStyleClass().add("period-control"); period.setId("dashboard-period");
        period.valueProperty().addListener((o,a,b)->refresh());
        status.getItems().addAll("Todos los estados","Confirmado","Pendiente","Pausada","Cancelado","Registrado"); status.getSelectionModel().selectFirst();
        status.setId("dashboard-status"); status.getStyleClass().add("dashboard-filter");
        status.valueProperty().addListener((o,a,b)->refresh());
        var exportButton=new Button("Exportar",Icons.of("export",16)); exportButton.getStyleClass().add("primary");
        exportButton.setOnAction(e->{ var view=new TableView<ObservableList<String>>(FXCollections.observableArrayList(filtered)); export.accept(section,view); });
        var title=new Label("Movimientos recientes",Icons.of("list",22)); title.getStyleClass().add("recent-title");
        var grow=new Region(); HBox.setHgrow(grow,Priority.ALWAYS);
        var tools=new HBox(12,title,grow,status,exportButton); tools.setAlignment(Pos.CENTER_LEFT); tools.getStyleClass().add("recent-toolbar");
        table.setId("dashboard-movements"); table.getStyleClass().add("dashboard-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(52); table.setMinHeight(310); table.setPrefHeight(310);
        table.setPlaceholder(new Label("No hay movimientos para estos filtros."));
        double[] widths={100,155,170,105,125,130};
        for(int i=0;i<section.columns().size();i++) {
            final int index=i;
            var col=new TableColumn<ObservableList<String>,String>(section.columns().get(i));
            col.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().get(index)));
            col.setPrefWidth(widths[i]); col.setMinWidth(i==1?125:85);
            if(i==0) col.setCellFactory(c->new TableCell<>() {
                @Override protected void updateItem(String value,boolean empty) { super.updateItem(value,empty); setText(empty?null:value); getStyleClass().remove("code-cell"); if(!empty)getStyleClass().add("code-cell"); }
            });
            if(i==4) col.setCellFactory(c->new TableCell<>() {
                @Override protected void updateItem(String value,boolean empty) {
                    super.updateItem(value,empty); setText(null); setGraphic(null);
                    if(!empty && value!=null) { var badge=new Label("●  "+value.toUpperCase(Locale.ROOT)); badge.getStyleClass().add("movement-status"); badge.getStyleClass().add(switch(value){case "Pendiente","Pausada"->"movement-warn";case "Cancelado"->"movement-danger";case "Registrado"->"movement-info";default->"movement-success";}); setGraphic(badge); }
                }
            });
            if(i==5) col.setCellFactory(c->new TableCell<>() {
                @Override protected void updateItem(String value,boolean empty) {
                    super.updateItem(value,empty); setText(null);setGraphic(null);
                    if(!empty && value!=null) { var date=new Label(LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd MMM yyyy",Locale.forLanguageTag("es-PE")))); date.getStyleClass().add("movement-date"); var note=new Label("Vista previa");note.getStyleClass().add("movement-time");setGraphic(new VBox(3,date,note)); }
                }
            });
            table.getColumns().add(col);
        }
        var actions=new TableColumn<ObservableList<String>,String>("Acciones"); actions.setMinWidth(78);actions.setMaxWidth(85);actions.setSortable(false);
        actions.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().getFirst()));
        actions.setCellFactory(c->new TableCell<>() {
            @Override protected void updateItem(String value,boolean empty) {
                super.updateItem(value,empty);setGraphic(null);setText(null);
                if(!empty) {var menu=new MenuButton("",Icons.of("more",18));menu.setAccessibleText("Acciones del movimiento");menu.getStyleClass().add("row-actions");var student=new MenuItem("Ver alumnos");student.setOnAction(e->navigate.accept("Alumnos"));var payments=new MenuItem("Ver pagos");payments.setOnAction(e->navigate.accept("Pagos"));menu.getItems().addAll(student,payments);setGraphic(menu);}
            }
        });table.getColumns().add(actions);
        pages.setAlignment(Pos.CENTER);pages.getStyleClass().add("movement-pages");pages.setId("dashboard-pages");
        pageSize.getItems().addAll(5,10,20);pageSize.getSelectionModel().selectFirst();pageSize.setId("dashboard-page-size");
        pageSize.valueProperty().addListener((o,a,b)->refresh());
        count.getStyleClass().add("pagination-caption");
        var right=new HBox(10,new Label("Filas por página"),pageSize);right.setAlignment(Pos.CENTER_RIGHT);
        var bottom=new BorderPane();bottom.setLeft(count);bottom.setCenter(pages);bottom.setRight(right);BorderPane.setAlignment(count,Pos.CENTER_LEFT);bottom.getStyleClass().add("recent-footer");
        var panel=new VBox(tools,table,bottom);panel.getStyleClass().add("recent-panel");panel.setMinHeight(Region.USE_PREF_SIZE);
        var note=new Label("DATOS DE DEMOSTRACIÓN  ·  ACADEMIA DE BAILE");note.getStyleClass().add("dashboard-note");note.setMaxWidth(Double.MAX_VALUE);note.setAlignment(Pos.CENTER);
        getChildren().addAll(indicators,panel,note);refresh();
    }
    Node periodControl(){return period;}
    void search(String value){query=value.trim().toLowerCase(Locale.ROOT);refresh();}
    private void refresh(){
        LocalDate start=switch(period.getSelectionModel().getSelectedIndex()){case 1->today;case 2->today.minusDays(29);case 3->LocalDate.MIN;default->today.minusDays(6);};
        filtered.setPredicate(r->!LocalDate.parse(r.get(5)).isBefore(start) && (status.getSelectionModel().getSelectedIndex()==0||r.get(4).equals(status.getValue())) && r.stream().anyMatch(v->v.toLowerCase(Locale.ROOT).contains(query)));
        pageCount=Math.max(1,(filtered.size()+pageSize.getValue()-1)/pageSize.getValue());pageIndex=0;showPage();
        indicators.getChildren().clear();
        long active=modules.get(1).sections().getFirst().rows().stream().filter(r->r.getLast().equals("Activo")).count();
        long enrollments=modules.get(2).sections().getFirst().rows().stream().filter(r->r.getLast().equals("Activa")).count();
        var payments=modules.get(5).sections().getFirst().rows();
        var total=payments.stream().filter(r->r.getLast().equals("Confirmado")).map(r->new java.math.BigDecimal(r.get(2))).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
        indicators.getChildren().addAll(metric("Alumnos","Alumnos activos",String.valueOf(active),"blue","Estado actual",new double[]{2,3,2,4,5,4,6}),metric("Matrículas","Matrículas activas",String.valueOf(enrollments),"green","Estado actual",new double[]{2,4,3,5,4,4,5}),metric("clock","Pagos pendientes",String.valueOf(payments.stream().filter(r->r.getLast().equals("Pendiente")).count()),"amber","Estado actual",new double[]{5,4,6,4,2,3,2}),metric("money","Cobros confirmados","S/ "+total.toPlainString(),"purple","Total de pagos de ejemplo",new double[]{2,3,2,5,6,4,5}));
    }
    private Node metric(String icon,String name,String value,String color,String description,double[] points){
        var symbol=new StackPane(Icons.of(icon,24));symbol.getStyleClass().addAll("summary-icon","summary-icon-"+color);
        var title=new Label(name);title.getStyleClass().add("summary-label");
        var number=new Label(value);number.getStyleClass().add("summary-value");
        var top=new HBox(12,symbol,new VBox(3,title,number));top.setAlignment(Pos.CENTER_LEFT);
        var trend=new Label(description);trend.getStyleClass().add("summary-description");
        var graph=new Canvas(65,28);var gc=graph.getGraphicsContext2D();
        Color stroke=Color.web(switch(color){case "green"->"#5b9d7f";case "amber"->"#d7a158";case "purple"->"#af98d0";default->"#7e9ac4";});
        gc.setFill(stroke.deriveColor(0,1,1,.13));gc.beginPath();gc.moveTo(0,28);for(int i=0;i<points.length;i++)gc.lineTo(i*10,27-points[i]*3.5);gc.lineTo(60,28);gc.closePath();gc.fill();
        gc.setStroke(stroke);gc.setLineWidth(1.5);gc.beginPath();for(int i=0;i<points.length;i++){if(i==0)gc.moveTo(0,27-points[i]*3.5);else gc.lineTo(i*10,27-points[i]*3.5);}gc.stroke();
        var gap=new Region();HBox.setHgrow(gap,Priority.ALWAYS);var bottom=new HBox(5,trend,gap,graph);bottom.setAlignment(Pos.CENTER_LEFT);
        var card=new VBox(16,top,bottom);card.getStyleClass().add("summary-card");card.setMinWidth(0);card.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(card,Priority.ALWAYS);return card;
    }
    private void showPage(){
        int start=Math.min(pageIndex*pageSize.getValue(),filtered.size()),end=Math.min(start+pageSize.getValue(),filtered.size());
        table.setItems(FXCollections.observableArrayList(filtered.subList(start,end)));
        count.setText("Mostrando "+(filtered.isEmpty()?0:start+1)+" a "+end+" de "+filtered.size()+" registros");
        pages.getChildren().clear();
        var previous=new Button("‹");previous.getStyleClass().add("page-button");previous.setDisable(pageIndex==0);previous.setOnAction(e->setPage(pageIndex-1));pages.getChildren().add(previous);
        for(int i=0;i<pageCount;i++){ final int page=i;var number=new Button(String.valueOf(i+1));number.getStyleClass().add("page-button");if(i==pageIndex)number.getStyleClass().add("current-page");number.setOnAction(e->setPage(page));pages.getChildren().add(number); }
        var next=new Button("›");next.getStyleClass().add("page-button");next.setDisable(pageIndex==pageCount-1);next.setOnAction(e->setPage(pageIndex+1));pages.getChildren().add(next);
    }
    private void setPage(int page){pageIndex=Math.max(0,Math.min(page,pageCount-1));showPage();}
    void verifyInteractions(){
        if(table.getItems().size()!=5)throw new AssertionError("Dashboard: tamaño inicial de página");
        setPage(1);if(table.getItems().size()!=2)throw new AssertionError("Dashboard: segunda página");
        search("__sin_resultados__");if(!table.getItems().isEmpty())throw new AssertionError("Dashboard: búsqueda");
        search("");status.setValue("Confirmado");if(table.getItems().size()!=2)throw new AssertionError("Dashboard: filtro de estado");
        status.getSelectionModel().selectFirst();period.setValue("Hoy");if(table.getItems().size()!=2)throw new AssertionError("Dashboard: filtro de fecha");
        period.setValue("Todo el historial");pageSize.setValue(10);if(table.getItems().size()!=8)throw new AssertionError("Dashboard: tamaño de página");
        period.getSelectionModel().selectFirst();pageSize.setValue(5);
    }
}
