package com.example.edfinal;
import cu.edu.cujae.ceis.graph.LinkedGraph;
import cu.edu.cujae.ceis.graph.vertex.Vertex;

import java.util.*;

public class SOM extends LinkedGraph {

    private static double initialLearningRate = 1.0;
    private static int epochs = 150;
    private int currentEpoch;

    private int maxNumberNeurons;
    public SOM()
    {
      super();
      this.currentEpoch = 1;
    }

    public void initialize()
    {
        for(int i=1;i<7;i++) {
            this.getVerticesList().add(new SOMNeuron(i, new Flower()));
        }
        makeConnections();
    }

    //This method guaranties that the first two neurons in the edges list of each
    //neuron to be the two previous ones and the other two be the two following neurons
    private void makeConnections()
    {   int i;

        this.insertEdgeDG(0,this.verticesList.size()-2);
        this.insertEdgeDG(0,this.verticesList.size()-1);
        this.insertEdgeDG(1,this.verticesList.size()-1);

        for(i=0;i<this.verticesList.size()-2;i++)
        {
            this.insertEdgeNDG(i,i+1);
            this.insertEdgeNDG(i, i+2);
        }

        this.insertEdgeNDG(i, i+1); //Esta seria la conexion de la penultima neurona con la ultima y la primera
        this.insertEdgeDG(i, 0);
        this.insertEdgeDG(++i, 0); //Esta seria la conexion de la ultima neurona con las dos primeras
        this.insertEdgeDG(i, 1);
    }
    public SOMNeuron findBMU(Flower flower)
    {
        SOMNeuron BMU = null;
        double shortestED = Double.MAX_VALUE;
        Iterator<Vertex> iter = this.verticesList.iterator();
        double currentDist = 0.0;
        while(iter.hasNext())
        {
            SOMNeuron current = (SOMNeuron) iter.next();
            currentDist = current.euclidianDistance(flower);
            if(currentDist<=shortestED)
                {
                    shortestED=currentDist;
                    BMU=current;
                }

        }

        return BMU;
    }

    public void train(){
        for(int i=0;i<=epochs;i++)
            train(GestorTxt.getDataBase());
    }

    public void train(ArrayList<Flower> dataBase){
        for(Flower f : dataBase){
            SOMNeuron bmu = this.findBMU(f);
            updateBMUandAdjacents(bmu, f);
        }
    }

    public void updateBMUandAdjacents(SOMNeuron bmu, Flower f){
        if (!bmu.isUpdated()) {
            updateWeights(bmu, f);
            bmu.setUpdated(true);

            LinkedList<Vertex> neighbors = bmu.getAdjacents(); // Conflicto obt vecinos
            for (Vertex neighbor : neighbors) {
                if (!((SOMNeuron)neighbor).isUpdated()) {
                    updateWeights((SOMNeuron)neighbor, f);
                    ((SOMNeuron)neighbor).setUpdated(true);
                }
            }
        }
    }
    private void updateWeights(SOMNeuron neuron, Flower flower) {
        double learningRate = calculateLearningRate();
        double[] weights = neuron.//aqui obtener los pesos
        double[] inputVector = {flower.getPetalWidth(), flower.getPetalLength(), flower.getSepalWidth(), flower.getSepalLength()};

        for (int i = 0; i < weights.length; i++) {
            weights[i] += learningRate * (inputVector[i] - weights[i]);
        }
    }
    private double calculateLearningRate() {

        double maxEpochs = (double) epochs;
        double currentEpochNormalized = (double) currentEpoch / maxEpochs;

        double initialEta = initialLearningRate;
        double theta = maxNumberNeurons / (2.0 * currentEpoch); // Ajustar mNN
        double eta = initialEta * Math.exp(-Math.pow(currentEpochNormalized, 2) / (2.0 * Math.pow(theta, 2)));

        return eta;
    }

}
