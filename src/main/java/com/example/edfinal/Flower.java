package com.example.edfinal;

import java.util.Random;

public class Flower {
    private String type;
    private double petalWidth;
    private double petalLength;
    private double sepalWidth;
    private double sepalLength;

    public Flower(String type, double petalWidth, double petalLength, double sepalWidth, double sepalLength)
    {
        setType(type);
        setPetalWidth(petalWidth);
        setPetalLength(petalLength);
        setSepalWidth(sepalWidth);
        setSepalLength(sepalLength);
    }

    public Flower()
    {
        Random r = new Random();
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

    private void setType(String type) {
        //validaciones luego
        this.type=type;
    }
}
