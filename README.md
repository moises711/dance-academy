# Administración de la academia

Base para una aplicación de escritorio con JavaFX y SQLite. La primera pantalla comprueba la conexión local y muestra la ubicación de la base de datos. La base incluye 21 tablas para los módulos de administración y 7 vistas para consultas e indicadores. Las pantallas de gestión todavía deben implementarse.

## Ejecutar

Instala el JDK 21 o superior y Maven 3.9. Desde la carpeta que contiene el POM ejecuta:

```bash
mvn javafx:run
```

La primera ejecución requiere acceso a Maven Central para descargar dependencias. Abre la aplicación en una sesión gráfica. Una vez descargadas las dependencias, puedes arrancar con `mvn -o javafx:run`.

## Organización

| Ruta | Contenido |
| --- | --- |
| `src/main/java/academia` | Punto de entrada |
| `src/main/java/academia/ui` | Ventanas y pantallas JavaFX |
| `src/main/java/academia/db` | Inicialización de SQLite |
| `src/main/resources/academia` | Esquema inicial y recursos de la aplicación |
| `data/` | Base local, fuera del control de versiones |

Ejecuta siempre desde la raíz del proyecto para usar el mismo archivo `data/academia.db`. El primer arranque crea las tablas dentro de una transacción. Para respaldar los datos, cierra la aplicación y copia el archivo de base de datos. Si más adelante cambia la estructura de las tablas, agrega migraciones para actualizar las bases existentes.

También puedes crear la base sin abrir JavaFX con `python scripts/init_database.py`. Verifica el esquema con `python scripts/test_database.py`; las pruebas utilizan una base temporal en memoria.

## Módulos de la base

| Módulo | Tablas o vistas |
| --- | --- |
| Dashboard | `v_dashboard`, `v_alertas_vencimientos`, `v_finanzas_diarias`, `movimientos_caja` |
| Alumnos | `estudiantes`, `alumnos`, `historial_alumnos`, `congelaciones` |
| Matrículas | `planes`, `matriculas`, `v_matriculas` |
| Asistencias | `asistencias`, código numérico único de cuatro dígitos en `alumnos` |
| Peso | `mediciones_peso`, `v_control_peso` |
| Pagos | `pagos`, métodos Yape y efectivo |
| Comprobantes | `comprobantes`, numeración única por serie |
| Instructores | `instructores`, `horarios`, `asistencias_instructores`, `remuneraciones` |
| Sorteos | `sorteos`, `participantes_sorteo`, `ganadores_sorteo`, `v_elegibles_sorteo` |
| Finanzas | `cajas`, `movimientos_caja`, `v_balance_cajas`, `v_finanzas_diarias` |
| Reportes | Consultas sobre tablas y vistas; la exportación se implementa en la aplicación |
| Configuración | `academia`, `planes`, `parametros` |

## Reglas para la integración

Los importes se almacenan en céntimos de soles (12000 = S/ 120.00). Las fechas de negocio usan `YYYY-MM-DD` y las horas `HH:MM:SS`. Los registros con `CURRENT_TIMESTAMP` se guardan en UTC; las vistas diarias convierten a hora de Lima. Cada nueva conexión debe habilitar `PRAGMA foreign_keys = ON`.

El esquema conserva `estudiantes` y crea perfiles en `alumnos` para los registros existentes. Para nuevos alumnos, insertar ambas filas en una transacción. Registrar las ediciones y cambios de estado en `historial_alumnos`.

Al pausar una matrícula, crear una congelación abierta y cambiar el estado a `pausada` en la misma transacción. Al reactivarla, cerrar la congelación, registrar los días prorrogados, extender el vencimiento y cambiar el estado a `activa`. Solo se permite una congelación abierta por matrícula. Para renovar, crear otra matrícula indicando `renovacion_de`; conservar el precio y las sesiones contratadas como valores históricos.

