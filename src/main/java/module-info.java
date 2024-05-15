module com.example.edfinal {
    requires javafx.controls;
    requires javafx.fxml;



    opens com.example.edfinal to javafx.fxml;
    exports com.example.edfinal;
}