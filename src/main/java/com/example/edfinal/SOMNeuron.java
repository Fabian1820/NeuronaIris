package com.example.edfinal;

import cu.edu.cujae.ceis.graph.vertex.Vertex;
import cu.edu.cujae.ceis.graph.vertex.WeightedVertex;

public class SOMNeuron extends Vertex{
    private int id;
    private boolean updated;
    public SOMNeuron( int id, Flower flower)
    {
        super(flower);
        this.id=id;
        this.updated = false;
    }

    public double euclidianDistance(Flower flower)
    {
        double petalLengthDist = Math.pow((((Flower)this.getInfo()).getPetalLength()-flower.getPetalLength()),2);
        double petalWidthDist = Math.pow((((Flower)this.getInfo()).getPetalWidth()-flower.getPetalWidth()),2);
        double sepalLengthDist = Math.pow((((Flower)this.getInfo()).getSepalLength()-flower.getSepalLength()),2);
        double sepalWidthDist = Math.pow((((Flower)this.getInfo()).getSepalWidth()-flower.getSepalWidth()),2);

        return Math.sqrt( petalLengthDist +  petalWidthDist + sepalLengthDist + sepalWidthDist);
    }

    public int getId()
    {
        return this.id;
    }
    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
}
