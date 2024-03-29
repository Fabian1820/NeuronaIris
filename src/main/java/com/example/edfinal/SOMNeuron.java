package com.example.edfinal;

public class SOMNeuron {

    private Flower flower;

    public SOMNeuron()
    {
        this.flower= new Flower();
    }

    public double euclidianDistance(Flower flower)
    {
        double petalLengthDist = Math.pow((this.flower.getPetalLength()-flower.getPetalLength()),2);
        double petalWidthDist = Math.pow((this.flower.getPetalWidth()-flower.getPetalWidth()),2);
        double sepalLengthDist = Math.pow((this.flower.getSepalLength()-flower.getSepalLength()),2);
        double sepalWidthDist = Math.pow((this.flower.getSepalWidth()-flower.getSepalWidth()),2);

        return Math.sqrt( petalLengthDist +  petalWidthDist + sepalLengthDist + sepalWidthDist);
    }

 }
