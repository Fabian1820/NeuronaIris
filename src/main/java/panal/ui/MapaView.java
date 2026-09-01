package panal.ui;

import panal.SOM;
import panal.data.Dataset;
import panal.data.SOMAnalysis;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 * Vista del mapa entrenado: U-matrix y planos de componentes.
 *
 * A diferencia del FXML principal —posiciones absolutas sobre un lienzo de
 * 1545x881—, esta pantalla se construye con contenedores reales y un Canvas que
 * se redibuja al redimensionar, así que se adapta a cualquier monitor sin
 * escalar nada.
 */
public class MapaView {

    private static final double MARGEN = 16;

    private final SOM som;
    private final Canvas lienzo = new Canvas();
    private final Label leyenda = new Label();
    private final ComboBox<String> selector = new ComboBox<>();

    public MapaView(SOM som) {
        this.som = som;
    }

    public void mostrar() {
        if (som == null || !som.esRejilla()) {
            throw new IllegalStateException("A grid topology map is required");
        }

        selector.getItems().add("U-matrix");
        selector.getItems().add("Labels");
        String[] nombres = som.getDataset().getFeatureNames();
        for (String n : nombres) selector.getItems().add("Variable · " + n);
        selector.getSelectionModel().selectFirst();
        selector.setOnAction(e -> dibujar());

        Label titulo = new Label("Self-organizing map  ·  "
                + (som.getTopology() == SOM.Topology.HEX ? "hex" : "rect") + " grid "
                + som.getRows() + "×" + som.getCols());
        titulo.setFont(Font.font("System", 16));
        titulo.setTextFill(Color.web("#dddddd"));

        HBox barra = new HBox(12, titulo, selector);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(12));

        // El Canvas no tiene tamaño propio: se ata al del panel que lo contiene.
        Pane contenedor = new Pane(lienzo);
        lienzo.widthProperty().bind(contenedor.widthProperty());
        lienzo.heightProperty().bind(contenedor.heightProperty());
        lienzo.widthProperty().addListener((o, a, b) -> dibujar());
        lienzo.heightProperty().addListener((o, a, b) -> dibujar());

        leyenda.setTextFill(Color.web("#aaaaaa"));
        leyenda.setWrapText(true);
        leyenda.setTextAlignment(TextAlignment.LEFT);
        leyenda.setPadding(new Insets(8, 12, 12, 12));

        VBox pie = new VBox(leyenda);
        VBox.setVgrow(contenedor, Priority.ALWAYS);

        BorderPane raiz = new BorderPane();
        raiz.setTop(barra);
        raiz.setCenter(contenedor);
        raiz.setBottom(pie);
        raiz.setStyle("-fx-background-color: #1e1e1e;");

        Stage ventana = new Stage();
        ventana.setTitle("Self-organizing map");
        ventana.setScene(new Scene(raiz, 900, 700));
        ventana.setMinWidth(480);
        ventana.setMinHeight(400);
        ventana.show();

