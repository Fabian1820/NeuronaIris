//package com.example.edfinal.utiles;
//
//import com.example.edfinal.Flower;
//import com.example.edfinal.GraphWriter;
//import com.example.edfinal.SOM;
//import com.example.edfinal.SOMNeuron;
//import cu.edu.cujae.ceis.graph.vertex.Vertex;
//
//import java.io.*;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class Control {
//    private SOM map;
//
//    public Control()
//    {
//        map = new SOM();
//    }
//
//    public SOM getMap()
//    {
//        return map;
//    }
//
//    public void setMap(SOM m)
//    {
//        this.map=m;
//    }
//
//    public String writeMapInDat() throws IOException {
//        String resp = "";
//        if(this.map.isTrained())
//        {
//            resp="Mapa guardado con �xito";
//            GraphWriter.writeNeurons(this.map.getVerticesList());
//        }
//        else
//            resp="El mapa no ha sido entrenado";
//
//        return resp;
//    }
//
//    public SOM readMapFromDat(){
//
//        SOM m = new SOM();
//        List<SOMNeuron> neuronas = new LinkedList<>();
//
//        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("Grafo.dat"))) {
//            // Leer el tama�o de la lista
//            int size = inputStream.readInt();
//
//            // Leer las neuronas
//            for (int i = 0; i < size; i++) {
//                SOMNeuron neurona = (SOMNeuron) inputStream.readObject();
//                m.getVerticesList().add(neurona);
//            }
//            System.out.println("Se han le�do " + m.getVerticesList().size() + " neuronas desde el archivo binario.");
//        } catch (Exception e) {
//            System.err.println("Error al leer las neuronas desde el archivo binario: " + e.getMessage());
//        }
//
//        Iterator<Vertex> iter = m.getVerticesList().iterator();
//
//        int i=1;
//
//        System.out.println("ANTES\n");
//        while(iter.hasNext())
//        {
//            SOMNeuron n = (SOMNeuron) iter.next();
//            System.out.println("Nodo "+ i++);
//            System.out.println("Sepal Width: "+((Flower)n.getInfo()).getSepalWidth());
//            System.out.println("Sepal Length: "+((Flower)n.getInfo()).getSepalLength());
//            System.out.println("Petal Width: "+((Flower)n.getInfo()).getPetalWidth());
//            System.out.println("Petal Length: "+((Flower)n.getInfo()).getPetalLength()+"\n");
//        }
//
//
//       return m;
//    }
//
//}
