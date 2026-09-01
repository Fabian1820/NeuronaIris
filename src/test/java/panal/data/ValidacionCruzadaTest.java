package panal.data;

import panal.RandomFeaturesPicker;
import panal.SOM;
import panal.utiles.BMUStock;
import panal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validación cruzada estratificada: reparto correcto y acierto sobre datos no
 * vistos, con cada muestra evaluada exactamente una vez.
 */
class ValidacionCruzadaTest {

    private static final List<String> ESPECIES = List.of("setosa", "versicolor", "virginica");

    @Test
    @DisplayName("Cada muestra se usa exactamente una vez como prueba")
    void cadaMuestraSeEvaluaUnaVez() {
        Dataset iris = GestorTxt.getIrisDataset();
        Dataset[][] pliegues = iris.kFold(5, 1);

        assertEquals(5, pliegues.length);

        Set<Sample> vistas = new HashSet<>();
        int total = 0;
        for (Dataset[] par : pliegues) {
            for (Sample s : par[1].getSamples()) {
                assertTrue(vistas.add(s), "una muestra aparece en dos pliegues de prueba");
                total++;
            }
        }
        assertEquals(iris.size(), total, "todas las muestras deben evaluarse");
    }

    @Test
    @DisplayName("Entrenamiento y prueba no se solapan y suman el dataset entero")
    void sinSolapamiento() {
        Dataset iris = GestorTxt.getIrisDataset();

        for (Dataset[] par : iris.kFold(5, 2)) {
            Set<Sample> entrena = new HashSet<>(par[0].getSamples());
            for (Sample s : par[1].getSamples()) {
                assertFalse(entrena.contains(s), "una muestra de prueba está en el entrenamiento");
            }
            assertEquals(iris.size(), par[0].size() + par[1].size());
        }
    }

    @Test
    @DisplayName("Cada pliegue conserva la proporción de cada especie")
    void reparteEstratificado() {
        Dataset[][] pliegues = GestorTxt.getIrisDataset().kFold(5, 3);

        for (Dataset[] par : pliegues) {
            assertEquals(30, par[1].size(), "150 muestras entre 5 pliegues");
            var cuenta = new java.util.HashMap<String, Integer>();
            for (Sample s : par[1].getSamples()) cuenta.merge(s.getLabel(), 1, Integer::sum);

            assertEquals(3, cuenta.size(), "las tres especies en cada pliegue");
            for (var e : cuenta.entrySet()) {
                assertEquals(10, e.getValue(), e.getKey() + " debería aportar 10 por pliegue");
            }
        }
    }

    @Test
    @DisplayName("Pedir menos de 2 pliegues, o más que la clase menos frecuente, se rechaza")
    void parametrosInvalidos() {
        Dataset iris = GestorTxt.getIrisDataset();

        var e1 = assertThrows(IllegalArgumentException.class, () -> iris.kFold(1, 1));
        assertTrue(e1.getMessage().contains("2 pliegues"), e1.getMessage());

        var e2 = assertThrows(IllegalArgumentException.class, () -> iris.kFold(80, 1));
        assertTrue(e2.getMessage().contains("menos"), e2.getMessage());
    }

    @Test
    @DisplayName("Acierto por validación cruzada de 5 pliegues, con su dispersión")
    void aciertoValidadoCruzado() {
        Dataset iris = GestorTxt.getIrisDataset();
        int[][] acumulada = new int[3][3];
        List<Double> porPliegue = new ArrayList<>();

        int repeticiones = 5;
        for (long semilla = 1; semilla <= repeticiones; semilla++) {
            for (Dataset[] par : iris.kFold(5, semilla)) {
                RandomFeaturesPicker.setSeed(semilla);
                BMUStock.clear();

                SOM som = new SOM(40, 8, 6, 0.5, 2, par[0], SOM.Topology.HEX);
                som.initialize();
                som.train();

                int[][] c = SOMAnalysis.confusionMatrix(som, par[1].getSamples(), ESPECIES);
                porPliegue.add(SOMAnalysis.accuracy(c));
                for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) acumulada[i][j] += c[i][j];
            }
        }

        double media = porPliegue.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double varianza = porPliegue.stream()
                .mapToDouble(a -> (a - media) * (a - media)).average().orElse(0);
        double desviacion = Math.sqrt(varianza);

        System.out.printf("=== validación cruzada 5 pliegues x %d repeticiones (%d evaluaciones) ===%n",
                repeticiones, porPliegue.size());
        System.out.printf("acierto: %.1f%% ± %.1f   (min %.1f%%, max %.1f%%)%n", media, desviacion,
                porPliegue.stream().mapToDouble(Double::doubleValue).min().orElse(0),
                porPliegue.stream().mapToDouble(Double::doubleValue).max().orElse(0));
        System.out.println("matriz de confusión acumulada (fila = real, columna = predicha)");
        System.out.printf("%-12s %10s %10s %10s%n", "", "setosa", "versicolor", "virginica");
        for (int i = 0; i < 3; i++) {
            System.out.printf("%-12s %10d %10d %10d%n",
                    ESPECIES.get(i), acumulada[i][0], acumulada[i][1], acumulada[i][2]);
        }

        assertEquals(iris.size() * repeticiones,
                java.util.Arrays.stream(acumulada).flatMapToInt(java.util.Arrays::stream).sum(),
                "cada muestra debe evaluarse una vez por repetición");
        assertTrue(media > 85.0, "acierto validado: " + media);
    }
}
