package com.example.edfinal;

import java.util.Random;

public class Flower {
    private double petalWidth;
    private double petalLength;
    private double sepalWidth;
    private double sepalLength;

    public Flower(double sepalLength, double sepalWidth, double petalLength, double petalWidth )
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
        if(sepalLength>=4.3 && sepalLength<=7.9)
            this.sepalLength=sepalLength;
        else
            throw new IllegalArgumentException("The sepal length must be between 4.3 and 7.9");
    }

    private void setSepalWidth(double sepalWidth) {
        if(sepalWidth>=2.0 && sepalWidth<=4.4)
            this.sepalWidth=sepalWidth;
        else
            throw new IllegalArgumentException("The sepal width must be between 2.0 and 4.4");
    }

    private void setPetalLength(double petalLength) {
       if(petalLength>=1.0 && petalLength<=6.9)
            this.petalLength=petalLength;
       else
           throw new IllegalArgumentException("The petal length must be between 1.0 and 6.9");
    }

    private void setPetalWidth(double petalWidth) {
        if(petalWidth>=0.1 && petalWidth<=2.5)
            this.petalWidth=petalWidth;
        else
            throw new IllegalArgumentException("The petal width must be between 0.1 and 2.5");
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