Antes de admitir un ingreso, la aplicación debe comprobar la vigencia, las congelaciones y las sesiones disponibles. El campo `alumnos.codigo` identifica al alumno con cuatro dígitos, de `0000` a `9999`, conservando ceros iniciales. La aplicación debe reintentar con otro código si el generado ya existe. El método `qr` de asistencias se conserva únicamente por compatibilidad con registros históricos; los nuevos ingresos usan `manual`. La migración a versión 2 reemplaza los QR existentes por códigos únicos y conserva los demás datos. El intervalo de peso de 15 días es un recordatorio configurable, y permite mediciones adicionales.

Confirmar un pago y registrar su ingreso en caja en una sola transacción. Las finanzas se calculan sobre `movimientos_caja`, sin sumar de nuevo los pagos. El saldo esperado de efectivo excluye Yape. Al anular un pago ya registrado, agregar un egreso de devolución conservando el movimiento original. Al pagar una remuneración, registrar su egreso y actualizar su estado en una transacción. La aplicación debe validar que los movimientos correspondan a una caja abierta y que sus importes coincidan con el pago o remuneración asociado.

Los comprobantes son boletas de emisión local; esta base no implementa emisión electrónica SUNAT. Guardar los datos del receptor y el importe al emitirlos. Para sorteos, consultar `v_elegibles_sorteo` antes de registrar participantes y volver a validar al elegir ganadores. La selección aleatoria se implementa en la aplicación.

## Servicios Java del backend

- `AlumnoService` registra, busca, edita y cambia el estado de alumnos. Las ediciones y los cambios de estado se registran en el historial.
- `MatriculaService` crea planes y matrículas, renueva, congela, reactiva y registra ingresos. Los reintentos de ingreso deben reutilizar matrícula, horario, fecha y hora.
- `FinanzasService` gestiona caja, cobros, devoluciones, comprobantes locales y pago de remuneraciones. Las referencias Yape son únicas. Los cobros requieren una clave UUID que se reutiliza al reintentar la misma operación.
- `OperacionService` registra instructores, horarios, asistencias, remuneraciones y peso. También consulta las vistas y configura los intervalos.
- `SorteoService` comprueba elegibilidad al inscribir y al elegir ganadores.

El vencimiento incluye el último día del plan. Una congelación abarca desde el primer día de pausa hasta el día anterior a la reactivación.

Los servicios requieren inicializar la base con `Database.initialize()` al arrancar. La prueba `academia.BackendIntegration` se ejecuta desde una carpeta temporal vacía con las clases principales y de prueba, los recursos y SQLite JDBC con sus dependencias en el classpath.

## Recuperación local

Ejecuta `python3 scripts/backup_database.py` para crear un respaldo. Con `python3 scripts/backup_database.py --restaurar RUTA_DEL_RESPALDO` se genera una copia verificada por separado en `data/`. Para activarla, cierra todos los procesos que usen la base, conserva una copia de la base actual y sustituye `data/academia.db` por la copia verificada.

## Acceso local y auditoría

La migración a versión 4 incorpora usuarios, auditoría y claves de solicitud de cobro. Desde la raíz del proyecto, ejecuta la clase `academia.service.AccesoService` con el nombre de usuario como argumento y con SQLite JDBC y sus dependencias en el classpath. La clase solicita la contraseña en una terminal y crea el primer administrador una sola vez.

Los servicios Java requieren una sesión de `AccesoService.autenticar`. La sesión vence a las ocho horas o al cerrarla. Las cuentas desactivadas pierden acceso en la siguiente operación. `ADMIN` puede administrar usuarios, planes y configuración, cerrar cajas, devolver pagos, gestionar remuneraciones y consultar auditoría. `OPERADOR` puede registrar las operaciones diarias y consultar datos operativos. `CONSULTA` tiene acceso al resumen general.

Las escrituras de los servicios guardan el usuario y la operación en la misma transacción. Las consultas de auditoría se paginan usando el identificador del último resultado como límite. Los permisos se aplican a través de los servicios Java. El acceso directo al archivo SQLite depende de los permisos del sistema operativo.

Genera y conserva un UUID por cada solicitud de cobro. El mismo UUID y los mismos datos devuelven el identificador original, incluso después de cerrar la caja. Si cambias algún dato del cobro, se rechaza la reutilización de la clave.
