package panal.data;

import panal.RandomFeaturesPicker;
import panal.SOM;
import panal.utiles.BMUStock;
import panal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Barrido de hiperparámetros y su sesgo de selección. */
class BusquedaTest {

    private static final List<String> ESPECIES = List.of("setosa", "versicolor", "virginica");

    private List<BusquedaHiperparametros.Config> parrillaCorta() {
        return BusquedaHiperparametros.parrilla(
                List.of(20, 40),
                List.of(24, 48),
                List.of(1, 2),
                List.of(0.5),
                List.of(SOM.Topology.HEX));
    }

    @Test
    @DisplayName("La parrilla es el producto cartesiano y descarta lo imposible")
    void parrillaCorrecta() {
        assertEquals(8, parrillaCorta().size(), "2 épocas x 2 neuronas x 2 radios");

        // Una rejilla no puede tener 2 neuronas: esas combinaciones se descartan.
        var conPocas = BusquedaHiperparametros.parrilla(
                List.of(10), List.of(2, 48), List.of(1), List.of(0.5),
                List.of(SOM.Topology.GRID, SOM.Topology.RING));
        assertEquals(3, conPocas.size(), "GRID descarta n=2; RING admite las dos");
    }

    @Test
    @DisplayName("El barrido devuelve las combinaciones ordenadas por acierto")
    void ordenadoPorAcierto() {
        var res = BusquedaHiperparametros.buscar(
                GestorTxt.getIrisDataset(), parrillaCorta(), 5, 1, null);

        assertEquals(parrillaCorta().size(), res.size(), "una entrada por combinación");
        for (int i = 1; i < res.size(); i++) {
            assertTrue(res.get(i - 1).acierto() >= res.get(i).acierto(),
                    "no está ordenado en la posición " + i);
        }
        assertTrue(res.get(0).acierto() > 80, "la mejor debería ser razonable");
    }

    @Test
    @DisplayName("Con la misma semilla el barrido da el mismo ganador")
    void reproducible() {
        var a = BusquedaHiperparametros.buscar(GestorTxt.getIrisDataset(), parrillaCorta(), 5, 7, null);
        var b = BusquedaHiperparametros.buscar(GestorTxt.getIrisDataset(), parrillaCorta(), 5, 7, null);

        assertEquals(a.get(0).config(), b.get(0).config());
        assertEquals(a.get(0).acierto(), b.get(0).acierto(), 1e-9);
    }

    @Test
    @DisplayName("Avisa del progreso una vez por combinación")
    void informaDelProgreso() {
        List<BusquedaHiperparametros.Resultado> vistos = new ArrayList<>();
        BusquedaHiperparametros.buscar(GestorTxt.getIrisDataset(), parrillaCorta(), 5, 1, vistos::add);
        assertEquals(parrillaCorta().size(), vistos.size());
    }

    @Test
    @DisplayName("Una parrilla vacía se rechaza")
    void parrillaVacia() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> BusquedaHiperparametros.buscar(GestorTxt.getIrisDataset(), List.of(), 5, 1, null));
        assertTrue(e.getMessage().contains("combinación"), e.getMessage());
    }

    @Test
    @DisplayName("Cuánto optimismo mete elegir por validación cruzada (evaluación anidada)")
    void sesgoDeSeleccion() {
        Dataset iris = GestorTxt.getIrisDataset();
        var parrilla = parrillaCorta();

        List<Double> interno = new ArrayList<>();   // lo que prometía el barrido
        List<Double> externo = new ArrayList<>();   // lo que rinde en datos que el barrido no vio

        // Bucle externo: se aparta un pliegue, se busca en el resto, se evalúa en el apartado.
        int k = 5;
        for (Dataset[] par : iris.kFold(k, 42)) {
            var mejor = BusquedaHiperparametros.buscar(par[0], parrilla, 4, 42, null).get(0);
            interno.add(mejor.acierto());

            RandomFeaturesPicker.setSeed(42);
            BMUStock.clear();
            SOM som = BusquedaHiperparametros.construir(mejor.config(), par[0]);
            som.initialize();
            som.train();
            externo.add(SOMAnalysis.accuracy(
                    SOMAnalysis.confusionMatrix(som, par[1].getSamples(), ESPECIES)));
        }

        double mediaInterna = interno.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double mediaExterna = externo.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        System.out.printf("=== sesgo de selección (anidada, %d pliegues externos) ===%n", k);
        System.out.printf("acierto que promete el barrido : %.1f%%%n", mediaInterna);
        System.out.printf("acierto en datos no usados     : %.1f%%%n", mediaExterna);
        System.out.printf("optimismo                      : %.1f puntos%n", mediaInterna - mediaExterna);

        assertTrue(mediaExterna > 80, "aun con sesgo debería generalizar: " + mediaExterna);
    }

    @Test
    @DisplayName("Barrido completo sobre el Iris: cuál gana")
    void barridoCompleto() {
        var res = BusquedaHiperparametros.buscar(GestorTxt.getIrisDataset(),
                BusquedaHiperparametros.parrillaPorDefecto(), 5, 1, null);

        System.out.println("=== mejores 5 de " + res.size() + " combinaciones (validación cruzada 5 pliegues) ===");
        for (int i = 0; i < 5; i++) {
            var r = res.get(i);
            System.out.printf("  %.1f%% ± %.1f  | cuant %.3f | topo %.1f%% | %s%n",
                    r.acierto(), r.desviacion(), r.cuantizacion(), r.topografico(), r.config());
        }
        var peor = res.get(res.size() - 1);
        System.out.printf("  peor: %.1f%% | %s%n", peor.acierto(), peor.config());

        assertTrue(res.get(0).acierto() >= peor.acierto());
    }
}
