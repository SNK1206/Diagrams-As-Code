$JDK   = "C:\Users\javi_\liberica-jdk21-full\jdk-21.0.7-full"
$SRC   = "$PSScriptRoot\src"
$OUT   = "$PSScriptRoot\out"

New-Item -ItemType Directory -Force -Path $OUT | Out-Null

Write-Host "Compilando..." -ForegroundColor Cyan
$archivos = Get-ChildItem "$SRC" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
$ProgressPreference = 'SilentlyContinue'
& "$JDK\bin\javac.exe" -d $OUT -sourcepath $SRC @archivos 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR de compilacion. Revisa los mensajes anteriores." -ForegroundColor Red
    Read-Host "Presiona Enter para salir"
    exit 1
}

Write-Host "Compilacion exitosa. Abriendo IDE..." -ForegroundColor Green
& "$JDK\bin\java.exe" -cp $OUT com.diagramas.MainFX
