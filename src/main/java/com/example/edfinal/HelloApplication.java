package com.example.edfinal;

import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.Graph;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Iris Classifier");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
//        Flower f = new Flower(5.0, 3.0, 2.0, 1.0);
//        System.out.println(f.getPetalWidth());
        launch();
//            SOM map = new SOM(100, 60, 1.0, 15);
//            map.initialize();
//            map.train();
//// 2.93 7.10
//        Iterator<Vertex> iter = map.getVerticesList().iterator();
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

//        int k=1;
//
//        ArrayList<Flower> arr = GestorTxt.getDataBase();
//
//        System.out.println("\n\n//////////Ordenado//////////////////////////Clasificacion////////////////\n\n");
//
//        for(int j=0;j<arr.size();j++)
//        {
//            Flower flor = arr.get(j);
//            System.out.print(k + "-" + flor.getType() + "      ");
//
//            SOMNeuron bmu = map.findBMU(flor);
//            System.out.print("   " + "//"+bmu.getId()+"//");
//            System.out.println("   " + k++ + "-" + map.classify(bmu));
//        }

         //   GestorTxt.writeMap(map);
//
//           map = GestorTxt.loadMap();
//        System.out.println(map.getEpochs());
//        System.out.println(map.getTotalNeurons());
//
//        Iterator<Vertex> iter2 = map.getVerticesList().iterator();
//        i=1;
//
//        System.out.println("ANTES\n");
//        while(iter2.hasNext())
//        {
//            SOMNeuron n = (SOMNeuron) iter2.next();
//            System.out.println("Nodo "+ i++);
//            System.out.println("Sepal Width: "+((Flower)n.getInfo()).getSepalWidth());
//            System.out.println("Sepal Length: "+((Flower)n.getInfo()).getSepalLength());
//            System.out.println("Petal Width: "+((Flower)n.getInfo()).getPetalWidth());
//            System.out.println("Petal Length: "+((Flower)n.getInfo()).getPetalLength()+"\n");
//        }
//
//        k=1;
//
//        System.out.println("\n\n//////////Ordenado//////////////////////////Clasificacion////////////////\n\n");
//
//        for(int j=0;j<arr.size();j++)
//        {
//            Flower flor = arr.get(j);
//            System.out.print(k + "-" + flor.getType() + "      ");
//
//            SOMNeuron bmu = map.findBMU(flor);
//            System.out.print("   " + "//"+bmu.getId()+"//");
//            System.out.println("   " + k++ + "-" + map.classify(bmu));
//        }
//
//        k=1;
//
//        ArrayList<Flower> arr = GestorTxt.getDataBase();
//
//        System.out.println("\n\n//////////Ordenado//////////////////////////Clasificacion////////////////\n\n");
//
//        for(int j=0;j<arr.size();j++)
//        {
//            Flower flor = arr.get(j);
//            System.out.print(k + "-" + flor.getType() + "      ");
//
//            SOMNeuron bmu = map.findBMU(flor);
//            System.out.print("   " + "//"+bmu.getId()+"//");
//            System.out.println("   " + k++ + "-" + map.classify(bmu));
//        }
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

//        Iterator<Vertex> iter = m.getVerticesList().iterator();
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

        //  GestorTxt.writeNeurons(m);
        //    m= GestorTxt.loadMap();

//        Iterator<Vertex> iter2 = m.getVerticesList().iterator();
//        i=1;
//
//        System.out.println("ANTES\n");
//        while(iter2.hasNext())
//        {
//            SOMNeuron n = (SOMNeuron) iter2.next();
//            System.out.println("Nodo "+ i++);
//            System.out.println("Sepal Width: "+((Flower)n.getInfo()).getSepalWidth());
//            System.out.println("Sepal Length: "+((Flower)n.getInfo()).getSepalLength());
//            System.out.println("Petal Width: "+((Flower)n.getInfo()).getPetalWidth());
//            System.out.println("Petal Length: "+((Flower)n.getInfo()).getPetalLength()+"\n");
//        }

//        Iterator<Vertex> iter4 = c.getMap().getVerticesList().iterator();
//        i=1;
//
//        System.out.println("ANTES\n");
//        while(iter4.hasNext())
//        {
//            SOMNeuron n = (SOMNeuron) iter4.next();
//            System.out.println("Nodo "+ i++);
//            System.out.println("Sepal Width: "+((Flower)n.getInfo()).getSepalWidth());
//            System.out.println("Sepal Length: "+((Flower)n.getInfo()).getSepalLength());
//            System.out.println("Petal Width: "+((Flower)n.getInfo()).getPetalWidth());
//            System.out.println("Petal Length: "+((Flower)n.getInfo()).getPetalLength()+"\n");
//        }
    }
}