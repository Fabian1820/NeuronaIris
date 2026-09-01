package panal.data;

import panal.RandomFeaturesPicker;
import panal.SOM;
import panal.SOMNeuron;
import panal.utiles.BMUStock;
import panal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ¿Compensa la rejilla hexagonal? Se mide contra la rectangular sobre las
 * mismas semillas, en vez de darlo por hecho porque "es lo canónico".
 */
class HexVsRectTest {

    private static final int SEMILLAS = 20;
    /** true = cuentan las celdas que se tocan (8 en rect, 6 en hex); false = solo aristas del grafo. */
    private static final boolean DIAGONALES = !Boolean.getBoolean("et.estricto");

    private record Medida(double topografico, double acierto, double cuantizacion, double muertas) {}

    private Medida correr(long semilla, SOM.Topology t, int epocas, int radio) {
        RandomFeaturesPicker.setSeed(semilla);
        BMUStock.clear();

        SOM som = new SOM(epocas, 8, 6, 0.5, radio, GestorTxt.getIrisDataset(), t);
        som.initialize();
        som.train();

        List<Sample> datos = som.getDataset().getSamples();
        double et = SOMAnalysis.topographicError(som, datos, DIAGONALES) * 100;

        int aciertos = 0;
        double err = 0;
        Set<Integer> ganadoras = new HashSet<>();
        for (Sample s : datos) {
            SOMNeuron bmu = som.findBMU(s);
            err += bmu.euclidianDistance(s);
            ganadoras.add(bmu.getId());
            if (s.getLabel().toLowerCase().contains(som.classify(bmu))) aciertos++;
        }
        return new Medida(et, aciertos * 100.0 / datos.size(), err / datos.size(),
                som.getVerticesList().size() - ganadoras.size());
    }

    private Medida promedio(SOM.Topology t, int epocas, int radio) {
        double a = 0, b = 0, c = 0, d = 0;
        for (long s = 1; s <= SEMILLAS; s++) {
            Medida m = correr(s, t, epocas, radio);
            a += m.topografico(); b += m.acierto(); c += m.cuantizacion(); d += m.muertas();
        }
        return new Medida(a / SEMILLAS, b / SEMILLAS, c / SEMILLAS, d / SEMILLAS);
    }

    @Test
    @DisplayName("Comparativa hexagonal vs rectangular sobre 20 semillas")
    void comparativa() {
        System.out.println("=== rejilla 8x6, lr 0.5, media de " + SEMILLAS + " semillas ===");
        System.out.println("épocas radio  topología    | err.topográfico | acierto | cuantización | muertas");

        for (int[] cfg : new int[][]{{20, 2}, {40, 2}, {40, 3}}) {
            Medida rect = promedio(SOM.Topology.GRID, cfg[0], cfg[1]);
            Medida hex = promedio(SOM.Topology.HEX, cfg[0], cfg[1]);

            System.out.printf("  %3d    %d    rectangular  |     %5.1f%%      |  %5.1f%% |    %.4f    |  %.1f/48%n",
                    cfg[0], cfg[1], rect.topografico(), rect.acierto(), rect.cuantizacion(), rect.muertas());
            System.out.printf("  %3d    %d    hexagonal    |     %5.1f%%      |  %5.1f%% |    %.4f    |  %.1f/48%n",
                    cfg[0], cfg[1], hex.topografico(), hex.acierto(), hex.cuantizacion(), hex.muertas());
        }
    }

    @Test
    @DisplayName("La hexagonal clasifica el Iris tan bien como la rectangular")
    void noEmpeora() {
        Medida rect = promedio(SOM.Topology.GRID, 40, 2);
        Medida hex = promedio(SOM.Topology.HEX, 40, 2);

        assertTrue(hex.acierto() > rect.acierto() - 2.0,
                "la hexagonal no debería perder acierto: hex " + hex.acierto()
                        + " vs rect " + rect.acierto());
    }
}
