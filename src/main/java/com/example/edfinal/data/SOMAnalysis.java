package com.example.edfinal.data;

import com.example.edfinal.SOM;
import com.example.edfinal.SOMNeuron;
import cu.edu.cujae.ceis.graph.vertex.Vertex;

import java.util.List;

/**
 * Lecturas de un mapa ya entrenado: U-matrix, planos de componentes y error
 * topográfico.
 *
 * Son las salidas canónicas de un SOM. Hasta ahora el proyecto solo sabía decir
 * a qué especie se parecía una flor; esto permite además *ver* cómo quedó
 * organizado el mapa.
 */
public class SOMAnalysis {

    private SOMAnalysis() {}

    /**
     * U-matrix: para cada neurona, la distancia media en el espacio de datos a
     * sus vecinas en la rejilla.
     *
     * Los valores altos son fronteras —zonas donde el mapa "se estira" para
     * saltar de un grupo a otro—, y los bajos son el interior de un grupo. Es
     * lo que se dibuja como mapa de calor.
     */
    public static double[][] uMatrix(SOM som) {
        exigirRejilla(som);
        int filas = som.getRows(), columnas = som.getCols();
        double[][] u = new double[filas][columnas];

        for (int r = 0; r < filas; r++) {
            for (int c = 0; c < columnas; c++) {
                SOMNeuron n = neuronaEn(som, r, c);
                double suma = 0;
                int vecinas = 0;
                for (Vertex v : n.getAdjacents()) {
                    suma += n.euclidianDistance(((SOMNeuron) v).getWeights());
                    vecinas++;
                }
                u[r][c] = vecinas == 0 ? 0 : suma / vecinas;
            }
        }
        return u;
    }

    /**
     * Plano de componentes: el valor de una variable concreta en cada celda.
     * Dibujados uno al lado de otro muestran qué variable manda en cada zona
     * del mapa.
     */
    public static double[][] componentPlane(SOM som, int variable) {
        exigirRejilla(som);
        int dim = som.getDataset().dimension();
        if (variable < 0 || variable >= dim) {
            throw new IllegalArgumentException(
                    "La variable " + variable + " no existe: el dataset tiene " + dim);
        }

        int filas = som.getRows(), columnas = som.getCols();
        double[][] plano = new double[filas][columnas];
        for (int r = 0; r < filas; r++) {
            for (int c = 0; c < columnas; c++) {
                plano[r][c] = neuronaEn(som, r, c).getWeights().get(variable);
            }
        }
        return plano;
    }

    /**
     * Error topográfico: proporción de muestras cuya mejor y segunda mejor
     * neurona **no** son vecinas en la rejilla.
     *
     * Mide si el mapa conserva la vecindad de los datos, que es justo lo que un
     * SOM promete y lo que el error de cuantización no captura: un mapa puede
     * representar bien los datos y aun así tenerlos mal ordenados.
     */
    public static double topographicError(SOM som, List<Sample> datos) {
        exigirRejilla(som);
        int fallos = 0;

        for (Sample s : datos) {
            SOMNeuron mejor = null, segunda = null;
            double d1 = Double.MAX_VALUE, d2 = Double.MAX_VALUE;

            for (Vertex v : som.getVerticesList()) {
                SOMNeuron n = (SOMNeuron) v;
                double d = n.euclidianDistance(s);
                if (d < d1) {
                    d2 = d1; segunda = mejor;
                    d1 = d; mejor = n;
                } else if (d < d2) {
                    d2 = d; segunda = n;
                }
            }

            if (segunda == null || !sonVecinas(mejor, segunda)) fallos++;
        }
        return (double) fallos / datos.size();
    }

    /** Etiqueta de cada celda, o null si esa neurona no ganó ninguna muestra. */
    public static String[][] labelGrid(SOM som) {
        exigirRejilla(som);
        int filas = som.getRows(), columnas = som.getCols();
        String[][] etiquetas = new String[filas][columnas];
        for (int r = 0; r < filas; r++) {
            for (int c = 0; c < columnas; c++) {
                SOMNeuron n = neuronaEn(som, r, c);
                String e = som.classify(n);
                etiquetas[r][c] = (e == null || e.isEmpty()) ? null : e;
            }
        }
        return etiquetas;
    }

    /** Mínimo y máximo de una matriz, para escalar el color al dibujarla. */
    public static double[] rango(double[][] m) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double[] fila : m) {
            for (double v : fila) { min = Math.min(min, v); max = Math.max(max, v); }
        }
        return new double[]{min, max};
    }

    private static boolean sonVecinas(SOMNeuron a, SOMNeuron b) {
        for (Vertex v : a.getAdjacents()) {
            if (v == b) return true;
        }
        return false;
    }

    private static SOMNeuron neuronaEn(SOM som, int fila, int columna) {
        return (SOMNeuron) som.getVerticesList().get(fila * som.getCols() + columna);
    }

    private static void exigirRejilla(SOM som) {
        if (som.getTopology() != SOM.Topology.GRID) {
            throw new IllegalStateException(
                    "Estas lecturas necesitan una rejilla 2-D; el mapa es un anillo");
        }
    }
}
