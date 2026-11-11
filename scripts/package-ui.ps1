param([string]$JdkHome = $env:JAVA_HOME)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $JdkHome) {
    $compiler = Get-Command javac -ErrorAction SilentlyContinue
    if ($compiler) {
        $JdkHome = Split-Path (Split-Path $compiler.Source -Parent) -Parent
    } else {
        $JdkHome = 'C:/Program Files/Android/Android Studio/jbr'
    }
}
if (-not (Test-Path -LiteralPath "$JdkHome/bin/javac.exe")) {
    throw 'Indica la ubicación de un JDK 21 para Windows con -JdkHome.'
}
$csc = "$env:WINDIR/Microsoft.NET/Framework64/v4.0.30319/csc.exe"
if (-not (Test-Path -LiteralPath $csc)) {
    throw 'El compilador de .NET Framework es necesario para generar Ritmo.exe.'
}

$packageRoot = Join-Path $projectRoot ('target/portable-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
$classes = Join-Path $packageRoot 'classes'
& "$PSScriptRoot/preview-ui.ps1" -JdkHome $JdkHome -CompileOnly -ClassesDirectory $classes
$distribution = Join-Path $packageRoot 'Ritmo'
$app = Join-Path $distribution 'app'
$runtime = Join-Path $distribution 'runtime'
$modules = Join-Path $app 'javafx'
New-Item -ItemType Directory -Path $modules,$runtime -Force | Out-Null

& "$JdkHome/bin/jar.exe" --create --file "$app/ritmo.jar" -C $classes academia
if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el JAR de la interfaz.' }
foreach ($module in @('base','controls','graphics')) {
    $file = "$env:USERPROFILE/.m2/repository/org/openjfx/javafx-$module/21.0.8/javafx-$module-21.0.8-win.jar"
    Copy-Item -LiteralPath $file -Destination $modules
}
foreach ($item in @('bin','conf','lib','legal','release')) {
    Copy-Item -LiteralPath (Join-Path $JdkHome $item) -Destination $runtime -Recurse
}

$launcher = Join-Path $distribution 'Ritmo.exe'
& $csc /nologo /target:winexe /platform:x64 /reference:System.Windows.Forms.dll "/out:$launcher" (Join-Path $PSScriptRoot "RitmoLauncher.cs")
if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar Ritmo.exe.' }

@"
Ritmo - Academia de baile

Extrae el ZIP completo. Abre Ritmo.exe con doble clic.
Puedes crear un acceso directo al ejecutable en el Escritorio.
Conserva las carpetas app y runtime junto a Ritmo.exe.
Incluye Java y JavaFX para Windows x64.

La vista previa comienza vacia y conserva los registros en memoria durante la sesion.
En Reportes puedes guardar todos los registros de la sesion en Excel.
"@ | Set-Content -LiteralPath (Join-Path $distribution 'LEEME.txt') -Encoding utf8

$archive = Join-Path $packageRoot 'Ritmo-Windows-x64.zip'
tar.exe -a -cf $archive -C $packageRoot Ritmo
if ($LASTEXITCODE -ne 0) { throw 'No se pudo comprimir el paquete portátil.' }
Write-Output "Ejecutable: $launcher"
Write-Output "Paquete: $archive"
