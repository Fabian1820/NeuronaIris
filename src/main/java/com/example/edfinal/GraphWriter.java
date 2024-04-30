package com.example.edfinal;

import cu.edu.cujae.ceis.graph.vertex.Vertex;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;

public class GraphWriter {
    public static void writeNeurons(LinkedList<Vertex> neurons) throws IOException {
        FileOutputStream file = new FileOutputStream("Grafo.dat");
        ObjectOutputStream oos = new ObjectOutputStream(file);

        oos.writeInt(neurons.size());
        Iterator<Vertex> iter = neurons.iterator();
        while (iter.hasNext())
        {
            SOMNeuron n = (SOMNeuron) iter.next();
            oos.writeObject(n);
        }
        file.close();
    }
}
