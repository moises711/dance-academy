# Administración de la academia

Base para una aplicación de escritorio con JavaFX y SQLite. La primera pantalla comprueba la conexión local y muestra la ubicación de la base de datos. El esquema inicial incluye la tabla de estudiantes. Las pantallas de gestión se pueden incorporar a medida que se definan sus flujos.

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

Ejecuta siempre desde la raíz del proyecto para usar el mismo archivo `data/academia.db`. SQLite crea el archivo y la tabla en el primer arranque. Para respaldar los datos, cierra la aplicación y copia el archivo de base de datos. Si más adelante cambia la estructura de las tablas, agrega migraciones para actualizar las bases existentes.
