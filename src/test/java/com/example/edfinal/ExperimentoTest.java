package com.example.edfinal;

import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mide sobre muchas semillas en vez de sobre una corrida suelta: con
 * inicialización aleatoria, un único experimento no dice nada.
 */
class ExperimentoTest {

    private static final int SEMILLAS = 20;

    private record Resultado(double error, double muertas, double acierto) {}

    private Resultado unaCorrida(long semilla, int epochs, int neuronas, double lr, int radio) {
        RandomFeaturesPicker.setSeed(semilla);
        BMUStock.clear();

        SOM som = new SOM(epochs, neuronas, lr, radio);
        som.initialize();
        som.train();

        List<Flower> db = GestorTxt.getDataBase();

        double err = 0;
        Set<Integer> ganadoras = new HashSet<>();
        int aciertos = 0;
        for (Flower f : db) {
            SOMNeuron bmu = som.findBMU(f);
            err += bmu.euclidianDistance(f);
            ganadoras.add(bmu.getId());
            String predicha = som.classify(bmu);
            if (f.getType() != null && f.getType().toLowerCase().contains(predicha)) aciertos++;
        }
        return new Resultado(err / db.size(),
                som.getVerticesList().size() - ganadoras.size(),
                aciertos * 100.0 / db.size());
    }

    @Test
    @DisplayName("Informe sobre 20 semillas: media y dispersión")
    void informeSobreVariasSemillas() {
        int[][] configs = {{10, 30}, {30, 30}, {50, 30}, {30, 60}};

        System.out.println("=== media sobre " + SEMILLAS + " semillas ===");
        System.out.println("épocas neuronas |  error (min-max)      | muertas | acierto (min-max)");

        for (int[] c : configs) {
            double sumErr = 0, sumMuertas = 0, sumAcc = 0;
            double minErr = Double.MAX_VALUE, maxErr = 0, minAcc = 100, maxAcc = 0;

            for (long s = 1; s <= SEMILLAS; s++) {
                Resultado r = unaCorrida(s, c[0], c[1], 0.5, 2);
                sumErr += r.error(); sumMuertas += r.muertas(); sumAcc += r.acierto();
                minErr = Math.min(minErr, r.error()); maxErr = Math.max(maxErr, r.error());
                minAcc = Math.min(minAcc, r.acierto()); maxAcc = Math.max(maxAcc, r.acierto());
            }

            System.out.printf("  %3d    %3d     | %.4f (%.4f-%.4f) | %4.1f/%d | %.1f%% (%.1f-%.1f)%n",
                    c[0], c[1], sumErr / SEMILLAS, minErr, maxErr,
                    sumMuertas / SEMILLAS, c[1], sumAcc / SEMILLAS, minAcc, maxAcc);
        }
    }

    @Test
    @DisplayName("¿Se asienta el mapa al final del entrenamiento?")
    void elMapaDebeAsentarse() {
        int epochs = 50;
        RandomFeaturesPicker.setSeed(7);
        BMUStock.clear();
        SOM som = new SOM(epochs, 30, 0.5, 2);
        som.initialize();
        som.train();

        // Foto de los pesos, una época más, y cuánto se movieron.
        double[] antes = pesos(som);
        som.train2(GestorTxt.getDataBase(), epochs);
        double[] despues = pesos(som);

        double desplazamiento = 0;
        for (int i = 0; i < antes.length; i++) desplazamiento += Math.abs(antes[i] - despues[i]);
        desplazamiento /= antes.length;

        System.out.printf("tasa de aprendizaje final : %.4f (inicial 0.5)%n", som.learningRate(epochs));
        System.out.printf("movimiento medio por peso en una época extra: %.4f%n", desplazamiento);

        assertTrue(desplazamiento < 0.05,
                "tras entrenar, una época más apenas debería mover el mapa; se movió " + desplazamiento);
    }

    private double[] pesos(SOM som) {
        double[] w = new double[som.getVerticesList().size() * 4];
        int i = 0;
        for (var v : som.getVerticesList()) {
            Flower f = (Flower) ((SOMNeuron) v).getInfo();
            w[i++] = f.getSepalLength(); w[i++] = f.getSepalWidth();
            w[i++] = f.getPetalLength(); w[i++] = f.getPetalWidth();
        }
        return w;
    }

    @Test
    @DisplayName("Con la misma semilla el resultado debe repetirse")
    void mismaSemillaMismoResultado() {
        Resultado a = unaCorrida(42, 20, 30, 0.5, 2);
        Resultado b = unaCorrida(42, 20, 30, 0.5, 2);

        assertTrue(Math.abs(a.error() - b.error()) < 1e-9,
                "el experimento debe ser reproducible: " + a.error() + " vs " + b.error());
        assertTrue(Math.abs(a.acierto() - b.acierto()) < 1e-9);
    }
}
