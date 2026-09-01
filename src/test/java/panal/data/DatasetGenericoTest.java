package panal.data;

import panal.RandomFeaturesPicker;
import panal.SOM;
import panal.SOMNeuron;
import panal.utiles.BMUStock;
import panal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El mapa ya no está atado al Iris: debe entrenar y clasificar sobre cualquier
 * dataset numérico, con cualquier número de variables y cualquier etiqueta.
 */
class DatasetGenericoTest {

    /** Tres grupos bien separados en 6 dimensiones. */
    private Dataset datasetSintetico(long semilla) {
        Random r = new Random(semilla);
        List<Sample> muestras = new ArrayList<>();
        String[] etiquetas = {"rojo", "verde", "azul"};
        double[] centros = {0.0, 10.0, 20.0};

        for (int g = 0; g < 3; g++) {
            for (int n = 0; n < 40; n++) {
                double[] v = new double[6];
                for (int i = 0; i < 6; i++) v[i] = centros[g] + r.nextGaussian();
                muestras.add(new Sample(v, etiquetas[g]));
            }
        }
        return new Dataset(muestras, null);
    }

    @Test
    @DisplayName("Un CSV cualquiera se carga con sus variables y su etiqueta")
    void cargaUnCsvCualquiera() throws IOException {
        String csv = """
                1.0,2.0,3.0,alfa
                1.1,2.1,3.1,alfa
                9.0,8.0,7.0,beta
                9.1,8.1,7.1,beta
                """;
        Dataset d = Dataset.fromReader(new StringReader(csv), null);

        assertEquals(4, d.size());
        assertEquals(3, d.dimension(), "tres columnas numéricas");
        assertEquals(2, d.labels().size());
        assertTrue(d.labels().contains("alfa"));
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, d.getMin(), 1e-9);
        assertArrayEquals(new double[]{9.1, 8.1, 7.1}, d.getMax(), 1e-9);
    }

    @Test
    @DisplayName("El mapa entrena y clasifica sobre 6 variables y etiquetas propias")
    void entrenaSobreSeisVariables() {
        RandomFeaturesPicker.setSeed(3);
        BMUStock.clear();

        Dataset d = datasetSintetico(3);
        SOM som = new SOM(30, 20, 0.5, 2, d);
        som.initialize();
        som.train();

        assertTrue(som.isTrained());

        int aciertos = 0;
        for (Sample s : d.getSamples()) {
            SOMNeuron bmu = som.findBMU(s);
            assertEquals(6, bmu.getWeights().size(), "los pesos viven en el mismo espacio que los datos");
            if (s.getLabel().equals(som.classify(bmu))) aciertos++;
        }
        double acierto = aciertos * 100.0 / d.size();
        System.out.printf("dataset sintético 6D, 3 grupos -> acierto %.1f%%%n", acierto);

        assertTrue(acierto > 95.0,
                "con grupos bien separados el mapa debería acertar casi todo, y da " + acierto + "%");
    }

    @Test
    @DisplayName("Agrupar ya no depende de que sean 150 muestras ordenadas 50/50/50")
    void agrupaPorEtiquetaNoPorIndice() {
        RandomFeaturesPicker.setSeed(11);
        BMUStock.clear();

        Dataset d = datasetSintetico(11);
        SOM som = new SOM(20, 15, 0.5, 2, d);
        som.initialize();
        som.train();

        assertEquals(3, BMUStock.labels().size(), "un grupo por etiqueta del dataset");
        assertTrue(BMUStock.labels().containsAll(List.of("rojo", "verde", "azul")));

        int total = 0;
        for (String etiqueta : BMUStock.labels()) total += BMUStock.forLabel(etiqueta).size();
        assertEquals(d.size(), total, "cada muestra aporta exactamente una BMU");
    }

    @Test
    @DisplayName("El Iris sigue funcionando igual a través del camino genérico")
    void elIrisNoSeRompe() {
        RandomFeaturesPicker.setSeed(5);
        BMUStock.clear();

        Dataset iris = GestorTxt.getIrisDataset();
        assertEquals(150, iris.size());
        assertEquals(4, iris.dimension());
        assertEquals(3, iris.labels().size());

        SOM som = new SOM(30, 30, 0.5, 2, iris);
        som.initialize();
        som.train();

        int aciertos = 0;
        for (Sample s : iris.getSamples()) {
            String esperada = s.getLabel().toLowerCase();
            if (esperada.contains(som.classify(som.findBMU(s)))) aciertos++;
        }
        double acierto = aciertos * 100.0 / iris.size();
        System.out.printf("iris por el camino genérico -> acierto %.1f%%%n", acierto);

        assertTrue(acierto > 90.0, "el Iris no debe empeorar con el refactor: " + acierto + "%");
    }

    @Test
    @DisplayName("El dataset viaja en el classpath, no en una ruta del disco")
    void elDatasetEstaEnElClasspath() {
        assertNotNull(GestorTxt.class.getResourceAsStream("/panal/iris.data"),
                "el Iris debe ir dentro del jar para que la app sea distribuible");
        assertEquals(150, GestorTxt.getIrisDataset().size());
    }

    @Test
    @DisplayName("El estado de la app se guarda en la carpeta del usuario")
    void elEstadoViveEnLaCarpetaDelUsuario() {
        String ruta = GestorTxt.archivoDeEstado("Map.dat");
        assertTrue(ruta.startsWith(System.getProperty("user.home")),
                "no debe escribirse en el directorio de trabajo: " + ruta);
        assertTrue(ruta.endsWith("Map.dat"));
        assertTrue(GestorTxt.carpetaDeEstado().isDirectory());
    }

    @Test
    @DisplayName("Normalizar deja cada variable en [0,1]")
    void normalizarEscalaTodasLasVariables() {
        Dataset norm = datasetSintetico(7).normalized();
        for (double v : norm.getMin()) assertEquals(0.0, v, 1e-9);
        for (double v : norm.getMax()) assertEquals(1.0, v, 1e-9);
    }

    @Test
    @DisplayName("Mezclar muestras de distinta dimensión se rechaza con un mensaje claro")
    void rechazaDimensionesMezcladas() {
        List<Sample> mezcla = List.of(
                new Sample(new double[]{1, 2, 3}, "a"),
                new Sample(new double[]{1, 2}, "b"));

        var e = assertThrows(IllegalArgumentException.class, () -> new Dataset(mezcla, null));
        assertTrue(e.getMessage().contains("dimensión"), e.getMessage());
    }
}
