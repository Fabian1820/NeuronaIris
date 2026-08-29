package com.example.edfinal;

import com.example.edfinal.data.Sample;
import com.example.edfinal.data.SOMAnalysis;
import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Topología hexagonal: seis vecinas y filas impares desplazadas. */
class HexagonalTest {

    private SOM hexagonal(int filas, int columnas, int epocas, int radio) {
        RandomFeaturesPicker.setSeed(19);
        BMUStock.clear();
        SOM som = new SOM(epocas, filas, columnas, 0.5, radio,
                GestorTxt.getIrisDataset(), SOM.Topology.HEX);
        som.initialize();
        return som;
    }

    private SOMNeuron en(SOM som, int fila, int columna) {
        return (SOMNeuron) som.getVerticesList().get(fila * som.getCols() + columna);
    }

    /** Fila y columna de cada vecina de una neurona. */
    private Set<String> vecinasDe(SOM som, int fila, int columna) {
        Set<String> out = new HashSet<>();
        for (Vertex v : en(som, fila, columna).getAdjacents()) {
            int[] p = som.positionOf((SOMNeuron) v);
            out.add(p[0] + "," + p[1]);
        }
        return out;
    }

    @Test
    @DisplayName("Una neurona interior tiene exactamente 6 vecinas")
    void seisVecinasEnElInterior() {
        SOM som = hexagonal(7, 7, 1, 1);

        // (3,3) está rodeada por todos lados en una rejilla 7x7
        assertEquals(6, en(som, 3, 3).getAdjacents().size());
        assertEquals(6, en(som, 4, 3).getAdjacents().size(), "también en fila impar");
    }

    @Test
    @DisplayName("En fila par las vecinas de abajo son (c-1) y (c); en impar, (c) y (c+1)")
    void desplazamientoDeLasFilasImpares() {
        SOM som = hexagonal(7, 7, 1, 1);

        // Fila 2 es par: abajo toca (3,2) y (3,3)
        Set<String> desdePar = vecinasDe(som, 2, 3);
        assertTrue(desdePar.contains("3,2"), desdePar.toString());
        assertTrue(desdePar.contains("3,3"), desdePar.toString());
        assertFalse(desdePar.contains("3,4"), "una fila par no toca c+1 abajo: " + desdePar);

        // Fila 3 es impar: abajo toca (4,3) y (4,4)
        Set<String> desdeImpar = vecinasDe(som, 3, 3);
        assertTrue(desdeImpar.contains("4,3"), desdeImpar.toString());
        assertTrue(desdeImpar.contains("4,4"), desdeImpar.toString());
        assertFalse(desdeImpar.contains("4,2"), "una fila impar no toca c-1 abajo: " + desdeImpar);
    }

    @Test
    @DisplayName("La vecindad es simétrica: si A toca a B, B toca a A")
    void vecindadSimetrica() {
        SOM som = hexagonal(6, 6, 1, 1);
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                for (String vecina : vecinasDe(som, r, c)) {
                    String[] p = vecina.split(",");
                    assertTrue(vecinasDe(som, Integer.parseInt(p[0]), Integer.parseInt(p[1]))
                                    .contains(r + "," + c),
                            "(" + r + "," + c + ") toca a " + vecina + " pero no al revés");
                }
            }
        }
    }

    @Test
    @DisplayName("Las esquinas y bordes tienen menos de 6, y nadie más de 6")
    void gradosEnLosBordes() {
        SOM som = hexagonal(6, 6, 1, 1);
        for (Vertex v : som.getVerticesList()) {
            int grado = ((SOMNeuron) v).getAdjacents().size();
            assertTrue(grado >= 2 && grado <= 6, "grado fuera de rango: " + grado);
        }
        assertTrue(en(som, 0, 0).getAdjacents().size() < 6, "una esquina no puede tener 6");
    }

    @Test
    @DisplayName("La vecindad por saltos crece más rápido que en la rectangular")
    void vecindadMasAmplia() {
        SOM hex = hexagonal(9, 9, 1, 1);

        RandomFeaturesPicker.setSeed(19);
        BMUStock.clear();
        SOM rect = new SOM(1, 9, 9, 0.5, 1, GestorTxt.getIrisDataset());
        rect.initialize();

        int centro = 4 * 9 + 4;
        int enHex = hex.neighborhood((SOMNeuron) hex.getVerticesList().get(centro), 2).size();
        int enRect = rect.neighborhood((SOMNeuron) rect.getVerticesList().get(centro), 2).size();

        assertEquals(7, hex.neighborhood((SOMNeuron) hex.getVerticesList().get(centro), 1).size(),
                "centro + 6 vecinas");
        assertEquals(19, enHex, "a 2 saltos: 1 + 6 + 12");
        assertTrue(enHex > enRect, "hex " + enHex + " debería superar a rect " + enRect);
    }

    @Test
    @DisplayName("Entrena y clasifica el Iris, y da U-matrix")
    void entrenaYClasifica() {
        SOM som = hexagonal(8, 6, 40, 2);
        som.train();

        int aciertos = 0;
        for (Sample s : som.getDataset().getSamples()) {
            if (s.getLabel().toLowerCase().contains(som.classify(som.findBMU(s)))) aciertos++;
        }
        double acierto = aciertos * 100.0 / som.getDataset().size();
        System.out.printf("rejilla hexagonal 8x6 -> acierto %.1f%%%n", acierto);

        assertTrue(acierto > 90.0, "acierto: " + acierto);
        double[] r = SOMAnalysis.rango(SOMAnalysis.uMatrix(som));
        assertTrue(r[1] > r[0], "la U-matrix debe tener relieve");
    }

    @Test
    @DisplayName("Una topología que no es 2-D se rechaza en el constructor de rejilla")
    void topologiaInvalida() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> new SOM(10, 4, 4, 0.5, 1, GestorTxt.getIrisDataset(), SOM.Topology.RING));
        assertTrue(e.getMessage().contains("GRID"), e.getMessage());
    }
}
