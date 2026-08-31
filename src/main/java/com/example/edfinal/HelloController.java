package com.example.edfinal;

import com.example.edfinal.data.BusquedaBayesiana;
import com.example.edfinal.data.BusquedaHiperparametros;
import com.example.edfinal.data.Dataset;
import com.example.edfinal.data.Sample;
import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.BMUandFManager;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
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
    private static final String TOPOLOGIA_HEX = "2-D hex grid";

    /** Colores con los que se pinta cada etiqueta del dataset, en orden de aparición. */
    private static final Color[] PALETA = {
            Color.RED, Color.GREEN, Color.DODGERBLUE, Color.GOLD, Color.MEDIUMORCHID, Color.CORAL
    };

    public SOM map;
    public boolean startPressed;

    public ScatterChart<Number, Number> chart3;
    public ScatterChart<Number, Number> chart1;
    public ScatterChart<Number, Number> chart0;
    public ScatterChart<Number, Number> chart2;

    public TextField NeuronsTF;
    public TextField EpochsTF;
    public TextField LearningRateTF;
    public TextField RadiusTF;
    public ComboBox<String> TopologyCB;

    public ComboBox<String> xVar0, xVar1, xVar2, xVar3;
    public ComboBox<String> yVar0, yVar1, yVar2, yVar3;
    public Button CloseButton;
    public Button startButton, trainButton, mapButton, tuneButton;
    public TextArea TextA;
    public ImageView ImgView;

    @FXML private AnchorPane ImgAnchor;
    @FXML private StackPane panelInferior;
    @FXML private Canvas distribucionCanvas;
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

    /**
     * Lo que hay dibujado ahora mismo, para poder repintarlo si cambian los ejes.
     * Sin esto, elegir otra variable dejaría las gráficas en blanco hasta el
     * siguiente entrenamiento.
     */
    private record Capa(List<Sample> muestras, Color color) {}
    private final List<Capa> capas = new ArrayList<>();

    /** Evita que rellenar los selectores dispare sus propios listeners. */
    private boolean actualizandoSelectores = false;

    /** El carrusel de fotos solo tiene sentido con el Iris. */
    private boolean datasetEsIris = true;

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

        TopologyCB.getItems().setAll(TOPOLOGIA_ANILLO, TOPOLOGIA_REJILLA, TOPOLOGIA_HEX);
        TopologyCB.getSelectionModel().select(TOPOLOGIA_HEX);

        for (ScatterChart<Number, Number> c : graficas()) c.setLegendVisible(false);

        Timeline imageChangeTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> changeImage()));
        imageChangeTimeline.setCycleCount(Timeline.INDEFINITE);
        imageChangeTimeline.play();

        // El Canvas no se redimensiona solo dentro de un StackPane.
        distribucionCanvas.widthProperty().bind(panelInferior.widthProperty());
        distribucionCanvas.heightProperty().bind(panelInferior.heightProperty());
        distribucionCanvas.widthProperty().addListener((o, a, b) -> dibujarDistribucion());
        distribucionCanvas.heightProperty().addListener((o, a, b) -> dibujarDistribucion());

        for (int k = 0; k < 4; k++) {
            final int indice = k;
            selectoresX().get(k).setOnAction(e -> cambiarEje(indice));
            selectoresY().get(k).setOnAction(e -> cambiarEje(indice));
        }

        usarDataset(GestorTxt.getIrisDataset(), "Iris", true);
    }

    private List<ComboBox<String>> selectoresX() {
        return List.of(xVar0, xVar1, xVar2, xVar3);
    }

    private List<ComboBox<String>> selectoresY() {
        return List.of(yVar0, yVar1, yVar2, yVar3);
    }

    private List<ScatterChart<Number, Number>> graficas() {
        return List.of(chart0, chart1, chart2, chart3);
    }

    // ---------- dataset ----------

    /**
     * Cambia el dataset de la pantalla: rehace los campos de entrada, los ejes de
     * las gráficas y la leyenda a partir de sus variables y etiquetas.
     *
     * Antes todo esto estaba cableado a las cuatro medidas del Iris, así que el
     * núcleo aceptaba cualquier CSV pero la interfaz no.
     */
    private void usarDataset(Dataset d, String nombre, boolean esIris) {
        this.dataset = d;
        this.datasetEsIris = esIris;
        this.map = null;
        this.startPressed = false;
        TextA.setText("");

        construirCamposDeEntrada();
        elegirParesDeEjes();
        configurarSelectores();
        asignarColores();
        construirLeyenda();
        actualizarPanelInferior();

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

    /** Rellena los ocho desplegables con las variables del dataset. */
    private void configurarSelectores() {
        actualizandoSelectores = true;
        try {
            String[] nombres = dataset.getFeatureNames();
            for (int k = 0; k < 4; k++) {
                ComboBox<String> cx = selectoresX().get(k), cy = selectoresY().get(k);
                cx.getItems().setAll(nombres);
                cy.getItems().setAll(nombres);
                cx.getSelectionModel().select(paresDeEjes[k][0]);
                cy.getSelectionModel().select(paresDeEjes[k][1]);
            }
        } finally {
            actualizandoSelectores = false;
        }
    }

    /** Cambia el par de variables de una gráfica y la vuelve a dibujar. */
    private void cambiarEje(int k) {
        if (actualizandoSelectores) return;

        int x = selectoresX().get(k).getSelectionModel().getSelectedIndex();
        int y = selectoresY().get(k).getSelectionModel().getSelectedIndex();
        if (x < 0 || y < 0) return;

        paresDeEjes[k] = new int[]{x, y};
        String[] nombres = dataset.getFeatureNames();
        ScatterChart<Number, Number> chart = graficas().get(k);
        ((NumberAxis) chart.getXAxis()).setLabel(nombres[x]);
        ((NumberAxis) chart.getYAxis()).setLabel(nombres[y]);

        redibujar(k);
    }

    /** Redibuja una gráfica a partir de las capas que hay guardadas. */
    private void redibujar(int k) {
        ScatterChart<Number, Number> chart = graficas().get(k);
        chart.getData().clear();
        for (Capa capa : capas) dibujarCapa(chart, k, capa);
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
            usarDataset(d, elegido.getName(), false);
        } catch (Exception e) {
            showAlert("Could not read the dataset: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Enseña el carrusel de fotos con el Iris, y con cualquier otro dataset la
     * distribución de muestras por etiqueta, que sí dice algo de los datos.
     */
    private void actualizarPanelInferior() {
        ImgAnchor.setVisible(datasetEsIris);
        ImgAnchor.setManaged(datasetEsIris);
        distribucionCanvas.setVisible(!datasetEsIris);
        dibujarDistribucion();
    }

    private void dibujarDistribucion() {
        if (datasetEsIris || dataset == null) return;
        double ancho = distribucionCanvas.getWidth(), alto = distribucionCanvas.getHeight();
        if (ancho <= 0 || alto <= 0) return;

        Map<String, Integer> cuenta = contarPorEtiqueta(dataset);

        GraphicsContext g = distribucionCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, ancho, alto);
        g.setFill(Color.web("#bbbbbb"));
        g.setFont(Font.font("System", 12));
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText("Samples per label", 6, 14);

        if (cuenta.isEmpty()) {
            g.fillText("(the dataset has no labels)", 6, 34);
            return;
        }

        int maximo = cuenta.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        double y = 26, altoBarra = Math.min(24, (alto - 34) / cuenta.size() - 6);
        double anchoMax = ancho - 110;

        for (Map.Entry<String, Integer> e : cuenta.entrySet()) {
            g.setFill(coloresPorEtiqueta.getOrDefault(e.getKey(), Color.GRAY));
            g.fillRect(6, y, Math.max(2, anchoMax * e.getValue() / maximo), altoBarra);
            g.setFill(Color.web("#dddddd"));
            g.fillText(e.getKey() + "  (" + e.getValue() + ")", 10, y + altoBarra - 4);
            y += altoBarra + 6;
        }
    }

    /** Cuántas muestras aporta cada etiqueta, en orden de aparición. */
    static Map<String, Integer> contarPorEtiqueta(Dataset d) {
        Map<String, Integer> cuenta = new LinkedHashMap<>();
        for (Sample s : d.getSamples()) {
            if (s.getLabel() == null) continue;
            String t = s.getLabel().trim().toLowerCase();
            int guion = t.lastIndexOf('-');
            cuenta.merge(guion >= 0 ? t.substring(guion + 1) : t, 1, Integer::sum);
        }
        return cuenta;
    }

    private void changeImage() {
        if (!datasetEsIris) return;
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

        // La muestra introducida a mano sustituye a la anterior, no se acumula.
        if (!capas.isEmpty() && capas.get(capas.size() - 1).color() == Color.YELLOW) {
            capas.remove(capas.size() - 1);
            for (int k = 0; k < graficas().size(); k++) redibujar(k);
        }
        pintar(List.of(entrada), Color.YELLOW);
    }

    // ---------- barrido de hiperparámetros ----------

    /**
     * Prueba muchas combinaciones de parámetros y deja la mejor en el formulario.
     *
     * Corre en un hilo aparte: el barrido son cientos de entrenamientos y en el
     * hilo de la interfaz la dejaría congelada. Mientras dura se desactivan los
     * botones que tocan el mapa, porque la semilla y el BMUStock son globales.
     */
    public void buscarHiperparametros(ActionEvent actionEvent) {
        // Búsqueda bayesiana (TPE), no aleatoria: cada configuración que se prueba
        // sale de lo aprendido en las anteriores.
        //
        // El presupuesto es el mismo de antes —unas 40 pasadas de validación
        // cruzada— pero repartido distinto: 13 configuraciones evaluadas con 3
        // particiones cada una, en vez de 40 con una sola. Medido sobre el Iris,
        // repartirlo así es lo que hace que TPE sirva de algo: con una partición
        // el ranking es medio ruido —la misma configuración se mueve ±1,6 puntos
        // según qué partición le toque— y un modelo ajustado a ruido no mejora a
        // sortear al azar. Con tres, la elegida rinde +0,23 puntos por encima de
        // la del azar (IC 95% de +0,07 a +0,39, sobre 50 semillas).
        final int PRESUPUESTO = 13;
        final int REPETICIONES = 3;
        final int ARRANQUE = 5;
        BusquedaHiperparametros.Espacio espacio = BusquedaHiperparametros.espacioPorDefecto();

        TextA.setText("Bayesian search (TPE): " + PRESUPUESTO + " configurations, each scored "
                + "by 5-fold cross-validation repeated " + REPETICIONES + "×…\n");
        botonesOcupados(true);

        Dataset datos = this.dataset;
        Task<List<BusquedaHiperparametros.Resultado>> tarea = new Task<>() {
            @Override
            protected List<BusquedaHiperparametros.Resultado> call() {
                int[] hechas = {0};
                return BusquedaBayesiana.buscar(datos, espacio, PRESUPUESTO, ARRANQUE, 5,
                        REPETICIONES, 1, r -> {
                    hechas[0]++;
                    int n = hechas[0];
                    Platform.runLater(() -> TextA.appendText(
                            "  " + n + " / " + PRESUPUESTO
                            + (n <= ARRANQUE ? "  (random start)" : "  (modelled)") + "\n"));
                });
            }
        };

        tarea.setOnSucceeded(e -> {
            botonesOcupados(false);
            List<BusquedaHiperparametros.Resultado> res = tarea.getValue();
            aplicar(res.get(0).config());

            TextA.appendText("\nBest of " + res.size() + ":\n");
            for (int i = 0; i < Math.min(5, res.size()); i++) {
                BusquedaHiperparametros.Resultado r = res.get(i);
                TextA.appendText(String.format("  %.1f%% ± %.1f  %s%n",
                        r.acierto(), r.desviacion(), r.config()));
            }
            TextA.appendText("\nForm filled with the best one. Press Start and Train.\n"
                    + "Note: this score is optimistic — picking the winner out of "
                    + res.size() + " already used these data. Measured on Iris, the winner "
                    + "scores about 0.9 points lower on splits the search never saw.\n");
        });

        tarea.setOnFailed(e -> {
            botonesOcupados(false);
            Throwable t = tarea.getException();
            showAlert("The search failed: " + (t == null ? "unknown" : t.getMessage()),
                    Alert.AlertType.ERROR);
        });

        Thread hilo = new Thread(tarea, "busqueda-hiperparametros");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Vuelca una configuración en el formulario. */
    private void aplicar(BusquedaHiperparametros.Config c) {
        NeuronsTF.setText(String.valueOf(c.neuronas()));
        EpochsTF.setText(String.valueOf(c.epocas()));
        RadiusTF.setText(String.valueOf(c.radio()));
        LearningRateTF.setText(String.valueOf(c.tasaAprendizaje()));
        TopologyCB.getSelectionModel().select(switch (c.topologia()) {
            case HEX -> TOPOLOGIA_HEX;
            case GRID -> TOPOLOGIA_REJILLA;
            case RING -> TOPOLOGIA_ANILLO;
        });
    }

    private void botonesOcupados(boolean ocupado) {
        tuneButton.setDisable(ocupado);
        startButton.setDisable(ocupado);
        trainButton.setDisable(ocupado);
        mapButton.setDisable(ocupado);
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
                map = new SOM(epochs, rejilla[0], rejilla[1], learningRate, radius, dataset,
                        topologiaSeleccionada());
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
        if (!map.esRejilla()) {
            showAlert("These readings need a 2-D map. Choose \"" + TOPOLOGIA_REJILLA
                    + "\" or \"" + TOPOLOGIA_HEX + "\" and press Start again.", Alert.AlertType.WARNING);
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
        String sel = TopologyCB.getSelectionModel().getSelectedItem();
        return TOPOLOGIA_REJILLA.equals(sel) || TOPOLOGIA_HEX.equals(sel);
    }

    private SOM.Topology topologiaSeleccionada() {
        return TOPOLOGIA_HEX.equals(TopologyCB.getSelectionModel().getSelectedItem())
                ? SOM.Topology.HEX : SOM.Topology.GRID;
    }

    /** Reparte n neuronas en la rejilla más cuadrada posible. */
    static int[] repartirEnRejilla(int n) {
        return SOM.rejillaPara(n);
    }

    // ---------- gráficas ----------

    private void limpiarGraficas() {
        capas.clear();
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
        Capa capa = new Capa(List.copyOf(muestras), color);
        capas.add(capa);
        List<ScatterChart<Number, Number>> cs = graficas();
        for (int k = 0; k < cs.size(); k++) dibujarCapa(cs.get(k), k, capa);
    }

    private void dibujarCapa(ScatterChart<Number, Number> chart, int k, Capa capa) {
        List<Sample> muestras = capa.muestras();
        double[] x = new double[muestras.size()];
        double[] y = new double[muestras.size()];
        for (int i = 0; i < muestras.size(); i++) {
            x[i] = muestras.get(i).get(paresDeEjes[k][0]);
            y[i] = muestras.get(i).get(paresDeEjes[k][1]);
        }
        updateChartDataGroup(chart, x, y, capa.color());
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
