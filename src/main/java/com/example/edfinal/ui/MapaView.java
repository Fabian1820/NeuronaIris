package com.example.edfinal.ui;

import com.example.edfinal.SOM;
import com.example.edfinal.data.Dataset;
import com.example.edfinal.data.SOMAnalysis;
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
        if (som == null || som.getTopology() != SOM.Topology.GRID) {
            throw new IllegalStateException("Se necesita un mapa con topología de rejilla");
        }

        selector.getItems().add("U-matrix");
        selector.getItems().add("Etiquetas");
        String[] nombres = som.getDataset().getFeatureNames();
        for (String n : nombres) selector.getItems().add("Variable · " + n);
        selector.getSelectionModel().selectFirst();
        selector.setOnAction(e -> dibujar());

        Label titulo = new Label("Mapa autoorganizado  ·  rejilla "
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
        ventana.setTitle("Mapa autoorganizado");
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
        double celda = Math.min((ancho - 2 * MARGEN) / columnas, (alto - 2 * MARGEN) / filas);
        double x0 = (ancho - celda * columnas) / 2;
        double y0 = (alto - celda * filas) / 2;

        String opcion = selector.getSelectionModel().getSelectedItem();
        if ("Etiquetas".equals(opcion)) {
            dibujarEtiquetas(g, x0, y0, celda, filas, columnas);
        } else {
            dibujarMatriz(g, x0, y0, celda, filas, columnas, opcion);
        }
    }

    private void dibujarMatriz(GraphicsContext g, double x0, double y0, double celda,
                               int filas, int columnas, String opcion) {
        double[][] m;
        String explicacion;

        if (opcion != null && opcion.startsWith("Variable · ")) {
            String nombre = opcion.substring("Variable · ".length());
            int k = indiceDe(nombre);
            m = SOMAnalysis.componentPlane(som, k);
            explicacion = "Plano de componentes de «" + nombre + "»: el valor que aprendió cada "
                    + "neurona para esa variable. Comparando planos se ve qué variable manda en "
                    + "cada zona del mapa.";
        } else {
            m = SOMAnalysis.uMatrix(som);
            explicacion = "U-matrix: distancia media de cada neurona a sus vecinas. Las zonas "
                    + "claras son fronteras entre grupos; las oscuras, el interior de un grupo.";
        }

        double[] r = SOMAnalysis.rango(m);
        double min = r[0], max = r[1], rango = (max - min) == 0 ? 1 : (max - min);

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                double t = (m[i][j] - min) / rango;
                g.setFill(colorDeCalor(t));
                g.fillRect(x0 + j * celda, y0 + i * celda, celda - 1, celda - 1);
            }
        }

        leyenda.setText(String.format("%s%nMínimo %.3f (oscuro)  ·  máximo %.3f (claro)",
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
                g.fillRect(x0 + j * celda, y0 + i * celda, celda - 1, celda - 1);
            }
        }

        StringBuilder sb = new StringBuilder("Especie que domina cada neurona. ");
        for (var e : colores.entrySet()) sb.append("· ").append(e.getKey()).append(" ");
        sb.append("· gris: neurona que no ganó ninguna muestra.");
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
