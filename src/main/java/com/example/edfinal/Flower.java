package com.example.edfinal;

import java.util.Random;

public class Flower {
    private double petalWidth;
    private double petalLength;
    private double sepalWidth;
    private double sepalLength;

    public Flower(String type, double petalWidth, double petalLength, double sepalWidth, double sepalLength)
    {

        setPetalWidth(petalWidth);
        setPetalLength(petalLength);
        setSepalWidth(sepalWidth);
        setSepalLength(sepalLength);
    }

    public Flower()
    {
        this.petalWidth=RandomFeaturesPicker.randomPetalWidth();
        this.petalLength=RandomFeaturesPicker.randomPetalLength();
        this.sepalLength=RandomFeaturesPicker.randomSepalLength();
        this.sepalWidth=RandomFeaturesPicker.randomSepalWidth();
    }

    private void setSepalLength(double sepalLength) {
        //validaciones luego
        this.sepalLength=sepalLength;
    }

    private void setSepalWidth(double sepalWidth) {
        //validaciones luego
        this.sepalWidth=sepalWidth;
    }

    private void setPetalLength(double petalLength) {
        //validaciones luego
        this.petalLength=petalLength;
    }

    private void setPetalWidth(double petalWidth) {
        //validaciones luego
        this.petalWidth=petalWidth;
    }

    public double getPetalWidth() {
        return this.petalWidth;
    }

    public double getPetalLength() {
        return this.petalLength;
    }

    public double getSepalWidth() {
        return this.sepalWidth;
    }

    public double getSepalLength() {
        return this.sepalLength;
    }

}
