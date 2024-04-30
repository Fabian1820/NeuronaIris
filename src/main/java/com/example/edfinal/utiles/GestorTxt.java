package com.example.edfinal.utiles;

import com.example.edfinal.Flower;
import com.example.edfinal.SOM;
import com.example.edfinal.SOMNeuron;
import cu.edu.cujae.ceis.graph.vertex.Vertex;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class GestorTxt {
    private static final String path = "src/main/java/com/example/edfinal/Ficheros/iris.data";

    private static ArrayList<Flower> flowers;

    public static ArrayList<Flower> getDataBase()
    {
        if(flowers==null)
        {
            flowers = new ArrayList<>();
            loadFlowers();
        }

        return flowers;
    }

    private static String getPath()
    {
        return path;
    }

    private static void loadFlowers(){
        int i=1;
        BufferedReader bufer;
        try {
            bufer = new BufferedReader(new FileReader(GestorTxt.getPath()));

            String linea;
            try {
                while((linea=bufer.readLine())!=null)
                {
                    String[] partes = linea.split(",");

                    double sepalLength = Double.parseDouble(partes[0]);
                    double sepalWidth = Double.parseDouble(partes[1]);
                    double petalLength = Double.parseDouble(partes[2]);
                    double petalWidth = Double.parseDouble(partes[3]);
                    String type = partes[4];

                    Flower f = new Flower(sepalLength, sepalWidth, petalLength, petalWidth, type);
                    flowers.add(f);
                }

            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void writeNeurons(SOM map) throws IOException {
        RandomAccessFile raf = new RandomAccessFile("Grafo.dat", "rw");

        raf.writeInt(map.getVerticesList().size());

        Iterator<Vertex> iter = map.getVerticesList().iterator();

        while (iter.hasNext())
        {
            SOMNeuron n = (SOMNeuron) iter.next();
            raf.writeInt(n.getId());
            raf.writeDouble(((Flower)n.getInfo()).getSepalLength());
            raf.writeDouble(((Flower)n.getInfo()).getSepalWidth());
            raf.writeDouble(((Flower)n.getInfo()).getPetalLength());
            raf.writeDouble(((Flower)n.getInfo()).getPetalWidth());
        }

        raf.close();
    }

    public static SOM loadMap() throws IOException {
        RandomAccessFile raf = new RandomAccessFile("Grafo.dat", "rw");
        SOM m = new SOM();
        m.setTrained(true);
        int neurons = raf.readInt();
        while(neurons>0)
        {
            int id = raf.readInt();
            double sepalLength = raf.readDouble();
            double sepalWidth = raf.readDouble();
            double petalLength = raf.readDouble();
            double petalWidth = raf.readDouble();
            m.getVerticesList().add(new SOMNeuron(id, new Flower(sepalLength, sepalWidth, petalLength, petalWidth)));
            neurons--;
        }
        m.makeConnections();
        raf.close();
        return  m;
    }
}
