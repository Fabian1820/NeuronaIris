module panal {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;   // Java2D/ImageIO: exportar el mapa a PNG

    opens panal to javafx.fxml;
    exports panal;
}
