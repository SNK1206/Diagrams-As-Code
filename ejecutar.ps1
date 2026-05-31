$SDK = "$PSScriptRoot\javafx-sdk-21.0.9\lib"

# === COMPILAR ===
New-Item -ItemType Directory -Force out | Out-Null
javac --module-path $SDK --add-modules javafx.controls,javafx.fxml -cp src -d out (Get-ChildItem -Recurse -Filter "*.java" src | Select-Object -ExpandProperty FullName)

# === EJECUTAR ===
java --module-path $SDK --add-modules javafx.controls,javafx.fxml -cp out com.diagramas.MainFX
