package panal.ui;

import panal.data.Dataset;
import panal.data.Sample;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * La franja de abajo, que enseña una cosa u otra según el dataset.
 *
 * Con el Iris pasa fotos de las tres especies, que es lo que traía el proyecto
 * original. Con cualquier otro CSV esas fotos no pintan nada, así que en su
 * lugar dibuja cuántas muestras aporta cada etiqueta —que sí dice algo de los
 * datos y avisa si vienen desbalanceados.
 */
public class PanelInferior {

    // Rutas dentro del classpath: funcionan en cualquier máquina y dentro del jar.
    private static final String[] IMAGENES = {
            "/Imagen/FLORP.jpg",
            "/Imagen/MORP.jpg",
            "/Imagen/OIPP.jpg",
            "/Imagen/RP.jpg",
            "/Imagen/SPIP.jpg"
    };

    private final AnchorPane marcoImagen;
    private final ImageView imagen;
    private final Canvas lienzo;

    private Dataset dataset;
    private boolean esIris = true;
    private Map<String, Color> colores = Map.of();
    private int imagenActual = 0;

    public PanelInferior(AnchorPane marcoImagen, ImageView imagen,
                         StackPane contenedor, Canvas lienzo) {
        this.marcoImagen = marcoImagen;
        this.imagen = imagen;
        this.lienzo = lienzo;

        // El Canvas no se redimensiona solo dentro de un StackPane.
        lienzo.widthProperty().bind(contenedor.widthProperty());
        lienzo.heightProperty().bind(contenedor.heightProperty());
        lienzo.widthProperty().addListener((o, a, b) -> dibujar());
        lienzo.heightProperty().addListener((o, a, b) -> dibujar());

        Timeline carrusel = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> siguienteImagen()));
        carrusel.setCycleCount(Timeline.INDEFINITE);
        carrusel.play();
    }

    /** @param colores los mismos que usan las gráficas, para que las barras casen */
    public void usarDataset(Dataset datos, boolean esIris, Map<String, Color> colores) {
        this.dataset = datos;
        this.esIris = esIris;
        this.colores = colores;

        marcoImagen.setVisible(esIris);
        marcoImagen.setManaged(esIris);
        lienzo.setVisible(!esIris);
        dibujar();
    }

    /** Cuántas muestras aporta cada etiqueta, en orden de aparición. */
    public static Map<String, Integer> contarPorEtiqueta(Dataset d) {
        Map<String, Integer> cuenta = new LinkedHashMap<>();
        for (Sample s : d.getSamples()) {
            if (s.getLabel() == null) continue;
            cuenta.merge(Etiquetas.corta(s.getLabel()), 1, Integer::sum);
        }
        return cuenta;
    }

    // ---------- interior ----------

    private void dibujar() {
        if (esIris || dataset == null) return;
        double ancho = lienzo.getWidth(), alto = lienzo.getHeight();
        if (ancho <= 0 || alto <= 0) return;

        Map<String, Integer> cuenta = contarPorEtiqueta(dataset);

        GraphicsContext g = lienzo.getGraphicsContext2D();
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
            g.setFill(colores.getOrDefault(e.getKey(), Color.GRAY));
            g.fillRect(6, y, Math.max(2, anchoMax * e.getValue() / maximo), altoBarra);
            g.setFill(Color.web("#dddddd"));
            g.fillText(e.getKey() + "  (" + e.getValue() + ")", 10, y + altoBarra - 4);
            y += altoBarra + 6;
        }
    }

    private void siguienteImagen() {
        if (!esIris) return;
        var stream = getClass().getResourceAsStream(IMAGENES[imagenActual]);
        if (stream != null) imagen.setImage(new Image(stream));
        imagenActual = (imagenActual + 1) % IMAGENES.length;
    }
}
