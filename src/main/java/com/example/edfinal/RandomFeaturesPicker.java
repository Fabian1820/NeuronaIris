package com.example.edfinal;

import java.util.Random;

/**
 * Pesos iniciales de las neuronas, sorteados dentro del rango real de cada
 * medida del dataset Iris. Si el mapa nace fuera del espacio de datos, gasta
 * épocas solo en viajar hasta él y deja neuronas muertas por el camino.
 */
public class RandomFeaturesPicker {
//                  Min  Max
//    sepal length: 4.3  7.9
//    sepal width:  2.0  4.4
//    petal length: 1.0  6.9
//    petal width:  0.1  2.5

    private static double sepalLengthMin = 4.3;
    private static double sepalLengthMax = 7.9;
    private static double sepalWidthMin = 2.0;
    private static double sepalWidthMax = 4.4;

    private static double petalLengthMin = 1.0;
    private static double petalLengthMax = 6.9;
    private static double petalWidthMin = 0.1;
    private static double petalWidthMax = 2.5;

    private static Random rand = new Random();

    /** Fija la semilla para que un experimento sea reproducible. */
    public static void setSeed(long seed)
    {
        rand = new Random(seed);
    }

    /** Fuente aleatoria compartida, para que la semilla valga en todo el proyecto. */
    public static Random getRandom()
    {
        return rand;
    }

    private static double entre(double min, double max)
    {
        return min + (max - min) * rand.nextDouble();
    }

    public static double randomSepalLength()
    {
        return entre(sepalLengthMin, sepalLengthMax);
    }

    public static double randomSepalWidth()
    {
        return entre(sepalWidthMin, sepalWidthMax);
    }

    public static double randomPetalLength()
    {
        return entre(petalLengthMin, petalLengthMax);
    }

    public static double randomPetalWidth()
    {
        return entre(petalWidthMin, petalWidthMax);
    }
}
