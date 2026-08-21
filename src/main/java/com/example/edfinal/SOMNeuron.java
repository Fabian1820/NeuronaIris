package com.example.edfinal;

import com.example.edfinal.data.Sample;
import cu.edu.cujae.ceis.graph.vertex.Vertex;

import java.io.Serializable;

/**
 * Una neurona del mapa. Sus pesos son una muestra del mismo espacio que los
 * datos: N variables, no cuatro medidas fijas.
 */
public class SOMNeuron extends Vertex implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;

    public SOMNeuron(int id, Sample weights)
    {
        super(weights);
        this.id=id;
    }

    /** Pesos de la neurona. */
    public Sample getWeights()
    {
        return (Sample) this.getInfo();
    }

    public double euclidianDistance(Sample other)
    {
        return getWeights().distanceTo(other);
    }

    public int getId()
    {
        return this.id;
    }

    public void updateWeight(double influenceRate, double learningRate, Sample target)
    {
        getWeights().moveToward(target, influenceRate, learningRate);
    }

    public double updateFeature(double influenceRate, double learningRate, double newWeight, double currentWeight){

        return currentWeight + influenceRate * learningRate * (newWeight - currentWeight);
    }

    @Override
    public String toString() {
        return getInfo() != null ? getInfo().toString() : "SOMNeuron sin datos";
    }
}
