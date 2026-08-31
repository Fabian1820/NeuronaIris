package com.example.edfinal.ui;

import com.example.edfinal.data.Dataset;
import com.example.edfinal.data.Sample;
import javafx.geometry.Pos;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Las cuatro gráficas de dispersión, sus selectores de ejes y la leyenda.
 *
 * Trabaja sobre nodos que le pasa el controlador en vez de inyectarlos por
 * FXML: así la pantalla se puede repartir en piezas sin tener que partir
 * también el .fxml, que es donde JavaFX se pone quisquilloso.
 */
public class PanelDispersion {

    /** Colores con los que se pinta cada etiqueta del dataset, en orden de aparición. */
    private static final Color[] PALETA = {
            Color.RED, Color.GREEN, Color.DODGERBLUE, Color.GOLD, Color.MEDIUMORCHID, Color.CORAL
    };

    private final List<ScatterChart<Number, Number>> graficas;
    private final List<ComboBox<String>> selectoresX, selectoresY;
    private final FlowPane leyendaPane;

    private Dataset dataset;

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

    public PanelDispersion(List<ScatterChart<Number, Number>> graficas,
                           List<ComboBox<String>> selectoresX,
                           List<ComboBox<String>> selectoresY,
                           FlowPane leyendaPane) {
        this.graficas = List.copyOf(graficas);
        this.selectoresX = List.copyOf(selectoresX);
        this.selectoresY = List.copyOf(selectoresY);
        this.leyendaPane = leyendaPane;

        for (ScatterChart<Number, Number> c : this.graficas) c.setLegendVisible(false);
        for (int k = 0; k < this.graficas.size(); k++) {
            final int indice = k;
            this.selectoresX.get(k).setOnAction(e -> cambiarEje(indice));
            this.selectoresY.get(k).setOnAction(e -> cambiarEje(indice));
        }
    }

    /** Rehace ejes, selectores, colores y leyenda a partir del dataset. */
    public void usarDataset(Dataset datos) {
        this.dataset = datos;
        elegirParesDeEjes();
        configurarSelectores();
        asignarColores();
        construirLeyenda();
        limpiar();
    }

    /** Los colores asignados a cada etiqueta, para que el resto de la pantalla los use. */
    public Map<String, Color> colores() {
        return Collections.unmodifiableMap(coloresPorEtiqueta);
    }

    public void limpiar() {
        capas.clear();
        for (ScatterChart<Number, Number> c : graficas) c.getData().clear();
    }

    /** Añade una capa de muestras del color dado y la dibuja en las cuatro gráficas. */
    public void pintar(List<Sample> muestras, Color color) {
        if (muestras.isEmpty()) return;
        Capa capa = new Capa(List.copyOf(muestras), color);
        capas.add(capa);
        for (int k = 0; k < graficas.size(); k++) dibujarCapa(k, capa);
    }

    /**
     * Quita la última capa si es del color dado, y repinta.
     *
     * Sirve para que la muestra tecleada a mano sustituya a la anterior en vez
     * de irse acumulando en la gráfica.
     */
    public void quitarUltimaCapaSiEs(Color color) {
        if (capas.isEmpty() || !capas.get(capas.size() - 1).color().equals(color)) return;
        capas.remove(capas.size() - 1);
        for (int k = 0; k < graficas.size(); k++) redibujar(k);
    }

    /**
     * Cuatro pares de variables para las cuatro gráficas.
     *
     * Con menos de cuatro combinaciones posibles se repite la última, para que
     * siempre haya un par por gráfica sea cual sea la dimensión del dataset.
     */
    public static int[][] paresDeVariables(int dim) {
        List<int[]> pares = new ArrayList<>();
        for (int i = 0; i < dim && pares.size() < 4; i++) {
            for (int j = i + 1; j < dim && pares.size() < 4; j++) pares.add(new int[]{i, j});
        }
        // Con una sola variable no hay pares: se dibuja contra sí misma.
        while (pares.size() < 4) pares.add(new int[]{0, Math.min(1, dim - 1)});
        return pares.toArray(new int[0][]);
    }

    // ---------- interior ----------

    private void elegirParesDeEjes() {
        paresDeEjes = paresDeVariables(dataset.dimension());
        String[] nombres = dataset.getFeatureNames();
        for (int k = 0; k < graficas.size(); k++) {
            ((NumberAxis) graficas.get(k).getXAxis()).setLabel(nombres[paresDeEjes[k][0]]);
            ((NumberAxis) graficas.get(k).getYAxis()).setLabel(nombres[paresDeEjes[k][1]]);
        }
    }

    /** Rellena los ocho desplegables con las variables del dataset. */
    private void configurarSelectores() {
        actualizandoSelectores = true;
        try {
            String[] nombres = dataset.getFeatureNames();
            for (int k = 0; k < graficas.size(); k++) {
                ComboBox<String> cx = selectoresX.get(k), cy = selectoresY.get(k);
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
        if (actualizandoSelectores || dataset == null) return;

        int x = selectoresX.get(k).getSelectionModel().getSelectedIndex();
        int y = selectoresY.get(k).getSelectionModel().getSelectedIndex();
        if (x < 0 || y < 0) return;

        paresDeEjes[k] = new int[]{x, y};
        String[] nombres = dataset.getFeatureNames();
        ScatterChart<Number, Number> chart = graficas.get(k);
        ((NumberAxis) chart.getXAxis()).setLabel(nombres[x]);
        ((NumberAxis) chart.getYAxis()).setLabel(nombres[y]);

        redibujar(k);
    }

    /** Redibuja una gráfica a partir de las capas que hay guardadas. */
    private void redibujar(int k) {
        graficas.get(k).getData().clear();
        for (Capa capa : capas) dibujarCapa(k, capa);
    }

    private void asignarColores() {
        coloresPorEtiqueta.clear();
        int i = 0;
        for (String etiqueta : dataset.labels()) {
            coloresPorEtiqueta.put(Etiquetas.corta(etiqueta), PALETA[i++ % PALETA.length]);
        }
    }

    private void construirLeyenda() {
        leyendaPane.getChildren().clear();
        leyendaPane.getChildren().add(entrada(Color.ORANGE, "Data / random weights"));
        for (Map.Entry<String, Color> e : coloresPorEtiqueta.entrySet()) {
            leyendaPane.getChildren().add(entrada(e.getValue(), e.getKey()));
        }
        leyendaPane.getChildren().add(entrada(Color.YELLOW, "BMU of data entry"));
    }

    private HBox entrada(Color color, String texto) {
        Circle punto = new Circle(8, color);
        punto.setStroke(Color.BLACK);
        HBox caja = new HBox(6, punto, new Label(texto));
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }

    private void dibujarCapa(int k, Capa capa) {
        List<Sample> muestras = capa.muestras();
        XYChart.Series<Number, Number> serie = new XYChart.Series<>();
        for (Sample m : muestras) {
            XYChart.Data<Number, Number> punto = new XYChart.Data<>(
                    m.get(paresDeEjes[k][0]), m.get(paresDeEjes[k][1]));
            Circle circulo = new Circle(5);
            circulo.setFill(capa.color());
            punto.setNode(circulo);
            serie.getData().add(punto);
        }
        graficas.get(k).getData().add(serie);
    }
}
