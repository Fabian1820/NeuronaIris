package com.example.edfinal;
import cu.edu.cujae.ceis.graph.LinkedGraph;
import cu.edu.cujae.ceis.graph.vertex.Vertex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class SOM extends LinkedGraph {
    public SOM()
    {
      super();
    }

    public void initialize()
    {
        for(int i=1;i<7;i++) {
            this.getVerticesList().add(new SOMNeuron(i, new Flower()));
        }
        makeConnections();
    }

    private void makeConnections()
    {   int i;
        for(i=0;i<this.verticesList.size()-2;i++)
        {
            this.insertEdgeNDG(i,i+1);
            this.insertEdgeNDG(i, i+2);
        }
        this.insertEdgeNDG(i, ++i);        //Esta seria la conexion de la penultima neurona con la ultima y la primera
        this.insertEdgeNDG(i, 0);

        this.insertEdgeNDG(i, 0); //Esta seria la conexion de la ultima neurona con las dos primeras
        this.insertEdgeNDG(i, 1);
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

}
