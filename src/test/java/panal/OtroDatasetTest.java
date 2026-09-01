package panal;

import panal.ui.PanelDispersion;
import panal.data.Dataset;
import panal.data.Sample;
import panal.data.SOMAnalysis;
import panal.utiles.BMUStock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Recorre con un dataset que no es el Iris el mismo camino que hace la interfaz:
 * cargar un CSV, montar el mapa, entrenar, clasificar y leer la U-matrix.
 */
class OtroDatasetTest {

    private static final String RUTA = "docs/ejemplo-3variables.csv";

    @Test
    @DisplayName("El dataset de ejemplo se carga con 3 variables y 3 etiquetas")
    void seCarga() throws IOException {
        Dataset d = Dataset.fromCsv(RUTA);
        assertEquals(120, d.size());
        assertEquals(3, d.dimension());
        assertEquals(3, d.labels().size());
        assertEquals(3, d.getFeatureNames().length, "sin cabecera, los nombres se generan solos");
    }

    @Test
    @DisplayName("La interfaz sabe repartir las gráficas de un dataset de 3 variables")
    void paresDeEjesParaTresVariables() throws IOException {
        Dataset d = Dataset.fromCsv(RUTA);
        int[][] pares = PanelDispersion.paresDeVariables(d.dimension());

        assertEquals(4, pares.length);
        for (int[] p : pares) {
            assertTrue(p[0] < d.dimension() && p[1] < d.dimension(),
                    "un eje se sale del dataset: " + p[0] + "," + p[1]);
        }
    }

    @Test
    @DisplayName("Entrena, clasifica y da U-matrix sobre el dataset de ejemplo")
    void cicloCompleto() throws IOException {
        Dataset d = Dataset.fromCsv(RUTA);

        RandomFeaturesPicker.setSeed(4);
        BMUStock.clear();

        int[] rejilla = SOM.rejillaPara(30);
        SOM som = new SOM(40, rejilla[0], rejilla[1], 0.5, 2, d);
        som.initialize();
        som.train();

        int aciertos = 0;
        for (Sample s : d.getSamples()) {
            if (s.getLabel().equals(som.classify(som.findBMU(s)))) aciertos++;
        }
        double acierto = aciertos * 100.0 / d.size();
        System.out.printf("dataset de ejemplo (3 variables) -> acierto %.1f%%%n", acierto);
        assertTrue(acierto > 95.0, "grupos bien separados deberían clasificarse casi perfecto: " + acierto);

        double[] r = SOMAnalysis.rango(SOMAnalysis.uMatrix(som));
        assertTrue(r[1] > r[0], "la U-matrix debe tener relieve");

        assertEquals(3, BMUStock.labels().size(), "un grupo por etiqueta del CSV");
    }

    @Test
    @DisplayName("Un fichero con otra dimensión no se mezcla con el mapa actual")
    void dimensionesIncompatibles() throws IOException {
        Dataset tres = Dataset.fromCsv(RUTA);
        Dataset iris = panal.utiles.GestorTxt.getIrisDataset();

        assertNotEquals(tres.dimension(), iris.dimension(),
                "este test necesita dos datasets de distinta dimensión");

        RandomFeaturesPicker.setSeed(4);
        BMUStock.clear();
        SOM som = new SOM(5, 4, 5, 0.5, 1, tres);
        som.initialize();

        // Buscar la BMU de una muestra de otra dimensión debe fallar con un mensaje claro.
        var e = assertThrows(IllegalArgumentException.class,
                () -> som.findBMU(iris.getSamples().get(0)));
        assertTrue(e.getMessage().contains("dimensión"), e.getMessage());
    }
}
