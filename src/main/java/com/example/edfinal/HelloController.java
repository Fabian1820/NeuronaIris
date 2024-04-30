package com.example.edfinal;

import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.BMUandFManager;
import com.example.edfinal.utiles.BMUandFlowers;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    public SOM map;
    public Label LabelResult;
    public ScatterChart<Number, Number> SepalChart;
    public ScatterChart<Number, Number> PetalChart;
    public TextField SepalWidthText;
    public TextField SepalLengthText;
    public TextField PetalWidthText;
    public TextField PetalLengthText;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.map = new SOM();
        SepalChart.setLegendVisible(false);
        PetalChart.setLegendVisible(false);
        mostrarBase();
    }


    public void loadFile(ActionEvent actionEvent) throws FileNotFoundException {
        if(map.isTrained()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar Archivo");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("DAT", "*.data")
            );
            File selectedFile = fileChooser.showOpenDialog(((Node) actionEvent.getSource()).getScene().getWindow());
            if (selectedFile != null) {
                String path = selectedFile.getAbsolutePath();
                ArrayList<Flower> list = GestorTxt.getFile(path);
                BMUandFManager.cleanList();
                int k = 1;
                for(Flower f: list)
                {
                    System.out.print(k + "-" + f.getType() + "      ");

                    SOMNeuron bmu = map.findBMU(f);
                    String resp = map.classify(bmu);

                    System.out.print("   " + "//" + bmu.getId() + "//");
                    System.out.println("   " + k++ + "-" +resp);

                    BMUandFManager.addToList(bmu, f, resp);
                }

                GestorTxt.writeItemB();
            }
        }
        else
        {
            showAlert("The map has not been trained. To do so press Train.");
        }
    }

    public void loadMap(ActionEvent actionEvent) throws IOException {
        PetalChart.getData().clear();
        SepalChart.getData().clear();
        this.map = GestorTxt.loadMap();
        BMUStock.getSetosa().clear();
        BMUStock.getVersicolor().clear();
        BMUStock.getVirginica().clear();
        map.groupBmus(GestorTxt.getDataBase());
        mostrarNeuronasGrupo(BMUStock.getSetosa(), Color.RED);
        mostrarNeuronasGrupo(BMUStock.getVersicolor(), Color.GREEN);
        mostrarNeuronasGrupo(BMUStock.getVirginica(), Color.BLUE);
    }

    public void train(ActionEvent actionEvent) {
        if(!map.isTrained()) {
            if (map.isInit()) {
                SepalChart.getData().clear();
                PetalChart.getData().clear();
                map.train();
                mostrarNeuronasGrupo(BMUStock.getSetosa(), Color.RED);
                mostrarNeuronasGrupo(BMUStock.getVersicolor(), Color.GREEN);
                mostrarNeuronasGrupo(BMUStock.getVirginica(), Color.BLUE);
            } else {
                showAlert("The map has not been created. To do so press Start");
            }
        }
        else
        {
            showAlert("Do not train the map again you fool!!!. Restart it!!!");
        }
    }

    public void mostrarBase()
    {
        ArrayList<Flower> flores = GestorTxt.getDataBase();
        double[] sepalWidth = new double[map.totalNeurons];
        double[] sepalLength = new double[map.totalNeurons];
        double[] petalWidth = new double[map.totalNeurons];
        double[] petalLength = new double[map.totalNeurons];
        for(int i=0;i<map.totalNeurons;i++)
        {
            sepalWidth[i]= flores.get(i).getSepalWidth();
            sepalLength[i]= flores.get(i).getSepalLength();
            petalWidth[i]= flores.get(i).getPetalWidth();
            petalLength[i]= flores.get(i).getPetalLength();
        }
        updateChartDataGroup(SepalChart, sepalWidth, sepalLength,Color.ORANGE);
        updateChartDataGroup(PetalChart, petalWidth, petalLength,Color.CYAN);
    }

    public void mostrarNeuronasGrupo(ArrayList<SOMNeuron> flores, Color color)
    {

        double[] sepalWidth = new double[flores.size()];
        double[] sepalLength = new double[flores.size()];
        double[] petalWidth = new double[flores.size()];
        double[] petalLength = new double[flores.size()];
        for(int i=0;i<flores.size();i++)
        {
            sepalWidth[i]= ((Flower)flores.get(i).getInfo()).getSepalWidth();
            sepalLength[i]= ((Flower)flores.get(i).getInfo()).getSepalLength();
            petalWidth[i]= ((Flower)flores.get(i).getInfo()).getPetalWidth();
            petalLength[i]= ((Flower)flores.get(i).getInfo()).getPetalLength();
        }
        updateChartDataGroup(SepalChart, sepalWidth, sepalLength,color);
        updateChartDataGroup(PetalChart, petalWidth, petalLength,color);
    }

    public void mostrar()
    {
        Iterator<Vertex> iter = map.getVerticesList().iterator();
        double[] sepalWidth = new double[map.totalNeurons];
        double[] sepalLength = new double[map.totalNeurons];
        double[] petalWidth = new double[map.totalNeurons];
        double[] petalLength = new double[map.totalNeurons];
        for(int i=0;i<map.totalNeurons && iter.hasNext();i++)
        {
            SOMNeuron n = (SOMNeuron) iter.next();
            sepalWidth[i]= ((Flower)n.getInfo()).getSepalWidth();
            sepalLength[i]= ((Flower)n.getInfo()).getSepalLength();
            petalWidth[i]= ((Flower)n.getInfo()).getPetalWidth();
            petalLength[i]= ((Flower)n.getInfo()).getPetalLength();
        }
        updateChartDataGroup(SepalChart, sepalWidth, sepalLength,Color.ORANGE);
        updateChartDataGroup(PetalChart, petalWidth, petalLength,Color.CYAN);
    }

    public void start(ActionEvent actionEvent) {
        PetalChart.getData().clear();
        SepalChart.getData().clear();
        if(!map.isTrained()) {
            map.initialize();
            mostrar();
        }
        else
        {
            map = new SOM();
            map.initialize();
            mostrar();
        }
    }

    public void classify(ActionEvent actionEvent) {
        if(map.isTrained())
        {
            try {
                if(SepalChart.getData().size()>3 && PetalChart.getData().size()>3 ) {
                    SepalChart.getData().remove(SepalChart.getData().size() - 1);
                    PetalChart.getData().remove(PetalChart.getData().size() - 1);
                }
                double sepalLength = Double.parseDouble(SepalLengthText.getText());
                double sepalWidth = Double.parseDouble(SepalWidthText.getText());
                double petalLength = Double.parseDouble(PetalLengthText.getText());
                double petalWidth = Double.parseDouble(PetalWidthText.getText());

                Flower f = new Flower(sepalLength, sepalWidth, petalLength, petalWidth);
                SOMNeuron bmu = map.findBMU(f);
                String resp = map.classify(bmu);
                LabelResult.setText(resp);

                System.out.println(((Flower)bmu.getInfo()).getSepalWidth());
                System.out.println(((Flower)bmu.getInfo()).getSepalLength());
                System.out.println(((Flower)bmu.getInfo()).getPetalWidth());
                System.out.println(((Flower)bmu.getInfo()).getPetalLength());


                updateChartData(SepalChart, ((Flower)bmu.getInfo()).getSepalWidth(), ((Flower)bmu.getInfo()).getSepalLength(),Color.YELLOW);
                updateChartData(PetalChart, ((Flower)bmu.getInfo()).getPetalWidth(), ((Flower)bmu.getInfo()).getPetalLength(),Color.YELLOW);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Check the data entry. Data must be entered this way:\nPetal Length: 1.0cm-6.9cm\nPetal Width: 0.1cm-2.5cm\nSepal Length: 4.3cm-7.9cm\nSepal Width: 2.0cm-4.4cm");
            }
        }
        else
        {
            showAlert("The map has not been trained. To do so press Train.");
        }
    }

    private void updateChartData(ScatterChart<Number, Number> chart, double x, double y, Color color) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        XYChart.Data<Number, Number> data = new XYChart.Data<>(x, y);

        Circle circle = new Circle(7);
        circle.setFill(color);

        data.setNode(circle);

        series.getData().add(data);
        chart.getData().add(series);

    }

    private void updateChartDataGroup(ScatterChart<Number, Number> chart, double[] x, double[] y, Color color) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        int tam = x.length;
        for(int i=0;i<tam;i++)
        {
            XYChart.Data<Number, Number> data = new XYChart.Data<>(x[i], y[i]);
            Circle circle = new Circle(5);
            circle.setFill(color);
            data.setNode(circle);
            series.getData().add(data);
        }
        chart.getData().add(series);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void save(ActionEvent actionEvent) throws IOException {
        GestorTxt.writeNeurons(map);
    }
}