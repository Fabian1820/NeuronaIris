package com.example.edfinal;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.LinkedGraph;
import cu.edu.cujae.ceis.graph.vertex.Vertex;

import java.util.*;

public class SOM extends LinkedGraph {

    private static double initialLearningRate = 1.0;

    private double currentLearningRate;
    private static int epochs = 150;
    private int totalNeurons=150;
    private boolean trained;
    public SOM()
    {
      super();
      this.trained=false;
    }

    public boolean isTrained()
    {
        return this.trained;
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

    public void groupBmus(ArrayList<Flower> dataBase)
    {
        for(int i=0;i<49;i++)
        {
            
        }
    }

    public void train()
    {
        for(int i=1;i<=epochs;i++)
        {
            this.currentLearningRate= learningRate(i);
            train2(GestorTxt.getDataBase(), i);
        }
        this.trained=true;
        groupBmus(GestorTxt.getDataBase());
    }

    public void train2(ArrayList<Flower> dataBase, int currentEpoch)
    {
        for (Flower flower : dataBase) {
            SOMNeuron bmu = this.findBMU(flower);
            updateBmuAndAdjacents(bmu, flower, currentEpoch);
        }
    }

    public void updateBmuAndAdjacents(SOMNeuron bmu, Flower flower, int currentEpoch)
    {
        //crear arrayList de actualizadas
        //bmu.actualizarPesos
        //crear arreglo con las cuatro adyacentes
        //actualizar sus cuatro adyacentes con un metodo para eso
        //llamar a actualizar por la izquierda
        //llamar a actualizar por la derecha
    }

    public void updateRadious(ArrayList<SOMNeuron> updated, SOMNeuron current, int distance, Flower flower,char direction, int currentEpoch)
    {
        if (distance <= 30) {

            LinkedList<Vertex> adjacents = current.getAdjacents();

            ArrayList<SOMNeuron> toUpdate = checkNotUpdated(updated, adjacents);
            updateGroup(toUpdate, ++distance, flower, currentEpoch);
            updated.addAll(toUpdate);
            if(direction=='L')
            {
                updateRadious(updated,(SOMNeuron)adjacents.getFirst(), ++distance, flower,'L', currentEpoch);
            }
            else
            {
                updateRadious(updated,(SOMNeuron)adjacents.getLast(), ++distance, flower,'R', currentEpoch);
            }
        }
    }

    public void updateGroup(ArrayList<SOMNeuron> toUpdate, int distance, Flower flower, int currentEpoch)
    {
        double influenceRate = influenceRate(distance, currentEpoch);
        for(SOMNeuron n : toUpdate)
        {
            n.updateWeight(influenceRate, this.currentLearningRate, flower);
        }
    }

    public double influenceRate(int distance, int currentEpoch)
    {
        return Math.exp((-Math.pow(distance,2))/(2*((double)totalNeurons/(2*currentEpoch))));
    }

//    public double learningRate(int currentEpoch)
//    {
//        return (initialLearningRate * Math.exp((double) -currentEpoch / epochs));
//    }

    public ArrayList<SOMNeuron> checkNotUpdated(ArrayList<SOMNeuron> updated, LinkedList<Vertex> adjacents)
    {
        ArrayList<SOMNeuron> toUpdate = new ArrayList<>();

        Iterator<Vertex> iter = adjacents.iterator();

        while(iter.hasNext())
        {
            SOMNeuron n = (SOMNeuron) iter.next();
            if(!updated.contains(n))
            {
                toUpdate.add(n);
            }
        }
        return toUpdate;
    }
}
