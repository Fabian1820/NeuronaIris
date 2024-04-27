package com.example.edfinal;

import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.Graph;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello! soy clari la tank");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        //launch();
//        System.out.println("edgfgdfvc");
//
//        int[][] data = {{3,2,1,4}, {4,5,6,7}, {8,6,4,3}, {9,7,5,6}};
//
//        System.out.println(data.length);
//


//        System.out.println("4.3       7.9");
//        int i = 20;
//        while(i!=0)
//        {
//            System.out.println(RandomFeaturesPicker.randomSepalLength());
//            i--;
//        }
//
//        System.out.println("2.0  4.4");
//        i = 20;
//        while(i!=0)
//        {
//            System.out.println(RandomFeaturesPicker.randomSepalWidth());
//            i--;
//        }

//        Flower f = new Flower();
//        SOMNeuron neurona = new SOMNeuron(1, f);
//
//        Flower f2 = new Flower();
//
//        System.out.println("Neurona pesos");
//        System.out.println(((Flower)neurona.getInfo()).getPetalLength());
//        System.out.println(((Flower)neurona.getInfo()).getPetalWidth());
//        System.out.println(((Flower)neurona.getInfo()).getSepalLength());
//        System.out.println(((Flower)neurona.getInfo()).getPetalWidth());
//
//        System.out.println("\nFlor pesos");
//        System.out.println(f2.getPetalLength());
//        System.out.println(f2.getPetalWidth());
//        System.out.println(f2.getSepalLength());
//        System.out.println(f2.getPetalWidth());
//
//        System.out.println();
//        System.out.println(neurona.euclidianDistance(f2));

        SOM mapa = new SOM();

        mapa.initialize();

        Iterator<Vertex> iter = mapa.getVerticesList().iterator();
        int i=1;

        System.out.println("ANTES\n");
        while(iter.hasNext())
        {
            SOMNeuron n = (SOMNeuron) iter.next();
            System.out.println("Nodo "+ i++);
            System.out.println("Sepal Width: "+((Flower)n.getInfo()).getSepalWidth());
            System.out.println("Sepal Length: "+((Flower)n.getInfo()).getSepalLength());
            System.out.println("Petal Width: "+((Flower)n.getInfo()).getPetalWidth());
            System.out.println("Petal Length: "+((Flower)n.getInfo()).getPetalLength()+"\n");
        }

        mapa.train();


        int k=1;

        ArrayList<Flower> arr = GestorTxt.getDataBase();

        System.out.println("\n\n//////////Ordenado//////////////////////////Clasificacion////////////////\n\n");

        for(int j=0;j<arr.size();j++)
        {
            Flower flor = arr.get(j);
            System.out.print(k + "-" + flor.getType() + "      ");

            SOMNeuron bmu = mapa.findBMU(flor);
            System.out.print("   " + "//"+bmu.getId()+"//");
            System.out.println("   " + k++ + "-" + mapa.classify(bmu));
        }
//
//        k=1;
//        Collections.shuffle(arr);
//
//        System.out.println("\n\n//////////Desordenado//////////////////////////Clasificacion////////////////\n\n");
//
//        for(int j=0;j<arr.size();j++)
//        {
//            Flower flor = arr.get(j);
//            System.out.print(k + "-" + flor.getType() + "      ");
//
//            SOMNeuron bmu = mapa.findBMU(flor);
//            System.out.print("   " + "//"+bmu.getId()+"//");
//            System.out.println("   " + k++ + "-" + mapa.classify(bmu));
//        }
//sdfg


    }
}