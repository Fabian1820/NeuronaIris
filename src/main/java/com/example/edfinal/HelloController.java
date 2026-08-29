package com.example.edfinal;

import com.example.edfinal.data.Dataset;
import com.example.edfinal.data.Sample;
import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.BMUandFManager;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class HelloController implements Initializable {

    private static final String TOPOLOGIA_ANILLO = "1-D ring";
    private static final String TOPOLOGIA_REJILLA = "2-D grid";

    /** Colores con los que se pinta cada etiqueta del dataset, en orden de aparición. */
    private static final Color[] PALETA = {
            Color.RED, Color.GREEN, Color.DODGERBLUE, Color.GOLD, Color.MEDIUMORCHID, Color.CORAL
    };

    public SOM map;
    public boolean startPressed;

    public ScatterChart<Number, Number> SepalChart;
    public ScatterChart<Number, Number> PetalChart;
    public ScatterChart<Number, Number> WidthChart;
    public ScatterChart<Number, Number> LengthChart;

    public TextField NeuronsTF;
    public TextField EpochsTF;
    public TextField LearningRateTF;
    public TextField RadiusTF;
    public ComboBox<String> TopologyCB;
    public Button CloseButton;
    public TextArea TextA;
    public ImageView ImgView;

    @FXML private AnchorPane ImgAnchor;
    @FXML private GridPane entradaGrid;
    @FXML private FlowPane leyendaPane;
    @FXML private Label datasetLabel;

    /** El dataset con el que trabaja la pantalla. Arranca con el Iris que va en el jar. */
    private Dataset dataset;

    /** Un campo de texto por variable del dataset. */
    private final List<TextField> camposEntrada = new ArrayList<>();

    /** Qué par de variables dibuja cada gráfica: {x, y}. */
    private int[][] paresDeEjes;

    private final Map<String, Color> coloresPorEtiqueta = new LinkedHashMap<>();

    private int currentImageIndex = 0;
    // Rutas dentro del classpath: funcionan en cualquier máquina y dentro del jar.
    private final String[] imagePaths = {
            "/Imagen/FLORP.jpg",
            "/Imagen/MORP.jpg",
            "/Imagen/OIPP.jpg",
            "/Imagen/RP.jpg",
            "/Imagen/SPIP.jpg"
    };

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startPressed = false;

        TopologyCB.getItems().setAll(TOPOLOGIA_ANILLO, TOPOLOGIA_REJILLA);
        TopologyCB.getSelectionModel().select(TOPOLOGIA_REJILLA);

        for (ScatterChart<Number, Number> c : graficas()) c.setLegendVisible(false);

        Timeline imageChangeTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> changeImage()));
        imageChangeTimeline.setCycleCount(Timeline.INDEFINITE);
        imageChangeTimeline.play();

        usarDataset(GestorTxt.getIrisDataset(), "Iris");
    }

    private List<ScatterChart<Number, Number>> graficas() {
        return List.of(WidthChart, PetalChart, LengthChart, SepalChart);
    }

    // ---------- dataset ----------

    /**
     * Cambia el dataset de la pantalla: rehace los campos de entrada, los ejes de
     * las gráficas y la leyenda a partir de sus variables y etiquetas.
     *
     * Antes todo esto estaba cableado a las cuatro medidas del Iris, así que el
     * núcleo aceptaba cualquier CSV pero la interfaz no.
     */
    private void usarDataset(Dataset d, String nombre) {
        this.dataset = d;
        this.map = null;
        this.startPressed = false;
        TextA.setText("");

        construirCamposDeEntrada();
        elegirParesDeEjes();
        asignarColores();
        construirLeyenda();

        datasetLabel.setText("Data Entry  ·  " + nombre + "  ("
                + d.size() + " samples, " + d.dimension() + " variables)");

        limpiarGraficas();
        mostrarBase();
    }

    private void construirCamposDeEntrada() {
        entradaGrid.getChildren().clear();
        camposEntrada.clear();

        String[] nombres = dataset.getFeatureNames();
        double[] min = dataset.getMin(), max = dataset.getMax();

        for (int i = 0; i < nombres.length; i++) {
            Label etiqueta = new Label(nombres[i]);
            TextField campo = new TextField();
            campo.setAlignment(Pos.CENTER);
            campo.setPrefHeight(34);
            campo.setPromptText(String.format("%.1f – %.1f", min[i], max[i]));
            camposEntrada.add(campo);

            // Dos variables por fila
            int fila = (i / 2) * 2, columna = i % 2;
            entradaGrid.add(etiqueta, columna, fila);
            entradaGrid.add(campo, columna, fila + 1);
        }
    }

    /**
     * Cuatro pares de variables para las cuatro gráficas.
     *
     * Con menos de cuatro combinaciones posibles se repite la última, para que
     * siempre haya un par por gráfica sea cual sea la dimensión del dataset.
     */
    static int[][] paresDeVariables(int dim) {
        List<int[]> pares = new ArrayList<>();
        for (int i = 0; i < dim && pares.size() < 4; i++) {
            for (int j = i + 1; j < dim && pares.size() < 4; j++) pares.add(new int[]{i, j});
        }
        // Con una sola variable no hay pares: se dibuja contra sí misma.
        while (pares.size() < 4) pares.add(new int[]{0, Math.min(1, dim - 1)});
        return pares.toArray(new int[0][]);
    }

    private void elegirParesDeEjes() {
        paresDeEjes = paresDeVariables(dataset.dimension());

        String[] nombres = dataset.getFeatureNames();
        List<ScatterChart<Number, Number>> cs = graficas();
        for (int k = 0; k < cs.size(); k++) {
            ((NumberAxis) cs.get(k).getXAxis()).setLabel(nombres[paresDeEjes[k][0]]);
            ((NumberAxis) cs.get(k).getYAxis()).setLabel(nombres[paresDeEjes[k][1]]);
        }
    }

    private void asignarColores() {
        coloresPorEtiqueta.clear();
        int i = 0;
        for (String etiqueta : dataset.labels()) {
            coloresPorEtiqueta.put(especie(etiqueta), PALETA[i++ % PALETA.length]);
        }
    }

    private void construirLeyenda() {
        leyendaPane.getChildren().clear();
        leyendaPane.getChildren().add(entradaLeyenda(Color.ORANGE, "Data / random weights"));
        for (Map.Entry<String, Color> e : coloresPorEtiqueta.entrySet()) {
            leyendaPane.getChildren().add(entradaLeyenda(e.getValue(), e.getKey()));
        }
        leyendaPane.getChildren().add(entradaLeyenda(Color.YELLOW, "BMU of data entry"));
    }

    private HBox entradaLeyenda(Color color, String texto) {
        Circle punto = new Circle(8, color);
        punto.setStroke(Color.BLACK);
        HBox caja = new HBox(6, punto, new Label(texto));
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }

    /** "Iris-setosa" -> "setosa" */
    private String especie(String tipo) {
        if (tipo == null) return "";
        String t = tipo.trim().toLowerCase();
        int guion = t.lastIndexOf('-');
        return guion >= 0 ? t.substring(guion + 1) : t;
    }

    /** Carga un CSV cualquiera como dataset de trabajo. */
    public void loadDataset(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select dataset");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Data files", "*.csv", "*.data", "*.txt"));

        File elegido = fileChooser.showOpenDialog(((Node) actionEvent.getSource()).getScene().getWindow());
        if (elegido == null) return;

        try {
            Dataset d = Dataset.fromCsv(elegido.getAbsolutePath());
            BMUStock.clear();
            usarDataset(d, elegido.getName());
        } catch (Exception e) {
            showAlert("Could not read the dataset: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void changeImage() {
        var stream = getClass().getResourceAsStream(imagePaths[currentImageIndex]);
        if (stream != null) {
            ImgView.setImage(new Image(stream));
        }
        currentImageIndex = (currentImageIndex + 1) % imagePaths.length;
    }

    // ---------- clasificación ----------

    public void loadFile(ActionEvent actionEvent) {
        if (!startPressed || map == null) {
            showAlert("No map has been created to classify a file. Press Start and then Train.", Alert.AlertType.WARNING);
            return;
        }
        if (!map.isTrained()) {
            showAlert("The map has not been trained. To do so press Train.", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Data files", "*.csv", "*.data", "*.txt"));

        File elegido = fileChooser.showOpenDialog(((Node) actionEvent.getSource()).getScene().getWindow());
        if (elegido == null) return;

        try {
            Dataset entrada = Dataset.fromCsv(elegido.getAbsolutePath());
            if (entrada.dimension() != dataset.dimension()) {
                showAlert("That file has " + entrada.dimension() + " variables and the map works with "
                        + dataset.dimension() + ".", Alert.AlertType.ERROR);
                return;
            }

            BMUandFManager.cleanList();
            TextA.appendText("— " + elegido.getName() + " —\n");
            for (Sample s : entrada.getSamples()) {
                SOMNeuron bmu = map.findBMU(s);
                String resp = map.classify(bmu);
                TextA.appendText("Id " + bmu.getId() + " → " + resp + "\n");
            }
            GestorTxt.writeItemB();
        } catch (Exception e) {
            showAlert("Could not classify the file: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void classify(ActionEvent actionEvent) {
        if (!startPressed || map == null) {
            showAlert("No map has been created to classify. Press Start and then Train", Alert.AlertType.WARNING);
            return;
        }
        if (!map.isTrained()) {
            showAlert("The map has not been trained. To do so press Train.", Alert.AlertType.WARNING);
            return;
        }

        double[] valores = new double[camposEntrada.size()];
        try {
            for (int i = 0; i < camposEntrada.size(); i++) {
                valores[i] = Double.parseDouble(camposEntrada.get(i).getText().trim());
            }
        } catch (Exception e) {
            showAlert("Fill every variable with a number.", Alert.AlertType.ERROR);
            return;
        }

        Sample entrada = new Sample(valores);
        SOMNeuron bmu = map.findBMU(entrada);
        Sample pesos = bmu.getWeights();

        DecimalFormat df = new DecimalFormat("#.00");
        StringBuilder sb = new StringBuilder("Id " + bmu.getId());
        String especie = map.classify(bmu);
        if (especie != null && !especie.isEmpty()) sb.append(" · ").append(especie);
        for (int i = 0; i < pesos.size(); i++) {
            sb.append("  ").append(dataset.getFeatureNames()[i]).append("=").append(df.format(pesos.get(i)));
        }
        TextA.appendText(sb + "\n");

        try {
            GestorTxt.writeInConfig(bmu);
        } catch (IOException ignored) {
            // El registro en Configuration.dat es accesorio: no debe romper la clasificación.
        }

        List<ScatterChart<Number, Number>> cs = graficas();
        for (int k = 0; k < cs.size(); k++) {
            if (cs.get(k).getData().size() > coloresPorEtiqueta.size()) {
                cs.get(k).getData().remove(cs.get(k).getData().size() - 1);
            }
            updateChartData(cs.get(k), entrada.get(paresDeEjes[k][0]), entrada.get(paresDeEjes[k][1]), Color.YELLOW);
        }
    }

    // ---------- mapa ----------

    public void loadMap(ActionEvent actionEvent) throws IOException {
        // El fichero lo genera "Save Map": en una copia recién instalada no existe.
        if (!GestorTxt.haySavedMap()) {
            showAlert("There is no saved map yet. Train a map and press Save first.", Alert.AlertType.WARNING);
            return;
        }
        limpiarGraficas();
        this.map = GestorTxt.loadMap();
        this.dataset = map.getDataset();
        GestorTxt.writeHeaderConfig(map);
        map.groupBmus(dataset.getSamples());
        pintarGrupos();
        TextA.setText("");
        this.startPressed = true;
    }

    public void start(ActionEvent actionEvent) {
        limpiarGraficas();
        TextA.setText("");
        try {
            BMUStock.clear();
            int epochs = Integer.parseInt(EpochsTF.getText());
            int neurons = Integer.parseInt(NeuronsTF.getText());
            double learningRate = Double.parseDouble(LearningRateTF.getText());
            int radius = Integer.parseInt(RadiusTF.getText());

            if (esRejillaSeleccionada()) {
                if (neurons < 4) {
                    showAlert("A 2-D map needs at least 4 neurons.", Alert.AlertType.WARNING);
                    return;
                }
                int[] rejilla = repartirEnRejilla(neurons);
                map = new SOM(epochs, rejilla[0], rejilla[1], learningRate, radius, dataset);
            } else {
                map = new SOM(epochs, neurons, learningRate, radius, dataset);
            }
            GestorTxt.writeHeaderConfig(map);
            map.initialize();
            mostrar();
            this.startPressed = true;
        } catch (Exception e) {
            showAlert("Fill all the map configuration parameters.", Alert.AlertType.WARNING);
        }
    }

    public void train(ActionEvent actionEvent) {
        if (!startPressed || map == null || !map.isInit()) {
            showAlert("The map has not been created. To do so press Start", Alert.AlertType.WARNING);
            return;
        }
        limpiarGraficas();
        // Reentrenar es válido: sigue ajustando el mapa desde los pesos
        // actuales y rehace el agrupamiento desde cero.
        map.train();
        pintarGrupos();
    }

    /**
     * Abre la vista del mapa actual: U-matrix, planos de componentes y etiquetas.
     */
    public void verMapa2D(ActionEvent actionEvent) {
        if (!startPressed || map == null) {
            showAlert("The map has not been created. To do so press Start", Alert.AlertType.WARNING);
            return;
        }
        if (map.getTopology() != SOM.Topology.GRID) {
            showAlert("These readings need a 2-D map. Choose \"" + TOPOLOGIA_REJILLA
                    + "\" and press Start again.", Alert.AlertType.WARNING);
            return;
        }
        if (!map.isTrained()) {
            showAlert("The map has not been trained. To do so press Train.", Alert.AlertType.WARNING);
            return;
        }

        try {
            new com.example.edfinal.ui.MapaView(map).mostrar();
        } catch (Exception e) {
            showAlert("Could not open the map: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean esRejillaSeleccionada() {
        return TOPOLOGIA_REJILLA.equals(TopologyCB.getSelectionModel().getSelectedItem());
    }

    /** Reparte n neuronas en la rejilla más cuadrada posible. */
    static int[] repartirEnRejilla(int n) {
        int filas = (int) Math.floor(Math.sqrt(n));
        while (filas > 1 && n % filas != 0) filas--;
        int columnas = n / filas;
        if (filas < 2) {              // número primo: se redondea hacia arriba
            filas = 2;
            columnas = (int) Math.ceil(n / 2.0);
        }
        return new int[]{filas, columnas};
    }

    // ---------- gráficas ----------

    private void limpiarGraficas() {
        for (ScatterChart<Number, Number> c : graficas()) c.getData().clear();
    }

    /** Dibuja las muestras del dataset. */
    public void mostrarBase() {
        pintar(dataset.getSamples(), Color.ORANGE);
    }

    /** Dibuja los pesos actuales de las neuronas. */
    public void mostrar() {
        List<Sample> pesos = new ArrayList<>();
        for (Vertex v : map.getVerticesList()) pesos.add(((SOMNeuron) v).getWeights());
        pintar(pesos, Color.ORANGE);
    }

    /** Dibuja las BMUs de cada etiqueta con su color. */
    private void pintarGrupos() {
        for (Map.Entry<String, Color> e : coloresPorEtiqueta.entrySet()) {
            List<Sample> pesos = new ArrayList<>();
            for (SOMNeuron n : BMUStock.forLabel(e.getKey())) pesos.add(n.getWeights());
            pintar(pesos, e.getValue());
        }
    }

    private void pintar(List<Sample> muestras, Color color) {
        if (muestras.isEmpty()) return;
        List<ScatterChart<Number, Number>> cs = graficas();
        for (int k = 0; k < cs.size(); k++) {
            double[] x = new double[muestras.size()];
            double[] y = new double[muestras.size()];
            for (int i = 0; i < muestras.size(); i++) {
                x[i] = muestras.get(i).get(paresDeEjes[k][0]);
                y[i] = muestras.get(i).get(paresDeEjes[k][1]);
            }
            updateChartDataGroup(cs.get(k), x, y, color);
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
        for (int i = 0; i < x.length; i++) {
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
        alert.initOwner(ImgAnchor.getScene().getWindow());
        alert.showAndWait();
    }

    public void save(ActionEvent actionEvent) throws IOException {
        if (!startPressed || map == null) {
            showAlert("No map has been created to save. Press Start and then Train.", Alert.AlertType.WARNING);
            return;
        }
        if (!map.isTrained()) {
            showAlert("Train the map first", Alert.AlertType.WARNING);
            return;
        }
        GestorTxt.writeMap(map);
    }

    public void closeScreen(ActionEvent actionEvent) {
        ((Stage) CloseButton.getScene().getWindow()).close();
    }
}
