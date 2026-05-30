# === COMPILAR ===
javac --module-path "C:\Users\bobal\Desktop\openjfx-26.0.1_windows-x64_bin-sdk\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.fxml -cp src -d out (Get-ChildItem -Recurse -Filter "*.java" src | Select-Object -ExpandProperty FullName)

# === EJECUTAR ===
java --module-path "C:\Users\bobal\Desktop\openjfx-26.0.1_windows-x64_bin-sdk\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.fxml -cp out com.diagramas.MainFX
