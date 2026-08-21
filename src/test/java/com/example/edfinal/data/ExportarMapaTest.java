package com.example.edfinal.data;

import com.example.edfinal.RandomFeaturesPicker;
import com.example.edfinal.SOM;
import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
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

    private static final int CELDA = 46;
    private static final int MARGEN = 30;

    @Test
    @DisplayName("Exporta U-matrix, planos de componentes y etiquetas a PNG")
    void exportarPng() throws IOException {
        RandomFeaturesPicker.setSeed(21);
        BMUStock.clear();

        SOM som = new SOM(40, 8, 6, 0.5, 2, GestorTxt.getIrisDataset());
        som.initialize();
        som.train();

        Dataset d = som.getDataset();
        int paneles = 2 + d.dimension();          // U-matrix + etiquetas + una por variable
        int columnasPanel = 3;
        int filasPanel = (int) Math.ceil(paneles / (double) columnasPanel);

        int anchoPanel = som.getCols() * CELDA + MARGEN * 2;
        int altoPanel = som.getRows() * CELDA + MARGEN * 2 + 24;

        BufferedImage img = new BufferedImage(anchoPanel * columnasPanel,
                altoPanel * filasPanel + 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x1e1e1e));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());

        g.setColor(new Color(0xdddddd));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("SOM sobre Iris · rejilla " + som.getRows() + "x" + som.getCols()
                + " · 40 épocas · radio 2", MARGEN, 26);

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
                "target/mapa-som.png"));
        salida.getParentFile().mkdirs();
        ImageIO.write(img, "png", salida);

        System.out.println("mapa exportado a " + salida.getAbsolutePath());
        assertTrue(salida.isFile() && salida.length() > 0);
    }

    private int dibujarMatriz(Graphics2D g, double[][] m, String titulo, int panel,
                              int columnasPanel, int anchoPanel, int altoPanel, SOM som) {
        int px = (panel % columnasPanel) * anchoPanel;
        int py = (panel / columnasPanel) * altoPanel + 40;

        g.setColor(new Color(0xbbbbbb));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(titulo, px + MARGEN, py + 16);

        double[] r = SOMAnalysis.rango(m);
        double rango = (r[1] - r[0]) == 0 ? 1 : (r[1] - r[0]);

        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                float t = (float) ((m[i][j] - r[0]) / rango);
                g.setColor(new Color(
                        (float) Math.pow(t, 0.75),
                        (float) (Math.pow(t, 1.35) * 0.85),
                        (float) (Math.pow(1 - t, 1.8) * 0.45 + t * 0.25)));
                g.fillRect(px + MARGEN + j * CELDA, py + 24 + i * CELDA, CELDA - 2, CELDA - 2);
            }
        }
        return panel + 1;
    }

    private int dibujarEtiquetas(Graphics2D g, SOM som, int panel, int columnasPanel,
                                 int anchoPanel, int altoPanel) {
        int px = (panel % columnasPanel) * anchoPanel;
        int py = (panel / columnasPanel) * altoPanel + 40;

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
                g.fillRect(px + MARGEN + j * CELDA, py + 24 + i * CELDA, CELDA - 2, CELDA - 2);
            }
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        int y = py + 24 + etiquetas.length * CELDA + 14;
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
