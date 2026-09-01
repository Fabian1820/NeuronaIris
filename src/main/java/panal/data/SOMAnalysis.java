package panal.data;

import panal.SOM;
import panal.SOMNeuron;
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
        return topographicError(som, datos, false);
    }

    /**
     * @param incluirDiagonales si una neurona en diagonal cuenta como vecina.
     *
     * La rejilla conecta 4 vecinas (arriba, abajo, izquierda, derecha), así que
     * con el criterio estricto una segunda mejor neurona en diagonal cuenta como
     * fallo aunque esté pegada. Contar diagonales mide la vecindad geométrica en
     * vez de la de las aristas.
     */
    public static double topographicError(SOM som, List<Sample> datos, boolean incluirDiagonales) {
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

            boolean vecinas = incluirDiagonales
                    ? sonContiguas(som, mejor, segunda)
                    : sonVecinas(mejor, segunda);
            if (segunda == null || !vecinas) fallos++;
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

    /**
     * Matriz de confusión: filas la especie real, columnas la predicha, en el
     * orden de {@code etiquetas}.
     *
     * Un porcentaje de acierto suelto esconde *dónde* falla el modelo; esto lo
     * enseña. En Iris se espera ver setosa perfecta y la confusión concentrada
     * entre versicolor y virginica, que es donde se solapan de verdad.
     */
    public static int[][] confusionMatrix(SOM som, List<Sample> datos, List<String> etiquetas) {
        int n = etiquetas.size();
        int[][] m = new int[n][n];

        for (Sample s : datos) {
            if (s.getLabel() == null) continue;
            int real = indiceDe(etiquetas, s.getLabel());
            int predicho = indiceDe(etiquetas, som.classify(som.findBMU(s)));
            if (real >= 0 && predicho >= 0) m[real][predicho]++;
        }
        return m;
    }

    /** Acierto global de una matriz de confusión. */
    public static double accuracy(int[][] confusion) {
        int aciertos = 0, total = 0;
        for (int i = 0; i < confusion.length; i++) {
            for (int j = 0; j < confusion[i].length; j++) {
                total += confusion[i][j];
                if (i == j) aciertos += confusion[i][j];
            }
        }
        return total == 0 ? 0 : aciertos * 100.0 / total;
    }

    /** Compara etiquetas ignorando el prefijo "Iris-" y las mayúsculas. */
    private static int indiceDe(List<String> etiquetas, String valor) {
        if (valor == null) return -1;
        String v = normalizar(valor);
        for (int i = 0; i < etiquetas.size(); i++) {
            if (normalizar(etiquetas.get(i)).equals(v)) return i;
        }
        return -1;
    }

    private static String normalizar(String s) {
        String t = s.trim().toLowerCase();
        int guion = t.lastIndexOf('-');
        return guion >= 0 ? t.substring(guion + 1) : t;
    }

    /** Mínimo y máximo de una matriz, para escalar el color al dibujarla. */
    public static double[] rango(double[][] m) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double[] fila : m) {
            for (double v : fila) { min = Math.min(min, v); max = Math.max(max, v); }
        }
        return new double[]{min, max};
    }

    /**
     * Contiguas geométricamente en la rejilla.
     *
     * En la rectangular cuenta también la diagonal (distancia de Chebyshev 1),
     * porque con vecindad de 4 una segunda mejor neurona pegada en diagonal
     * contaría como fallo. En la hexagonal las seis vecinas ya son las de las
     * aristas, así que no hay diagonales que rescatar.
     */
    private static boolean sonContiguas(SOM som, SOMNeuron a, SOMNeuron b) {
        if (som.getTopology() == SOM.Topology.HEX) return sonVecinas(a, b);
        int[] pa = som.positionOf(a), pb = som.positionOf(b);
        if (pa == null || pb == null) return false;
        return Math.abs(pa[0] - pb[0]) <= 1 && Math.abs(pa[1] - pb[1]) <= 1;
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
        if (!som.esRejilla()) {
            throw new IllegalStateException(
                    "Estas lecturas necesitan una rejilla 2-D; el mapa es un anillo");
        }
    }
}
