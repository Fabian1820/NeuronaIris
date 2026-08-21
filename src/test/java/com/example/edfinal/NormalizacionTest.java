package com.example.edfinal;

import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ¿Cuánto pesa cada variable en la distancia euclidiana sin normalizar, y
 * cuánto se gana normalizando? Experimento, todavía no un cambio de producción.
 */
class NormalizacionTest {

    @Test
    @DisplayName("Contribución de cada variable a la distancia total")
    void contribucionDeCadaVariable() {
        List<Flower> db = GestorTxt.getDataBase();
        double[] suma = new double[4];
        int pares = 0;

        for (int i = 0; i < db.size(); i += 7) {
            for (int j = i + 1; j < db.size(); j += 11) {
                Flower a = db.get(i), b = db.get(j);
                suma[0] += Math.pow(a.getSepalLength() - b.getSepalLength(), 2);
                suma[1] += Math.pow(a.getSepalWidth() - b.getSepalWidth(), 2);
                suma[2] += Math.pow(a.getPetalLength() - b.getPetalLength(), 2);
                suma[3] += Math.pow(a.getPetalWidth() - b.getPetalWidth(), 2);
                pares++;
            }
        }

        double total = suma[0] + suma[1] + suma[2] + suma[3];
        String[] nombres = {"largo sépalo", "ancho sépalo", "largo pétalo", "ancho pétalo"};
        System.out.println("=== peso de cada variable en la distancia (sobre " + pares + " pares) ===");
        for (int k = 0; k < 4; k++) {
            System.out.printf("  %-14s %5.1f%%%n", nombres[k], suma[k] / total * 100);
        }
    }

    /** Copia del dataset escalada a [0,1] por variable. */
    private List<Flower> normalizado(List<Flower> db) {
        double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] max = new double[4];
        for (Flower f : db) {
            double[] v = {f.getSepalLength(), f.getSepalWidth(), f.getPetalLength(), f.getPetalWidth()};
            for (int k = 0; k < 4; k++) { min[k] = Math.min(min[k], v[k]); max[k] = Math.max(max[k], v[k]); }
        }
        List<Flower> out = new ArrayList<>();
        for (Flower f : db) {
            double[] v = {f.getSepalLength(), f.getSepalWidth(), f.getPetalLength(), f.getPetalWidth()};
            for (int k = 0; k < 4; k++) v[k] = (v[k] - min[k]) / (max[k] - min[k]);
            out.add(new Flower(v[0], v[1], v[2], v[3], f.getType()));
        }
        return out;
    }

    /**
     * Entrena a mano sobre una lista dada (SOM.train() usa siempre GestorTxt.getDataBase())
     * y devuelve el acierto por vecino más cercano etiquetado.
     */
    private double aciertoSobre(List<Flower> datos, long semilla, int epochs, int neuronas) {
        RandomFeaturesPicker.setSeed(semilla);
        BMUStock.clear();
        SOM som = new SOM(epochs, neuronas, 0.5, 2);
        som.initialize();

        // Reescala los pesos iniciales al mismo rango que los datos.
        double maxDato = 0;
        for (Flower f : datos) maxDato = Math.max(maxDato,
                Math.max(Math.max(f.getSepalLength(), f.getSepalWidth()),
                         Math.max(f.getPetalLength(), f.getPetalWidth())));
        for (var v : som.getVerticesList()) {
            Flower w = (Flower) ((SOMNeuron) v).getInfo();
            w.setSepalLength(w.getSepalLength() / 7.9 * maxDato);
            w.setSepalWidth(w.getSepalWidth() / 4.4 * maxDato);
            w.setPetalLength(w.getPetalLength() / 6.9 * maxDato);
            w.setPetalWidth(w.getPetalWidth() / 2.5 * maxDato);
        }

        for (int e = 1; e <= epochs; e++) {
            som.setCurrentLearningRate(som.learningRate(e));
            for (Flower f : datos) som.updateBmuAndAdjacents(som.findBMU(f), f, e);
        }

        // Etiqueta cada neurona por voto mayoritario de las muestras que gana.
        java.util.Map<Integer, java.util.Map<String, Integer>> votos = new java.util.HashMap<>();
        for (Flower f : datos) {
            int id = som.findBMU(f).getId();
            votos.computeIfAbsent(id, k -> new java.util.HashMap<>())
                 .merge(f.getType(), 1, Integer::sum);
        }
        int aciertos = 0;
        Set<Integer> ganadoras = new HashSet<>();
        for (Flower f : datos) {
            int id = som.findBMU(f).getId();
            ganadoras.add(id);
            String etiqueta = votos.get(id).entrySet().stream()
                    .max(java.util.Map.Entry.comparingByValue()).get().getKey();
            if (etiqueta.equals(f.getType())) aciertos++;
        }
        return aciertos * 100.0 / datos.size();
    }

    @Test
    @DisplayName("Acierto sin normalizar frente a normalizado (20 semillas)")
    void compararNormalizacion() {
        List<Flower> crudo = GestorTxt.getDataBase();
        List<Flower> norm = normalizado(crudo);

        double sumCrudo = 0, sumNorm = 0;
        int semillas = 20;
        for (long s = 1; s <= semillas; s++) {
            sumCrudo += aciertoSobre(crudo, s, 30, 30);
            sumNorm += aciertoSobre(norm, s, 30, 30);
        }
        System.out.printf("acierto sin normalizar : %.1f%%%n", sumCrudo / semillas);
        System.out.printf("acierto normalizado    : %.1f%%%n", sumNorm / semillas);
    }
}
