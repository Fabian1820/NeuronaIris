module com.example.edfinal {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;   // Java2D/ImageIO: exportar el mapa a PNG



    opens com.example.edfinal to javafx.fxml;
    exports com.example.edfinal;
}