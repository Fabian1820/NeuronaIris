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

    private static ArrayList<Flower> flowersDataBase;
    private static ArrayList<Flower> lista;

    public static ArrayList<Flower> getDataBase()
    {
        if(flowersDataBase==null)
        {
            flowersDataBase = load(path);
        }
        return flowersDataBase;
    }

    public static ArrayList<Flower> getFile(String path)
    {
        lista = load(path);

        return lista;
    }

    private static String getPath()
    {
        return path;
    }

    private static ArrayList<Flower> load(String path){
        ArrayList<Flower> flowers = new ArrayList<>();
        BufferedReader bufer;
        try {
            bufer = new BufferedReader(new FileReader(path));

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

        return flowers;
    }

    public static void writeMap(SOM map) throws IOException {
        RandomAccessFile raf = new RandomAccessFile("Map.dat", "rw");

        raf.writeInt(map.getEpochs());
        raf.writeInt(map.getTotalNeurons());
        raf.writeDouble(map.getInitialLearningRate());
        raf.writeInt(map.getRadious());

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
        RandomAccessFile raf = new RandomAccessFile("Map.dat", "rw");
        int epochs= raf.readInt();
        int neurons = raf.readInt();
        double learningRate = raf.readDouble();
        int radius = raf.readInt();
        SOM m = new SOM(epochs, neurons, learningRate, radius);
        m.setTrained(true);
        m.setInit(true);
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

    public static void writeItemB() {
        try {
            PrintWriter pw = new PrintWriter("incisoB.txt");
            for(BMUandFlowers baf : BMUandFManager.getLista())
            {
                pw.write("BMU: " + baf.bmu.getId()+" ");
                double sepalLength = ((Flower)baf.bmu.getInfo()).getSepalLength();
                double sepalWidth = ((Flower)baf.bmu.getInfo()).getSepalWidth();
                double petalLength = ((Flower)baf.bmu.getInfo()).getPetalLength();
                double petalWidth = ((Flower)baf.bmu.getInfo()).getPetalWidth();
                pw.print(sepalLength +","+ sepalWidth +","+ petalLength +","+ petalWidth +"  ");
                pw.println(baf.type);
                for(Flower f : baf.flores)
                {
                    double sepalLengthF = f.getSepalLength();
                    double sepalWidthF = f.getSepalWidth();
                    double petalLengthF = f.getPetalLength();
                    double petalWidthF = f.getPetalWidth();
                    pw.println(sepalLengthF +","+ sepalWidthF +","+ petalLengthF +","+ petalWidthF);
                }
                pw.println();
            }
            pw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }
}
