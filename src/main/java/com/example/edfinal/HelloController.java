package com.example.edfinal;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements Initializable {

    @FXML
    private Canvas grafic;

    @FXML
    public void graficCreate(){
        GraphicsContext gc = grafic.getGraphicsContext2D();
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.0);
        gc.strokeLine(0, grafic.getHeight() / 2, grafic.getWidth(), grafic.getHeight() / 2); // Eje X
        gc.strokeLine(grafic.getWidth() / 2, 0, grafic.getWidth() / 2, grafic.getHeight()); // Eje Y
        gc.setFill(Color.WHITE);
        gc.fillText("0", grafic.getWidth() / 2 -10, grafic.getHeight() / 2 + 15); // Etiqueta del origen (0, 0)
        gc.fillText("X", grafic.getWidth() - 10, grafic.getHeight() / 2 + 15); // Etiqueta del extremo derecho del eje X
        gc.fillText("Y", grafic.getWidth() / 2 - 15, 10); // Etiqueta del extremo superior del eje Y
        puntitos(gc);
    }

    @FXML
    public void puntitos( GraphicsContext gc){
        final int tamanno = 10;
        final double x = (grafic.getWidth()- tamanno) / 2;
        final double y = (grafic.getHeight()- tamanno) / 2;
        gc.fillOval(x+4,y+(-1*30),tamanno,tamanno);
    }

    @FXML
    private BubbleChart<Number,Number> bubbleChart;

    @FXML
    public void fabian (){
        NumberAxis yAxis ;
        NumberAxis xAxis ;
        //Crear el objeto de data series
        XYChart.Series<Number,Number> series2004= new XYChart.Series<>();
        series2004.setName("Candela");

        //Annadir las bolitas a la serie
        series2004.getData().add(new XYChart.Data<>(10,10,0.25));
        /////////////////////////////////////////////////////////////////////
//        series2004.getData().add(new XYChart.Data<>(10, 10, 0.25));
//        XYChart.Data<Number, Number> data = series2004.getData().get(series2004.getData().size() - 1);
//        data.getNode().getStyleClass().add("my-bubble");

        //////////////////////////////////////////////////////////////////////
        series2004.getData().add(new XYChart.Data<>(13,10,0.25));
        series2004.getData().add(new XYChart.Data<>(15,10,0.25));
        series2004.getData().add(new XYChart.Data<>(17,10,0.25));


        //Annadir la serie a la bubbleChart
        bubbleChart.getData().add(series2004);


    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        fabian();
    }
}