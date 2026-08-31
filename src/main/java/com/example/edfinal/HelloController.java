package com.example.edfinal;

import com.example.edfinal.data.BusquedaBayesiana;
import com.example.edfinal.data.BusquedaHiperparametros;
import com.example.edfinal.data.Dataset;
import com.example.edfinal.data.Sample;
import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.BMUandFManager;
import com.example.edfinal.ui.FormularioEntrada;
import com.example.edfinal.ui.PanelDispersion;
import com.example.edfinal.ui.PanelInferior;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class HelloController implements Initializable {

    private static final String TOPOLOGIA_ANILLO = "1-D ring";
    private static final String TOPOLOGIA_REJILLA = "2-D grid";
    private static final String TOPOLOGIA_HEX = "2-D hex grid";

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

    /** El carrusel de fotos solo tiene sentido con el Iris. */
    private boolean datasetEsIris = true;

    // Las tres piezas en las que se reparte la pantalla. El controlador solo las
    // cablea y las coordina; el dibujo y el estado de cada zona vive en ellas.
    private PanelDispersion dispersion;
    private PanelInferior inferior;
    private FormularioEntrada formulario;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startPressed = false;

        TopologyCB.getItems().setAll(TOPOLOGIA_ANILLO, TOPOLOGIA_REJILLA, TOPOLOGIA_HEX);
        TopologyCB.getSelectionModel().select(TOPOLOGIA_HEX);

        dispersion = new PanelDispersion(
                List.of(chart0, chart1, chart2, chart3),
                List.of(xVar0, xVar1, xVar2, xVar3),
                List.of(yVar0, yVar1, yVar2, yVar3),
                leyendaPane);
        inferior = new PanelInferior(ImgAnchor, ImgView, panelInferior, distribucionCanvas);
        formulario = new FormularioEntrada(entradaGrid);

        usarDataset(GestorTxt.getIrisDataset(), "Iris", true);
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
        this.map = null;
        this.startPressed = false;
        TextA.setText("");

        aplicarDataset(d, nombre, esIris);
        dispersion.pintar(d.getSamples(), Color.ORANGE);
    }

    /**
     * Rehace la pantalla para un dataset sin tocar el mapa.
     *
     * Separado de {@link #usarDataset} porque "Load Map" trae su propio dataset
     * dentro del mapa guardado: antes no pasaba por aquí y las gráficas se
     * quedaban con los ejes del dataset anterior, lo que reventaba si el mapa
     * cargado tenía menos variables que el que había en pantalla.
     */
    private void aplicarDataset(Dataset d, String nombre, boolean esIris) {
        this.dataset = d;
        this.datasetEsIris = esIris;

        formulario.reconstruir(d);
        dispersion.usarDataset(d);
        inferior.usarDataset(d, esIris, dispersion.colores());

        datasetLabel.setText("Data Entry  ·  " + nombre + "  ("
                + d.size() + " samples, " + d.dimension() + " variables)");
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

        double[] valores = formulario.valores();
        if (valores == null) {
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
        dispersion.quitarUltimaCapaSiEs(Color.YELLOW);
        dispersion.pintar(List.of(entrada), Color.YELLOW);
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
        this.map = GestorTxt.loadMap();
        aplicarDataset(map.getDataset(), "saved map", false);
        GestorTxt.writeHeaderConfig(map);
        map.groupBmus(dataset.getSamples());
        pintarGrupos();
        TextA.setText("");
        this.startPressed = true;
    }

    public void start(ActionEvent actionEvent) {
        dispersion.limpiar();
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
                int[] rejilla = SOM.rejillaPara(neurons);
                map = new SOM(epochs, rejilla[0], rejilla[1], learningRate, radius, dataset,
                        topologiaSeleccionada());
            } else {
                map = new SOM(epochs, neurons, learningRate, radius, dataset);
            }
            GestorTxt.writeHeaderConfig(map);
            map.initialize();
            mostrarPesos();
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
        dispersion.limpiar();
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

    // ---------- gráficas ----------

    /** Dibuja los pesos actuales de las neuronas. */
    private void mostrarPesos() {
        List<Sample> pesos = new ArrayList<>();
        for (Vertex v : map.getVerticesList()) pesos.add(((SOMNeuron) v).getWeights());
        dispersion.pintar(pesos, Color.ORANGE);
    }

    /** Dibuja las BMUs de cada etiqueta con su color. */
    private void pintarGrupos() {
        for (Map.Entry<String, Color> e : dispersion.colores().entrySet()) {
            List<Sample> pesos = new ArrayList<>();
            for (SOMNeuron n : BMUStock.forLabel(e.getKey())) pesos.add(n.getWeights());
            dispersion.pintar(pesos, e.getValue());
        }
    }

    private void showAlert(String message, Alert.AlertType at) {
        Alert alert = new Alert(at);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(ImgAnchor.getScene().getWindow());

        // Un Alert abre escena propia, así que no hereda la hoja de estilos de
        // la ventana principal: sin esto salen diálogos claros sobre una app
        // oscura. Se reutiliza el mismo tema en vez de una hoja aparte.
        alert.getDialogPane().getStylesheets().addAll(
                ImgAnchor.getScene().getStylesheets());

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
