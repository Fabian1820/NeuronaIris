package com.example.edfinal;

import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.BMUandFManager;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    public SOM map;

    public boolean startPressed;
    public Label LabelResult;
    public ScatterChart<Number, Number> SepalChart;
    public ScatterChart<Number, Number> PetalChart;
    public TextField SepalWidthText;
    public TextField SepalLengthText;
    public TextField PetalWidthText;
    public TextField PetalLengthText;
    public ScatterChart WidthChart;
    public ScatterChart LengthChart;
    public TextField NeuronsTF;
    public TextField EpochsTF;
    public TextField LearningRateTF;
    public TextField RadiusTF;
    public Button CloseButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startPressed=false;
        SepalChart.setLegendVisible(false);
        PetalChart.setLegendVisible(false);
        WidthChart.setLegendVisible(false);
        LengthChart.setLegendVisible(false);
        mostrarBase();
    }


    public void loadFile(ActionEvent actionEvent) throws FileNotFoundException {
        if(startPressed) {
            if (map.isTrained()) {
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
                    for (Flower f : list) {
                        System.out.print(k + "-" + f.getType() + "      ");

                        SOMNeuron bmu = map.findBMU(f);
                        String resp = map.classify(bmu);

                        System.out.print("   " + "//" + bmu.getId() + "//");
                        System.out.println("   " + k++ + "-" + resp);

                        BMUandFManager.addToList(bmu, f, resp);
                    }

                    GestorTxt.writeItemB();
                }
            } else {
                showAlert("The map has not been trained. To do so press Train.", Alert.AlertType.WARNING);
            }
        }else {
            showAlert("No map has been created to classify a file. Press Start and then Train.", Alert.AlertType.WARNING);
        }
    }

    public void loadMap(ActionEvent actionEvent) throws IOException {
        PetalChart.getData().clear();
        SepalChart.getData().clear();
        WidthChart.getData().clear();
        LengthChart.getData().clear();
        this.map = GestorTxt.loadMap();
        //GestorTxt.writeHeaderConfig(map);
        BMUStock.getSetosa().clear();
        BMUStock.getVersicolor().clear();
        BMUStock.getVirginica().clear();
        map.groupBmus(GestorTxt.getDataBase());
        mostrarNeuronasGrupo(BMUStock.getSetosa(), Color.RED);
        mostrarNeuronasGrupo(BMUStock.getVersicolor(), Color.GREEN);
        mostrarNeuronasGrupo(BMUStock.getVirginica(), Color.BLUE);
        this.startPressed=true;
    }

    public void start(ActionEvent actionEvent) {
        PetalChart.getData().clear();
        SepalChart.getData().clear();
        WidthChart.getData().clear();
        LengthChart.getData().clear();
        try {
            BMUStock.getSetosa().clear();
            BMUStock.getVersicolor().clear();
            BMUStock.getVirginica().clear();
            int epochs = Integer.parseInt(EpochsTF.getText());
            int neurons = Integer.parseInt(NeuronsTF.getText());
            double learningRate = Double.parseDouble(LearningRateTF.getText());
            int radius = Integer.parseInt(RadiusTF.getText());
            map = new SOM(epochs, neurons, learningRate, radius);
          //  GestorTxt.writeHeaderConfig(map);
            map.initialize();
            mostrar();
            this.startPressed = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            showAlert("Fill all the map configuration parameters.", Alert.AlertType.WARNING);
        }
    }

    public void train(ActionEvent actionEvent) {
        if(startPressed) {
            if (!map.isTrained()) {
                if (map.isInit()) {
                    SepalChart.getData().clear();
                    PetalChart.getData().clear();
                    WidthChart.getData().clear();
                    LengthChart.getData().clear();
                    map.train();
                    mostrarNeuronasGrupo(BMUStock.getSetosa(), Color.RED);
                    mostrarNeuronasGrupo(BMUStock.getVersicolor(), Color.GREEN);
                    mostrarNeuronasGrupo(BMUStock.getVirginica(), Color.BLUE);
                } else {
                    showAlert("The map has not been created. To do so press Start", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("Do not train the map again you fool!!!. You'll break this sh*t. Restart it!!!", Alert.AlertType.WARNING);
            }
        }else
        {
            showAlert("The map has not been created. To do so press Start", Alert.AlertType.WARNING);
        }
    }

    public void mostrarBase()
    {
        ArrayList<Flower> flores = GestorTxt.getDataBase();
        double[] sepalWidth = new double[flores.size()];
        double[] sepalLength = new double[flores.size()];
        double[] petalWidth = new double[flores.size()];
        double[] petalLength = new double[flores.size()];
        for(int i=0;i<flores.size();i++)
        {
            sepalWidth[i]= flores.get(i).getSepalWidth();
            sepalLength[i]= flores.get(i).getSepalLength();
            petalWidth[i]= flores.get(i).getPetalWidth();
            petalLength[i]= flores.get(i).getPetalLength();
        }
        updateChartDataGroup(SepalChart, sepalWidth, sepalLength,Color.ORANGE);
        updateChartDataGroup(PetalChart, petalWidth, petalLength,Color.CYAN);
        updateChartDataGroup(WidthChart, petalWidth, sepalWidth,Color.ORANGE);
        updateChartDataGroup(LengthChart, petalLength, sepalLength,Color.CYAN);
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
        updateChartDataGroup(WidthChart, petalWidth, sepalWidth,color);
        updateChartDataGroup(LengthChart, petalLength, sepalLength,color);
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
        updateChartDataGroup(WidthChart, petalWidth, sepalWidth,Color.ORANGE);
        updateChartDataGroup(LengthChart, petalLength, sepalLength,Color.CYAN);
    }

    public void classify(ActionEvent actionEvent) {
        if(startPressed) {
            if (map.isTrained()) {
                try {
                    if (SepalChart.getData().size() > 3 && PetalChart.getData().size() > 3 && WidthChart.getData().size() > 3 && LengthChart.getData().size() > 3) {
                        SepalChart.getData().remove(SepalChart.getData().size() - 1);
                        PetalChart.getData().remove(PetalChart.getData().size() - 1);
                        WidthChart.getData().remove(WidthChart.getData().size() - 1);
                        LengthChart.getData().remove(LengthChart.getData().size() - 1);
                    }
                    double sepalLength = Double.parseDouble(SepalLengthText.getText());
                    double sepalWidth = Double.parseDouble(SepalWidthText.getText());
                    double petalLength = Double.parseDouble(PetalLengthText.getText());
                    double petalWidth = Double.parseDouble(PetalWidthText.getText());

                    Flower f = new Flower(sepalLength, sepalWidth, petalLength, petalWidth, 'E');
                    SOMNeuron bmu = map.findBMU(f);
                   // GestorTxt.writeInConfig(bmu);

                    sepalLength=((Flower) bmu.getInfo()).getSepalLength();
                    sepalWidth=((Flower) bmu.getInfo()).getSepalWidth();
                    petalLength=((Flower) bmu.getInfo()).getPetalLength();
                    petalWidth=((Flower) bmu.getInfo()).getPetalWidth();

                    updateChartData(SepalChart, sepalWidth, sepalLength, Color.YELLOW);
                    updateChartData(PetalChart, petalWidth, petalLength, Color.YELLOW);
                    updateChartData(WidthChart, petalWidth, sepalWidth, Color.YELLOW);
                    updateChartData(LengthChart, petalLength, sepalLength, Color.YELLOW);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Check the data entry. Data must be entered this way:\nPetal Length: 1.0cm-6.9cm\nPetal Width: 0.1cm-2.5cm\nSepal Length: 4.3cm-7.9cm\nSepal Width: 2.0cm-4.4cm", Alert.AlertType.ERROR);
                }
            } else {
                showAlert("The map has not been trained. To do so press Train.", Alert.AlertType.WARNING);
            }
        }else
        {
            showAlert("No map has been created to classify a flower. Press Start and then Train", Alert.AlertType.WARNING);
        }
    }

    private void updateChartData(ScatterChart<Number, Number> chart, double x, double y, Color color) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        XYChart.Data<Number, Number> data = new XYChart.Data<>(x, y);

        Circle circle = new Circle(7);
        circle.setFill(color);
        circle.setOpacity(40);

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

    private void showAlert(String message, Alert.AlertType at) {
        Alert alert = new Alert(at);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void save(ActionEvent actionEvent) throws IOException {
        if (startPressed) {
            if (map.isTrained()) {
                GestorTxt.writeMap(map);
            }
            else
            {
                showAlert("Train the map first", Alert.AlertType.WARNING);
            }
        }
        else
        {
            showAlert("No map has been created to save. Press Start and then Train.", Alert.AlertType.WARNING);
        }
    }

    public void closeScreen(ActionEvent actionEvent) throws IOException {
        ((Stage)CloseButton.getScene().getWindow()).close();
    }
}