package academia.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

/** Datos de presentación aislados de la persistencia. */
final class PreviewData {
    record Section(String name, List<String> columns, ObservableList<ObservableList<String>> rows) {}
    record Module(String name, String icon, String description, List<Section> sections) {}
    static Section section(String name, String columns, String... rows) {
        var items = FXCollections.<ObservableList<String>>observableArrayList();
        for (String row : rows) items.add(FXCollections.observableArrayList(row.split("\\|", -1)));
        return new Section(name, List.of(columns.split("\\|")), items);
    }
    static List<Module> modules() {
        return List.of(
            new Module("Dashboard", "◈", "Una mirada a tu academia, cada día.", List.of()),
            new Module("Alumnos", "◎", "Personas, membresías y nuevos comienzos.", List.of(
                section("Alumnos", "Código|Nombre|DNI|Teléfono|Correo|Estado", "0007|Sofía Demo||||Activo"),
                section("Congelaciones", "Alumno|Inicio|Fin|Motivo|Estado"),
                section("Historial", "Alumno|Fecha|Evento|Detalle"))),
            new Module("Matrículas", "▤", "Consulta la vigencia, los importes y las renovaciones.", List.of(
                section("Matrículas", "Alumno|Inicio|Vencimiento|Importe (S/)|Estado"),
                section("Renovaciones", "Alumno|Inicio|Vencimiento|Estado"))),
            new Module("Asistencias", "▦", "Cada ingreso cuenta. Identifica con cuatro dígitos.", List.of(
                section("Ingresos", "Código|Alumno|Fecha|Ingreso|Salida|Estado"))),
            new Module("Peso", "↗", "Acompaña el progreso con mediciones cada 15 días.", List.of(
                section("Mediciones", "Alumno|Fecha|Peso (kg)|Próxima medición|Observaciones"))),
            new Module("Pagos", "◇", "Cobros claros, por Yape o en efectivo.", List.of(
                section("Pagos", "Alumno|Concepto|Importe (S/)|Método|Referencia|Estado"))),
            new Module("Comprobantes", "▧", "Boletas locales organizadas en un solo lugar.", List.of(
                section("Boletas locales", "Serie|Número|Receptor|Documento|Total (S/)|Estado"))),
            new Module("Instructores", "♧", "El equipo que pone en movimiento la academia.", List.of(
                section("Instructores", "Nombre|DNI|Teléfono|Correo|Tarifa hora (S/)|Estado"),
                section("Horarios", "Instructor|Clase|Día|Inicio|Fin|Capacidad"),
                section("Asistencias", "Instructor|Fecha|Ingreso|Salida"),
                section("Remuneraciones", "Instructor|Desde|Hasta|Importe (S/)|Estado"))),
            new Module("Sorteos", "☆", "Celebra la constancia de tu comunidad.", List.of(
                section("Sorteos", "Nombre|Premio|Fecha|Asistencias mínimas|Estado"),
                section("Participantes", "Sorteo|Alumno|Asistencias|Elegibilidad"),
                section("Ganadores", "Sorteo|Alumno|Puesto|Fecha"))),
            new Module("Finanzas", "▥", "Tu caja y el balance de la academia, a la vista.", List.of(
                section("Movimientos", "Fecha|Concepto|Tipo|Importe (S/)|Método"),
                section("Caja", "Responsable|Apertura|Saldo inicial (S/)|Efectivo esperado (S/)|Estado"),
                section("Balance", "Periodo|Ingresos (S/)|Egresos (S/)|Balance (S/)"))),
            new Module("Reportes", "▱", "Explora tus datos y exporta la vista que necesitas.", List.of(
                section("Resumen", "Módulo|Periodo|Indicador|Valor"),
                section("Vencimientos", "Alumno|Vencimiento|Estado"))),
            new Module("Configuración", "⚙", "Los detalles que hacen tuya esta academia.", List.of(
                section("Academia", "Nombre|RUC|Dirección|Teléfono|Correo"),
                section("Planes", "Nombre|Modalidad|Duración (días)|Sesiones|Precio (S/)|Estado"),
                section("Parámetros", "Parámetro|Valor|Descripción")))
        );
    }
}
