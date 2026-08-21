package com.example.edfinal.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

/**
 * Una muestra de N variables numéricas con una etiqueta opcional.
 *
 * Es la unidad con la que trabaja el mapa: tanto los datos de entrada como los
 * pesos de cada neurona son muestras. Sustituye al antiguo Flower de cuatro
 * campos fijos, que ataba todo el proyecto al dataset Iris.
 */
public class Sample implements Serializable {

    private static final long serialVersionUID = 2L;

    private final double[] features;
    private String label;

    public Sample(double[] features, String label) {
        this.features = Arrays.copyOf(features, features.length);
        this.label = label;
    }

    public Sample(double[] features) {
        this(features, null);
    }

    /** Copia independiente: los pesos de una neurona no deben compartir array con un dato. */
    public Sample copy() {
        return new Sample(this.features, this.label);
    }

    /** Muestra aleatoria dentro del rango de cada variable. */
    public static Sample random(double[] min, double[] max, Random rand) {
        double[] v = new double[min.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = min[i] + (max[i] - min[i]) * rand.nextDouble();
        }
        return new Sample(v);
    }

    public int size() {
        return features.length;
    }

    public double get(int i) {
        return features[i];
    }

    public void set(int i, double value) {
        features[i] = value;
    }

    public double[] toArray() {
        return Arrays.copyOf(features, features.length);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /** Distancia euclidiana sobre todas las variables. */
    public double distanceTo(Sample other) {
        if (other.size() != this.size()) {
            throw new IllegalArgumentException(
                    "No se pueden comparar muestras de distinta dimensión: "
                            + this.size() + " vs " + other.size());
        }
        double suma = 0;
        for (int i = 0; i < features.length; i++) {
            double d = features[i] - other.features[i];
            suma += d * d;
        }
        return Math.sqrt(suma);
    }

    /**
     * Acerca esta muestra (los pesos de una neurona) hacia otra:
     * w += influencia * tasaDeAprendizaje * (x - w)
     */
    public void moveToward(Sample target, double influenceRate, double learningRate) {
        for (int i = 0; i < features.length; i++) {
            features[i] += influenceRate * learningRate * (target.features[i] - features[i]);
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(features) + (label == null ? "" : " -> " + label);
    }
}
