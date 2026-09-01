package com.example.edfinal;

import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.Graph;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedMap;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Parent raiz = fxmlLoader.load();

        // El layout reparte el espacio por sí solo (BorderPane + GridPane +
        // FlowPane), así que la ventana solo necesita abrirse a un tamaño que
        // quepa en el monitor. Antes había que escalar el contenido porque el
        // FXML era un lienzo fijo de 1545x881.
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        double ancho = Math.min(1545, pantalla.getWidth());
        double alto = Math.min(881, pantalla.getHeight());

        stage.setTitle("NeuronaIris");

        // El icono va en los recursos del módulo, así que viaja dentro del jar.
        var icono = HelloApplication.class.getResourceAsStream("logo.png");
        if (icono != null) stage.getIcons().add(new Image(icono));

        stage.setScene(new Scene(raiz, ancho, alto));
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
//        Flower f = new Flower(5.0, 3.0, 2.0, 1.0);
//        System.out.println(f.getPetalWidth());
       launch();
//            SOM map = new SOM(150, 150, 1.0, 30);
//            map.initialize();
//            map.train();
//
//            GestorTxt.writeHeaderConfig(map);
//
//        ArrayList<Flower> arr = GestorTxt.getDataBase();
//        for(int j=0;j<5;j++)
//        {
//            Flower flor = arr.get(j);
//            SOMNeuron bmu = map.findBMU(flor);
//            GestorTxt.writeInConfig(bmu);
//            System.out.print("   " + "//"+bmu.getId()+"//");
//        }
//
//        System.out.println();
//
//        ArrayList<SOMNeuron> list = GestorTxt.readConfig();
//
//        for(SOMNeuron n : list)
//        {
//            System.out.print("   " + "//"+n.getId()+"//");
//        }

//            ArrayList<SOMNeuron> n = map.findBMUConLista(new Flower(5.0,3.0,2.0,1.0));
//            System.out.println(n.size());
//            String resp = map.classify(n);
//            System.out.println(((Flower)n.getInfo()).getPetalLength());
//            System.out.println(resp);
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