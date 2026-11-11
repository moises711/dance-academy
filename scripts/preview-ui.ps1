param(
    [switch]$TestViews,
    [switch]$CompileOnly,
    [string]$ClassesDirectory,
    [string]$JdkHome = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $JdkHome) {
    $javaCompiler = Get-Command javac -ErrorAction SilentlyContinue
    if ($javaCompiler) { $JdkHome = Split-Path (Split-Path $javaCompiler.Source -Parent) -Parent }
    elseif (Test-Path 'C:/Program Files/Android/Android Studio/jbr/bin/javac.exe') { $JdkHome = 'C:/Program Files/Android/Android Studio/jbr' }
    else { throw 'Configura JAVA_HOME con un JDK 21 o superior.' }
}
$fxJars = @(Get-ChildItem "$env:USERPROFILE/.m2/repository/org/openjfx" -Recurse -Filter '*-21.0.8-win.jar')
if ($fxJars.Count -lt 3) { throw 'Faltan dependencias JavaFX 21.0.8. Ejecuta mvn compile primero.' }
$modulePath = $fxJars.FullName -join ';'
$classes = if ($ClassesDirectory) { $ClassesDirectory } else { Join-Path $projectRoot 'target/ui-classes' }
New-Item -ItemType Directory -Force $classes | Out-Null
$sources = @((Get-ChildItem "$projectRoot/src/main/java/academia/ui" -Filter '*.java').FullName)
$sources += "$projectRoot/src/main/java/academia/Main.java"
& "$JdkHome/bin/javac.exe" -encoding UTF-8 --release 21 --module-path $modulePath --add-modules javafx.controls -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de la interfaz.' }
New-Item -ItemType Directory -Force "$classes/academia" | Out-Null
Copy-Item "$projectRoot/src/main/resources/academia/ui.css" "$classes/academia/ui.css" -Force
if ($CompileOnly) { return }
$javaArguments = @("-Djavafx.cachedir=$projectRoot/target/javafx-cache", '--module-path', $modulePath, '--add-modules', 'javafx.controls', '-cp', $classes, 'academia.Main')
if ($TestViews) { $javaArguments += '--test-views' }
Push-Location $projectRoot
try {
    & "$JdkHome/bin/java.exe" @javaArguments
    if ($LASTEXITCODE -ne 0) { throw 'Falló la ejecución de la interfaz.' }
} finally { Pop-Location }
