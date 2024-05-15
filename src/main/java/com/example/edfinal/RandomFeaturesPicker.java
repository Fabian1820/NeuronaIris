package com.example.edfinal;

import java.util.Random;

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

    public static double randomSepalLength()
    {
        Random rand = new Random();

        return 0 + (100) * rand.nextDouble();
    }

    public static double randomSepalWidth()
    {
        Random rand = new Random();

        return 0 + (100) * rand.nextDouble();
    }

    public static double randomPetalLength()
    {
        Random rand = new Random();

        return 0 + (100) * rand.nextDouble();
    }

    public static double randomPetalWidth()
    {
        Random rand = new Random();

        return 0 + (100) * rand.nextDouble();
    }
}
