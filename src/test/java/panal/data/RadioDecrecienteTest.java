package panal.data;

import panal.RandomFeaturesPicker;
import panal.SOM;
import panal.utiles.BMUStock;
import panal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ¿Baja el error topográfico si el radio de vecindad se encoge con las épocas?
 * Se mide antes de decidir, sobre varias semillas.
 */
class RadioDecrecienteTest {

    private static final int SEMILLAS = 20;
    private static final boolean DIAGONALES = Boolean.getBoolean("et.diagonales");

    private record Medida(double topografico, double acierto, double cuantizacion) {}

    private Medida correr(long semilla, boolean encoger, int radio, int epocas) {
        RandomFeaturesPicker.setSeed(semilla);
        BMUStock.clear();

        SOM som = new SOM(epocas, 8, 6, 0.5, radio, GestorTxt.getIrisDataset());
        som.setShrinkRadius(encoger);
        som.initialize();
        som.train();

        List<Sample> datos = som.getDataset().getSamples();
        double et = SOMAnalysis.topographicError(som, datos, DIAGONALES);

        int aciertos = 0;
        double err = 0;
        for (Sample s : datos) {
            var bmu = som.findBMU(s);
            err += bmu.euclidianDistance(s);
            if (s.getLabel().toLowerCase().contains(som.classify(bmu))) aciertos++;
        }
        return new Medida(et * 100, aciertos * 100.0 / datos.size(), err / datos.size());
    }

    private Medida promedio(boolean encoger, int radio, int epocas) {
        double t = 0, a = 0, c = 0;
        for (long s = 1; s <= SEMILLAS; s++) {
            Medida m = correr(s, encoger, radio, epocas);
            t += m.topografico(); a += m.acierto(); c += m.cuantizacion();
        }
        return new Medida(t / SEMILLAS, a / SEMILLAS, c / SEMILLAS);
    }

    @Test
    @DisplayName("El radio decreciente va de r a 1 a lo largo del entrenamiento")
    void elRadioDecrece() {
        SOM som = new SOM(10, 8, 6, 0.5, 4, GestorTxt.getIrisDataset());
        som.setShrinkRadius(true);

        assertEquals(4, som.radiusAt(1), "en la primera época, el radio inicial");
        assertEquals(1, som.radiusAt(10), "en la última, la vecindad mínima");
        for (int e = 1; e < 10; e++) {
            assertTrue(som.radiusAt(e) >= som.radiusAt(e + 1), "no debe volver a crecer");
        }

        som.setShrinkRadius(false);
        assertEquals(4, som.radiusAt(10), "desactivado, el radio no cambia");
    }

    @Test
    @DisplayName("Comparativa radio fijo vs decreciente sobre 20 semillas")
    void comparativa() {
        System.out.println("=== rejilla 8x6, lr 0.5, media de " + SEMILLAS + " semillas ===");
        System.out.println("radio épocas  esquema      | err.topográfico | acierto | cuantización");

        for (int[] cfg : new int[][]{{2, 40}, {3, 40}, {4, 40}, {3, 80}}) {
            Medida fijo = promedio(false, cfg[0], cfg[1]);
            Medida decr = promedio(true, cfg[0], cfg[1]);

            System.out.printf("  %d     %3d   fijo         |     %5.1f%%      |  %5.1f%% |   %.4f%n",
                    cfg[0], cfg[1], fijo.topografico(), fijo.acierto(), fijo.cuantizacion());
            System.out.printf("  %d     %3d   decreciente  |     %5.1f%%      |  %5.1f%% |   %.4f   %s%n",
                    cfg[0], cfg[1], decr.topografico(), decr.acierto(), decr.cuantizacion(),
                    decr.topografico() < fijo.topografico() ? "<-- mejora" : "");
        }
    }
}
