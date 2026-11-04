package academia.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
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
        String today = LocalDate.now(java.time.ZoneId.of("America/Lima")).toString();
        return List.of(
            new Module("Dashboard", "◈", "Una mirada a tu academia, cada día.", List.of()),
            new Module("Alumnos", "◎", "Personas, membresías y nuevos comienzos.", List.of(
                section("Alumnos", "Código|Nombre|DNI|Teléfono|Correo|Estado", "0007|Ana Torres|74829163|987 654 321|ana@example.com|Activo", "0024|Luis Mendoza|71938462|986 321 450|luis@example.com|Activo", "0108|María Flores|73281945|975 432 100|maria@example.com|Pausado"),
                section("Congelaciones", "Alumno|Inicio|Fin|Motivo|Estado", "María Flores|2026-11-02||Viaje|Pausado"),
                section("Historial", "Alumno|Fecha|Evento|Detalle", "Ana Torres|2026-11-01|Renovación|Plan mensual", "María Flores|2026-11-02|Pausa|Viaje"))),
            new Module("Matrículas", "▤", "Planes que acompañan el ritmo de tus alumnos.", List.of(
                section("Matrículas", "Alumno|Plan|Inicio|Vencimiento|Importe (S/)|Estado", "Ana Torres|Mensual libre|2026-11-01|2026-11-30|120.00|Activa", "Luis Mendoza|8 sesiones|2026-11-01|2026-11-30|90.00|Activa", "María Flores|Mensual libre|2026-10-15|2026-11-14|120.00|Pausada"),
                section("Renovaciones", "Alumno|Plan|Inicio|Vencimiento|Estado", "Ana Torres|Mensual libre|2026-12-01|2026-12-30|Pendiente"))),
            new Module("Asistencias", "▦", "Cada ingreso cuenta. Identifica con cuatro dígitos.", List.of(
                section("Ingresos", "Código|Alumno|Fecha|Ingreso|Salida|Estado", "0007|Ana Torres|"+today+"|09:02|10:00|Registrado", "0024|Luis Mendoza|"+today+"|09:10||En clase"))),
            new Module("Peso", "↗", "Acompaña el progreso con mediciones cada 15 días.", List.of(
                section("Mediciones", "Alumno|Fecha|Peso (kg)|Próxima medición|Observaciones", "Ana Torres|2026-11-01|62.5|2026-11-16|Control quincenal", "Luis Mendoza|2026-11-01|74.2|2026-11-16|Control quincenal"))),
            new Module("Pagos", "◇", "Cobros claros, por Yape o en efectivo.", List.of(
                section("Pagos", "Alumno|Concepto|Importe (S/)|Método|Referencia|Estado", "Ana Torres|Mensualidad|120.00|Yape|YP-0001|Confirmado", "Luis Mendoza|Sesiones|90.00|Efectivo||Confirmado"))),
            new Module("Comprobantes", "▧", "Boletas locales organizadas en un solo lugar.", List.of(
                section("Boletas locales", "Serie|Número|Receptor|Documento|Total (S/)|Estado", "LOCAL|000001|Ana Torres|74829163|120.00|Emitido", "LOCAL|000002|Luis Mendoza|71938462|90.00|Emitido"))),
            new Module("Instructores", "♧", "El equipo que pone en movimiento la academia.", List.of(
                section("Instructores", "Nombre|DNI|Teléfono|Correo|Tarifa hora (S/)|Estado", "Carla Ríos|71234986|955 321 640|carla@example.com|35.00|Activo", "Diego Castro|72349861|944 230 510|diego@example.com|40.00|Activo"),
                section("Horarios", "Instructor|Clase|Día|Inicio|Fin|Capacidad", "Carla Ríos|Salsa inicial|Lunes|09:00|10:00|20", "Diego Castro|Bachata|Miércoles|18:00|19:00|18"),
                section("Asistencias", "Instructor|Fecha|Ingreso|Salida", "Carla Ríos|"+today+"|08:45|10:15"),
                section("Remuneraciones", "Instructor|Desde|Hasta|Importe (S/)|Estado", "Carla Ríos|2026-11-01|2026-11-15|350.00|Pendiente"))),
            new Module("Sorteos", "☆", "Celebra la constancia de tu comunidad.", List.of(
                section("Sorteos", "Nombre|Premio|Fecha|Asistencias mínimas|Estado", "Baila y gana|Un mes gratis|2026-11-30|8|Abierto"),
                section("Participantes", "Sorteo|Alumno|Asistencias|Elegibilidad", "Baila y gana|Ana Torres|10|Elegible", "Baila y gana|Luis Mendoza|8|Elegible"),
                section("Ganadores", "Sorteo|Alumno|Puesto|Fecha"))),
            new Module("Finanzas", "▥", "Tu caja y el balance de la academia, a la vista.", List.of(
                section("Movimientos", "Fecha|Concepto|Tipo|Importe (S/)|Método", today+"|Mensualidad Ana|Ingreso|120.00|Yape", today+"|Sesiones Luis|Ingreso|90.00|Efectivo", today+"|Limpieza|Egreso|20.00|Efectivo"),
                section("Caja", "Responsable|Apertura|Saldo inicial (S/)|Efectivo esperado (S/)|Estado", "Administración|"+today+"|100.00|170.00|Abierta"),
                section("Balance", "Periodo|Ingresos (S/)|Egresos (S/)|Balance (S/)", "Noviembre|210.00|20.00|190.00"))),
            new Module("Reportes", "▱", "Explora tus datos y exporta la vista que necesitas.", List.of(
                section("Resumen", "Módulo|Periodo|Indicador|Valor", "Alumnos|Noviembre|Activos|2", "Pagos|Noviembre|Cobros confirmados|S/ 210.00", "Asistencias|Hoy|Ingresos|2"),
                section("Vencimientos", "Alumno|Plan|Vencimiento|Estado", "María Flores|Mensual libre|2026-11-14|Pausada"))),
            new Module("Configuración", "⚙", "Los detalles que hacen tuya esta academia.", List.of(
                section("Academia", "Nombre|RUC|Dirección|Teléfono|Correo", "Academia de baile||Lima, Perú|01 555 0100|hola@example.com"),
                section("Planes", "Nombre|Modalidad|Duración (días)|Sesiones|Precio (S/)|Estado", "Mensual libre|Mensualidad|30||120.00|Activo", "8 sesiones|Sesiones|30|8|90.00|Activo"),
                section("Parámetros", "Parámetro|Valor|Descripción", "Intervalo de peso|15|Días entre mediciones", "Alerta de vencimiento|7|Días de anticipación")))
        );
    }
}