        dibujar();
    }

    private void dibujar() {
        double ancho = lienzo.getWidth(), alto = lienzo.getHeight();
        if (ancho <= 0 || alto <= 0) return;

        GraphicsContext g = lienzo.getGraphicsContext2D();
        g.setFill(Color.web("#1e1e1e"));
        g.fillRect(0, 0, ancho, alto);

        int filas = som.getRows(), columnas = som.getCols();
        boolean hex = som.getTopology() == SOM.Topology.HEX;

        double celda;
        double x0, y0;
        if (hex) {
            // Hexágonos con vértice arriba: ancho √3·s, separación vertical 1.5·s,
            // y las filas impares corridas medio hexágono.
            double porAncho = (ancho - 2 * MARGEN) / (Math.sqrt(3) * (columnas + 0.5));
            double porAlto = (alto - 2 * MARGEN) / (1.5 * (filas - 1) + 2);
            celda = Math.min(porAncho, porAlto);
            x0 = (ancho - Math.sqrt(3) * celda * (columnas + 0.5)) / 2;
            y0 = (alto - (1.5 * (filas - 1) + 2) * celda) / 2;
        } else {
            celda = Math.min((ancho - 2 * MARGEN) / columnas, (alto - 2 * MARGEN) / filas);
            x0 = (ancho - celda * columnas) / 2;
            y0 = (alto - celda * filas) / 2;
        }

        String opcion = selector.getSelectionModel().getSelectedItem();
        if ("Labels".equals(opcion)) {
            dibujarEtiquetas(g, x0, y0, celda, filas, columnas);
        } else {
            dibujarMatriz(g, x0, y0, celda, filas, columnas, opcion);
        }
    }

    /** Pinta la celda (fila, columna) con la forma que toque según la topología. */
    private void pintarCelda(GraphicsContext g, double x0, double y0, double celda,
                             int fila, int columna) {
        if (som.getTopology() != SOM.Topology.HEX) {
            g.fillRect(x0 + columna * celda, y0 + fila * celda, celda - 1, celda - 1);
            return;
        }

        double w = Math.sqrt(3) * celda;
        double cx = x0 + w * (columna + ((fila % 2 == 1) ? 1.0 : 0.5));
        double cy = y0 + celda * (1 + 1.5 * fila);

        double[] xs = new double[6], ys = new double[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(60 * i - 90);   // vértice arriba
            xs[i] = cx + (celda - 0.6) * Math.cos(a);
            ys[i] = cy + (celda - 0.6) * Math.sin(a);
        }
        g.fillPolygon(xs, ys, 6);
    }

    private void dibujarMatriz(GraphicsContext g, double x0, double y0, double celda,
                               int filas, int columnas, String opcion) {
        double[][] m;
        String explicacion;

        if (opcion != null && opcion.startsWith("Variable · ")) {
            String nombre = opcion.substring("Variable · ".length());
            int k = indiceDe(nombre);
            m = SOMAnalysis.componentPlane(som, k);
            explicacion = "Component plane for \u00ab" + nombre + "\u00bb: the value each neuron "
                    + "learned for that variable. Comparing planes shows which variable dominates "
                    + "each area of the map.";
        } else {
            m = SOMAnalysis.uMatrix(som);
            explicacion = "U-matrix: average distance from each neuron to its neighbours. Light "
                    + "areas are boundaries between clusters; dark areas, the inside of a cluster.";
        }

        double[] r = SOMAnalysis.rango(m);
        double min = r[0], max = r[1], rango = (max - min) == 0 ? 1 : (max - min);

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                double t = (m[i][j] - min) / rango;
                g.setFill(colorDeCalor(t));
                pintarCelda(g, x0, y0, celda, i, j);
            }
        }

        leyenda.setText(String.format("%s%nMinimum %.3f (dark)  \u00b7  maximum %.3f (light)",
                explicacion, min, max));
    }

    private void dibujarEtiquetas(GraphicsContext g, double x0, double y0, double celda,
                                  int filas, int columnas) {
        String[][] etiquetas = SOMAnalysis.labelGrid(som);
        var colores = new java.util.LinkedHashMap<String, Color>();
        Color[] paleta = {Color.web("#e05252"), Color.web("#4caf50"), Color.web("#4a90d9"),
                          Color.web("#c9a227"), Color.web("#9b59b6")};

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                String e = etiquetas[i][j];
                Color color = Color.web("#3a3a3a");
                if (e != null) {
                    colores.computeIfAbsent(e, k -> paleta[colores.size() % paleta.length]);
                    color = colores.get(e);
                }
                g.setFill(color);
                pintarCelda(g, x0, y0, celda, i, j);
            }
        }

        StringBuilder sb = new StringBuilder("Dominant species per neuron. ");
        for (var e : colores.entrySet()) sb.append("· ").append(e.getKey()).append(" ");
        sb.append("\u00b7 grey: neuron that won no samples.");
        leyenda.setText(sb.toString());
    }

    /** Escala oscuro → claro, con un toque cálido en los valores altos. */
    private Color colorDeCalor(double t) {
        t = Math.max(0, Math.min(1, t));
        return Color.color(Math.pow(t, 0.75),
                           Math.pow(t, 1.35) * 0.85,
                           Math.pow(1 - t, 1.8) * 0.45 + t * 0.25);
    }

    private int indiceDe(String nombre) {
        String[] nombres = som.getDataset().getFeatureNames();
        for (int i = 0; i < nombres.length; i++) {
            if (nombres[i].equals(nombre)) return i;
        }
        return 0;
    }
}
