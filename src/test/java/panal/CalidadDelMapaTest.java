package panal;

import panal.utiles.BMUStock;
import panal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mide la calidad real del mapa, no solo que el código corra:
 * error de cuantización, neuronas muertas y acierto de clasificación.
 */
class CalidadDelMapaTest {

    @BeforeEach
    void limpiar() {
        BMUStock.clear();
    }

    private SOM entrenar(int epochs, int neuronas, double lr, int radio) {
        SOM som = new SOM(epochs, neuronas, lr, radio);
        som.initialize();
        som.train();
        return som;
    }

    /** Distancia media de cada muestra a su BMU. Cuanto menor, mejor representa el mapa a los datos. */
    private double errorDeCuantizacion(SOM som, List<Flower> db) {
        double total = 0;
        for (Flower f : db) total += som.findBMU(f).euclidianDistance(f);
        return total / db.size();
    }

    /** Neuronas que no ganan ninguna muestra: capacidad desperdiciada. */
    private int neuronasMuertas(SOM som, List<Flower> db) {
        Set<Integer> ganadoras = new HashSet<>();
        for (Flower f : db) ganadoras.add(som.findBMU(f).getId());
        return som.getVerticesList().size() - ganadoras.size();
    }

    @Test
    @DisplayName("Los pesos iniciales deberían caer dentro del rango de los datos")
    void pesosInicialesDentroDelRangoDeLosDatos() {
        SOM som = new SOM(1, 30, 0.5, 2);
        som.initialize();

        double maxVisto = 0;
        for (Vertex v : som.getVerticesList()) {
            Flower w = (Flower) ((SOMNeuron) v).getInfo();
            maxVisto = Math.max(maxVisto, Math.max(
                    Math.max(w.getSepalLength(), w.getSepalWidth()),
                    Math.max(w.getPetalLength(), w.getPetalWidth())));
        }

        // El máximo de cualquier medida del dataset Iris es 7.9 cm.
        assertTrue(maxVisto <= 10.0,
                "las neuronas nacen fuera del espacio de datos: valor máximo " + maxVisto
                        + " (el dataset no pasa de 7.9)");
    }

    @Test
    @DisplayName("Informe de calidad del mapa entrenado")
    void informeDeCalidad() {
        ArrayList<Flower> db = GestorTxt.getDataBase();
        SOM som = entrenar(30, 30, 0.5, 2);

        double eq = errorDeCuantizacion(som, db);
        int muertas = neuronasMuertas(som, db);

        System.out.println("=== calidad del mapa (30 épocas, 30 neuronas, lr=0.5, radio=2) ===");
        System.out.printf("error de cuantización : %.4f%n", eq);
        System.out.printf("neuronas muertas      : %d de %d%n", muertas, som.getVerticesList().size());

        for (int ep : new int[]{5, 10, 20}) {
            BMUStock.clear();
            SOM s = entrenar(ep, 30, 0.5, 2);
            System.out.printf("  %2d épocas -> error %.4f | muertas %d/30%n",
                    ep, errorDeCuantizacion(s, db), neuronasMuertas(s, db));
        }

        assertTrue(eq < 1.0,
                "el error de cuantización debería ser pequeño frente a la escala del dataset"
                        + " (0.1-7.9 cm), y es " + eq);
    }

    @Test
    @DisplayName("La tasa de acierto sobre el propio dataset debería ser alta")
    void tasaDeAcierto() {
        ArrayList<Flower> db = GestorTxt.getDataBase();
        SOM som = entrenar(30, 30, 0.5, 2);

        int aciertos = 0;
        for (Flower f : db) {
            String predicha = som.classify(som.findBMU(f));
            if (f.getType() != null && f.getType().toLowerCase().contains(predicha)) aciertos++;
        }
        double acierto = aciertos * 100.0 / db.size();
        System.out.printf("acierto sobre el dataset de entrenamiento: %.1f%%%n", acierto);

        assertTrue(acierto > 80.0,
                "un SOM sobre Iris debería superar holgadamente el 80% y da " + acierto + "%");
    }
}
