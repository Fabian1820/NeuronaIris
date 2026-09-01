package panal.data;

import panal.RandomFeaturesPicker;
import panal.SOM;
import panal.utiles.BMUStock;
import panal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vuelca el mapa entrenado a PNG con Java2D (sin JavaFX, así corre headless).
 * Sirve para revisar el resultado sin abrir la app y para las capturas del README.
 */
class ExportarMapaTest {

    private static final SOM.Topology TOPOLOGIA =
            SOM.Topology.valueOf(System.getProperty("mapa.topologia", "GRID"));

    private static final int CELDA = 46;
    private static final int MARGEN = 30;

    /** Hueco del título de la imagen, del de cada panel y del pie con la escala. */
    private static final int CABECERA = 40;
    private static final int ALTO_TITULO = 24;
    private static final int ALTO_LEYENDA = 34;

    @Test
    @DisplayName("Exporta U-matrix, planos de componentes y etiquetas a PNG")
    void exportarPng() throws IOException {
        RandomFeaturesPicker.setSeed(21);
        BMUStock.clear();

        SOM som = new SOM(40, 8, 6, 0.5, 2, GestorTxt.getIrisDataset(), TOPOLOGIA);
        som.initialize();
        som.train();

        Dataset d = som.getDataset();
        int paneles = 2 + d.dimension();          // U-matrix + etiquetas + una por variable
        int columnasPanel = 3;
        int filasPanel = (int) Math.ceil(paneles / (double) columnasPanel);

        // El panel se mide por lo que ocupa el dibujo, no por filas x CELDA: una
        // rejilla hexagonal avanza 1.5 s por fila y no una celda entera, así que
        // reservar de más dejaba una franja vacía al pie de la imagen.
        int anchoPanel = anchoContenido(som) + MARGEN * 2;
        int altoPanel = ALTO_TITULO + altoContenido(som) + ALTO_LEYENDA + MARGEN;

        BufferedImage img = new BufferedImage(anchoPanel * columnasPanel,
                altoPanel * filasPanel + CABECERA, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x1e1e1e));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());

        g.setColor(new Color(0xdddddd));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("SOM sobre Iris · rejilla " + (TOPOLOGIA == SOM.Topology.HEX ? "hexagonal " : "")
                + som.getRows() + "x" + som.getCols() + " · 40 épocas · radio 2", MARGEN, 26);

        int panel = 0;
        panel = dibujarMatriz(g, SOMAnalysis.uMatrix(som), "U-matrix", panel,
                columnasPanel, anchoPanel, altoPanel, som);
        panel = dibujarEtiquetas(g, som, panel, columnasPanel, anchoPanel, altoPanel);
        for (int k = 0; k < d.dimension(); k++) {
            panel = dibujarMatriz(g, SOMAnalysis.componentPlane(som, k),
                    d.getFeatureNames()[k], panel, columnasPanel, anchoPanel, altoPanel, som);
        }
        g.dispose();

        File salida = new File(System.getProperty("mapa.salida",
                TOPOLOGIA == SOM.Topology.HEX ? "target/mapa-som-hex.png" : "target/mapa-som.png"));
        salida.getParentFile().mkdirs();
        ImageIO.write(img, "png", salida);

        System.out.println("mapa exportado a " + salida.getAbsolutePath());
        assertTrue(salida.isFile() && salida.length() > 0);
    }

    private int dibujarMatriz(Graphics2D g, double[][] m, String titulo, int panel,
                              int columnasPanel, int anchoPanel, int altoPanel, SOM som) {
        int px = (panel % columnasPanel) * anchoPanel;
        int py = (panel / columnasPanel) * altoPanel + CABECERA;

        g.setColor(new Color(0xbbbbbb));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(titulo, px + MARGEN, py + 16);

        double[] r = SOMAnalysis.rango(m);
        double rango = (r[1] - r[0]) == 0 ? 1 : (r[1] - r[0]);

        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                g.setColor(color((m[i][j] - r[0]) / rango));
                celda(g, px + MARGEN, py + ALTO_TITULO, i, j);
            }
        }

        barraDeColor(g, px + MARGEN, py + ALTO_TITULO + altoContenido(som) + 12,
                anchoContenido(som), r[0], r[1]);
        return panel + 1;
    }

    /**
     * La escala del panel, con sus extremos numerados.
     *
     * Sin esto la imagen enseña la forma del mapa pero no deja leer la magnitud:
     * se ve dónde hay frontera, no cuánta.
     */
    private void barraDeColor(Graphics2D g, int x, int y, int ancho, double min, double max) {
        int alto = 9;
        for (int i = 0; i < ancho; i++) {
            g.setColor(color(i / (double) (ancho - 1)));
            g.fillRect(x + i, y, 1, alto);
        }
        g.setColor(new Color(0x888888));
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String izq = String.format("%.2f", min), der = String.format("%.2f", max);
        g.drawString(izq, x, y + alto + 12);
        g.drawString(der, x + ancho - g.getFontMetrics().stringWidth(der), y + alto + 12);
    }

    /** La misma escala para las celdas y para la barra: si difieren, la leyenda miente. */
    private static Color color(double t) {
        float u = (float) Math.max(0, Math.min(1, t));
        return new Color((float) Math.pow(u, 0.75),
                (float) (Math.pow(u, 1.35) * 0.85),
                (float) (Math.pow(1 - u, 1.8) * 0.45 + u * 0.25));
    }

    /** Lo que ocupa de ancho la rejilla dibujada. */
    private static int anchoContenido(SOM som) {
        if (TOPOLOGIA != SOM.Topology.HEX) return som.getCols() * CELDA;
        double s = CELDA / 1.8;
        return (int) Math.ceil(Math.sqrt(3) * s * (som.getCols() + 0.5));
    }

    /** Y de alto: una rejilla hexagonal avanza 1.5 s por fila, no una celda. */
    private static int altoContenido(SOM som) {
        if (TOPOLOGIA != SOM.Topology.HEX) return som.getRows() * CELDA;
        double s = CELDA / 1.8;
        return (int) Math.ceil(s * (1.5 * (som.getRows() - 1) + 2));
    }

    /** Cuadrado o hexágono según la topología del mapa. */
    private void celda(Graphics2D g, int x0, int y0, int fila, int columna) {
        if (TOPOLOGIA != SOM.Topology.HEX) {
            g.fillRect(x0 + columna * CELDA, y0 + fila * CELDA, CELDA - 2, CELDA - 2);
            return;
        }
        double s = CELDA / 1.8;
        double w = Math.sqrt(3) * s;
        double cx = x0 + w * (columna + ((fila % 2 == 1) ? 1.0 : 0.5));
        double cy = y0 + s * (1 + 1.5 * fila);

        int[] xs = new int[6], ys = new int[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(60 * i - 90);
            xs[i] = (int) Math.round(cx + (s - 0.8) * Math.cos(a));
            ys[i] = (int) Math.round(cy + (s - 0.8) * Math.sin(a));
        }
        g.fillPolygon(xs, ys, 6);
    }

    private int dibujarEtiquetas(Graphics2D g, SOM som, int panel, int columnasPanel,
                                 int anchoPanel, int altoPanel) {
        int px = (panel % columnasPanel) * anchoPanel;
        int py = (panel / columnasPanel) * altoPanel + CABECERA;

        g.setColor(new Color(0xbbbbbb));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString("especie dominante", px + MARGEN, py + 16);

        String[][] etiquetas = SOMAnalysis.labelGrid(som);
        var colores = new java.util.LinkedHashMap<String, Color>();
        Color[] paleta = {new Color(0xe05252), new Color(0x4caf50), new Color(0x4a90d9)};

        for (int i = 0; i < etiquetas.length; i++) {
            for (int j = 0; j < etiquetas[i].length; j++) {
                String e = etiquetas[i][j];
                Color c = new Color(0x3a3a3a);
                if (e != null) {
                    colores.computeIfAbsent(e, k -> paleta[colores.size() % paleta.length]);
                    c = colores.get(e);
                }
                g.setColor(c);
                celda(g, px + MARGEN, py + ALTO_TITULO, i, j);
            }
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        int y = py + ALTO_TITULO + altoContenido(som) + 21;
        int x = px + MARGEN;
        for (var e : colores.entrySet()) {
            g.setColor(e.getValue());
            g.fillRect(x, y - 9, 10, 10);
            g.setColor(new Color(0xbbbbbb));
            g.drawString(e.getKey(), x + 14, y);
            x += 22 + g.getFontMetrics().stringWidth(e.getKey());
        }
        return panel + 1;
    }
}
