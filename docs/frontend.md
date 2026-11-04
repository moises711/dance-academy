# Vista previa del frontend JavaFX

La interfaz contiene los 12 módulos: Dashboard, Alumnos, Matrículas, Asistencias,
Peso, Pagos, Comprobantes, Instructores, Sorteos, Finanzas, Reportes y Configuración.
Cada módulo incluye sus paneles secundarios, tablas y formularios de ejemplo.

Esta versión trabaja exclusivamente con datos de demostración en memoria.
No abre SQLite, no inicializa la base de datos y no guarda cambios de negocio.
La exportación CSV permite guardar la tabla filtrada en un archivo elegido por el usuario.

## Abrir la aplicación

Con JDK 21 y las dependencias JavaFX 21.0.8 descargadas en Maven:

```powershell
./scripts/preview-ui.ps1
```

Si el JDK no está configurado:

```powershell
./scripts/preview-ui.ps1 -JdkHome 'C:/ruta/al/jdk-21'
```

El comando compila solamente el frontend y el punto de entrada. Los recursos
se generan en `target/ui-classes`. También se puede usar `mvn javafx:run`.

## Pruebas de vistas

```powershell
./scripts/preview-ui.ps1 -TestViews
```

La prueba abre JavaFX, recorre los botones del menú, verifica las tablas y
pestañas, prueba búsquedas sin resultados y su restablecimiento, y comprueba
pausa/reactivación en memoria. También verifica que el formulario rechace
un código de tres dígitos y permita agregar un alumno con cuatro dígitos.

Genera las capturas reales de los 12 módulos, el formulario de alumnos, una
vista compacta y el informe `target/ui-preview/resultados.txt`.

## Revisión manual

- Recorrer el menú y las pestañas de cada módulo.
- Buscar por nombre, DNI o código y filtrar por los estados de ejemplo.
- Seleccionar un alumno, editarlo y pausar/reactivar su estado de ejemplo.
- En Asistencias, probar `0007` y comprobar el registro local del ingreso.
- Crear un registro y comprobar los mensajes de validación del formulario.
- Exportar una tabla y abrir el CSV para comprobar encabezados y acentos.
- Cerrar y volver a abrir: los datos vuelven al ejemplo inicial.

La validación de membresías reales, las renovaciones, la emisión de comprobantes,
la elegibilidad real y las operaciones de caja quedan pendientes de integración
con el backend. Los indicadores del dashboard son ejemplos de diseño.

## Dashboard de la academia

El dashboard sigue la referencia visual entregada: menú lateral azul, barra de
búsqueda con acceso Ctrl+K, fecha, avisos, perfil, cuatro tarjetas de indicadores
y tabla de movimientos con estados de colores.

La búsqueda superior filtra los movimientos visibles. Al escribir el nombre de
un módulo y pulsar Enter, abre ese módulo. Los filtros de periodo y estado se
aplican a los movimientos de demostración; las tarjetas muestran los totales de
los datos de ejemplo de la sesión, independientemente del filtro de la tabla.
Los gráficos pequeños son ilustrativos. La exportación incluye todos los
movimientos filtrados, aunque ocupen más de una página.

Las pruebas verifican búsqueda, filtro de estado, periodo, cambio de página y
cantidad de filas. También generan `target/ui-preview/Dashboard-compacto.png`.
