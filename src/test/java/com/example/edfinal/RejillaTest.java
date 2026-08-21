package com.example.edfinal;

import com.example.edfinal.data.Dataset;
import com.example.edfinal.data.Sample;
import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Topología de rejilla 2-D y vecindad por recorrido en anchura. */
class RejillaTest {

    private SOM rejilla(int filas, int columnas, int epocas, int radio) {
        RandomFeaturesPicker.setSeed(13);
        BMUStock.clear();
        SOM som = new SOM(epocas, filas, columnas, 0.5, radio, GestorTxt.getIrisDataset());
        som.initialize();
        return som;
    }

    @Test
    @DisplayName("La rejilla tiene filas x columnas neuronas")
    void tamanoDeLaRejilla() {
        SOM som = rejilla(6, 5, 1, 1);
        assertEquals(30, som.getVerticesList().size());
        assertEquals(SOM.Topology.GRID, som.getTopology());
    }

    @Test
    @DisplayName("Las esquinas tienen 2 vecinas, los bordes 3 y el interior 4")
    void gradosDeLaRejilla() {
        int filas = 5, columnas = 5;
        SOM som = rejilla(filas, columnas, 1, 1);

        for (int r = 0; r < filas; r++) {
            for (int c = 0; c < columnas; c++) {
                SOMNeuron n = (SOMNeuron) som.getVerticesList().get(r * columnas + c);
                int esperado = 4;
                if (r == 0 || r == filas - 1) esperado--;
                if (c == 0 || c == columnas - 1) esperado--;

                assertEquals(esperado, n.getAdjacents().size(),
                        "la neurona en (" + r + "," + c + ") debería tener " + esperado + " vecinas");
            }
        }
    }

    @Test
    @DisplayName("La vecindad por saltos crece como un rombo alrededor de la neurona")
    void vecindadEnRombo() {
        SOM som = rejilla(7, 7, 1, 1);
        SOMNeuron centro = (SOMNeuron) som.getVerticesList().get(3 * 7 + 3);

        Map<SOMNeuron, Integer> a1 = som.neighborhood(centro, 1);
        Map<SOMNeuron, Integer> a2 = som.neighborhood(centro, 2);

        // En una rejilla, a r saltos del centro hay 1 + 2r(r+1) neuronas.
        assertEquals(5, a1.size(), "centro + 4 vecinas");
        assertEquals(13, a2.size(), "centro + 4 + 8");
        assertEquals(0, a1.get(centro), "la propia neurona está a distancia 0");
    }

    @Test
    @DisplayName("En el anillo la vecindad sigue siendo la de antes")
    void vecindadDelAnilloNoCambia() {
        RandomFeaturesPicker.setSeed(13);
        BMUStock.clear();
        SOM anillo = new SOM(1, 20, 0.5, 2);
        anillo.initialize();

        SOMNeuron n = (SOMNeuron) anillo.getVerticesList().get(10);
        // Cada neurona conecta con 2 anteriores y 2 siguientes: a 1 salto hay 4.
        assertEquals(5, anillo.neighborhood(n, 1).size());
        assertEquals(4, n.getAdjacents().size());
    }

    @Test
    @DisplayName("Una rejilla entrena y clasifica el Iris")
    void laRejillaEntrenaYClasifica() {
        SOM som = rejilla(6, 5, 30, 2);
        som.train();

        assertTrue(som.isTrained());

        Dataset iris = som.getDataset();
        int aciertos = 0;
        for (Sample s : iris.getSamples()) {
            if (s.getLabel().toLowerCase().contains(som.classify(som.findBMU(s)))) aciertos++;
        }
        double acierto = aciertos * 100.0 / iris.size();
        System.out.printf("rejilla 6x5, 30 épocas, radio 2 -> acierto %.1f%%%n", acierto);

        assertTrue(acierto > 90.0, "la rejilla debería clasificar el Iris bien: " + acierto + "%");
    }

    @Test
    @DisplayName("Una rejilla más pequeña que 2x2 se rechaza")
    void rejillaDemasiadoPequena() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> new SOM(10, 1, 5, 0.5, 1, GestorTxt.getIrisDataset()));
        assertTrue(e.getMessage().contains("2x2"), e.getMessage());
    }

    @Test
    @DisplayName("positionOf devuelve la fila y columna de cada neurona")
    void posicionEnLaRejilla() {
        SOM som = rejilla(4, 6, 1, 1);

        int[] p0 = som.positionOf((SOMNeuron) som.getVerticesList().get(0));
        int[] p7 = som.positionOf((SOMNeuron) som.getVerticesList().get(7));
        int[] pUlt = som.positionOf((SOMNeuron) som.getVerticesList().get(23));

        assertArrayEquals(new int[]{0, 0}, p0);
        assertArrayEquals(new int[]{1, 1}, p7);
        assertArrayEquals(new int[]{3, 5}, pUlt);
    }
}
