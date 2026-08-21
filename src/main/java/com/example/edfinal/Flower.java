package com.example.edfinal;

import com.example.edfinal.data.Sample;

/**
 * Una flor del dataset Iris: cuatro medidas con nombre.
 *
 * Es una vista cómoda sobre {@link Sample}, que es lo que el mapa entiende de
 * verdad. El núcleo trabaja con muestras de N variables; esta clase solo pone
 * nombres a los índices 0..3 para la interfaz y para el caso Iris.
 */
public class Flower extends Sample {

    public static final int SEPAL_LENGTH = 0;
    public static final int SEPAL_WIDTH  = 1;
    public static final int PETAL_LENGTH = 2;
    public static final int PETAL_WIDTH  = 3;

    /** Nombres de las variables, en el orden en que vienen en iris.data. */
    public static final String[] FEATURE_NAMES = {
            "largo sépalo", "ancho sépalo", "largo pétalo", "ancho pétalo"
    };

    public Flower(double sepalLength, double sepalWidth, double petalLength, double petalWidth) {
        this(sepalLength, sepalWidth, petalLength, petalWidth, (String) null);
    }

    public Flower(double sepalLength, double sepalWidth, double petalLength, double petalWidth, String type) {
        super(new double[]{sepalLength, sepalWidth, petalLength, petalWidth}, type);
    }

    /** Igual que el anterior pero validando que cada medida caiga en el rango del Iris. */
    public Flower(double sepalLength, double sepalWidth, double petalLength, double petalWidth, char e) {
        super(new double[]{sepalLength, sepalWidth, petalLength, petalWidth});
        comprobar(sepalLength, 4.3, 7.9, "sepal length");
        comprobar(sepalWidth, 2.0, 4.4, "sepal width");
        comprobar(petalLength, 1.0, 6.9, "petal length");
        comprobar(petalWidth, 0.1, 2.5, "petal width");
    }

    /** Flor aleatoria dentro del rango de cada medida (pesos iniciales de una neurona). */
    public Flower() {
        super(new double[]{
                RandomFeaturesPicker.randomSepalLength(),
                RandomFeaturesPicker.randomSepalWidth(),
                RandomFeaturesPicker.randomPetalLength(),
                RandomFeaturesPicker.randomPetalWidth()
        });
    }

    /** Envuelve una muestra de 4 variables para poder leerla con nombres. */
    public static Flower from(Sample s) {
        if (s instanceof Flower f) return f;
        if (s.size() != 4) {
            throw new IllegalArgumentException(
                    "Una flor tiene 4 medidas y la muestra trae " + s.size());
        }
        return new Flower(s.get(0), s.get(1), s.get(2), s.get(3), s.getLabel());
    }

    private static void comprobar(double valor, double min, double max, String nombre) {
        if (valor < min || valor > max) {
            throw new IllegalArgumentException(
                    "The " + nombre + " must be between " + min + " and " + max);
        }
    }

    public double getSepalLength() { return get(SEPAL_LENGTH); }
    public double getSepalWidth()  { return get(SEPAL_WIDTH); }
    public double getPetalLength() { return get(PETAL_LENGTH); }
    public double getPetalWidth()  { return get(PETAL_WIDTH); }

    public void setSepalLength(double v) { set(SEPAL_LENGTH, v); }
    public void setSepalWidth(double v)  { set(SEPAL_WIDTH, v); }
    public void setPetalLength(double v) { set(PETAL_LENGTH, v); }
    public void setPetalWidth(double v)  { set(PETAL_WIDTH, v); }

    public void setSepalLengthE(double v) { comprobar(v, 4.3, 7.9, "sepal length"); setSepalLength(v); }
    public void setSepalWidthE(double v)  { comprobar(v, 2.0, 4.4, "sepal width");  setSepalWidth(v); }
    public void setPetalLengthE(double v) { comprobar(v, 1.0, 6.9, "petal length"); setPetalLength(v); }
    public void setPetalWidthE(double v)  { comprobar(v, 0.1, 2.5, "petal width");  setPetalWidth(v); }

    public String getType() {
        return getLabel();
    }

    @Override
    public String toString() {
        return "Flower{" +
                "petalWidth=" + getPetalWidth() +
                ", petalLength=" + getPetalLength() +
                ", sepalWidth=" + getSepalWidth() +
                ", sepalLength=" + getSepalLength() +
                '}';
    }
}
