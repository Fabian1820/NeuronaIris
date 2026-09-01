package panal.data;

import panal.RandomFeaturesPicker;
import panal.SOM;
import panal.utiles.BMUStock;
import panal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SOMAnalysisTest {

    private SOM entrenada(int filas, int columnas, int epocas) {
        RandomFeaturesPicker.setSeed(21);
        BMUStock.clear();
        SOM som = new SOM(epocas, filas, columnas, 0.5, 2, GestorTxt.getIrisDataset());
        som.initialize();
        som.train();
        return som;
    }

    @Test
    @DisplayName("La U-matrix tiene la forma de la rejilla y valores no negativos")
    void formaDeLaUMatrix() {
        SOM som = entrenada(8, 6, 30);
        double[][] u = SOMAnalysis.uMatrix(som);

        assertEquals(8, u.length);
        assertEquals(6, u[0].length);
        for (double[] fila : u) {
            for (double v : fila) assertTrue(v >= 0, "una distancia media no puede ser negativa");
        }
    }

    @Test
    @DisplayName("La U-matrix marca fronteras: su máximo supera claramente a su mínimo")
    void laUMatrixMarcaFronteras() {
        SOM som = entrenada(8, 6, 40);
        double[] r = SOMAnalysis.rango(SOMAnalysis.uMatrix(som));

        System.out.printf("U-matrix: min %.4f  max %.4f  ratio %.1fx%n", r[0], r[1], r[1] / r[0]);
        assertTrue(r[1] > r[0] * 2,
                "si el mapa se organizó, debe haber zonas de frontera bastante más altas que el interior");
    }

    @Test
    @DisplayName("Cada plano de componentes queda dentro del rango de su variable")
    void planosDeComponentes() {
        SOM som = entrenada(8, 6, 30);
        Dataset d = som.getDataset();

        for (int k = 0; k < d.dimension(); k++) {
            double[] r = SOMAnalysis.rango(SOMAnalysis.componentPlane(som, k));
            assertTrue(r[0] >= d.getMin()[k] - 1e-6 && r[1] <= d.getMax()[k] + 1e-6,
                    "el plano de la variable " + k + " se sale del rango de los datos");
        }
    }

    @Test
    @DisplayName("Pedir una variable que no existe se rechaza")
    void variableInexistente() {
        SOM som = entrenada(4, 4, 5);
        var e = assertThrows(IllegalArgumentException.class, () -> SOMAnalysis.componentPlane(som, 9));
        assertTrue(e.getMessage().contains("no existe"), e.getMessage());
    }

    @Test
    @DisplayName("El error topográfico de una rejilla entrenada debe ser bajo")
    void errorTopografico() {
        SOM som = entrenada(8, 6, 40);
        double et = SOMAnalysis.topographicError(som, som.getDataset().getSamples());

        System.out.printf("error topográfico: %.1f%%%n", et * 100);
        assertTrue(et >= 0 && et <= 1);
        assertTrue(et < 0.30,
                "si el mapa conserva la vecindad, la mayoría de muestras deben tener sus dos"
                        + " mejores neuronas contiguas; salió " + (et * 100) + "%");
    }

    @Test
    @DisplayName("Las etiquetas de la rejilla cubren las tres especies")
    void etiquetasDeLaRejilla() {
        SOM som = entrenada(8, 6, 40);
        String[][] g = SOMAnalysis.labelGrid(som);

        var vistas = new java.util.HashSet<String>();
        for (String[] fila : g) for (String e : fila) if (e != null) vistas.add(e);

        assertEquals(3, vistas.size(), "las tres especies deben aparecer en el mapa: " + vistas);
    }

    @Test
    @DisplayName("Sobre un anillo estas lecturas se rechazan con un mensaje claro")
    void noAplicaAlAnillo() {
        RandomFeaturesPicker.setSeed(21);
        BMUStock.clear();
        SOM anillo = new SOM(5, 20, 0.5, 2);
        anillo.initialize();

        var e = assertThrows(IllegalStateException.class, () -> SOMAnalysis.uMatrix(anillo));
        assertTrue(e.getMessage().contains("rejilla"), e.getMessage());
    }
}
