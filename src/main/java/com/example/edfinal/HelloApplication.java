package com.example.edfinal;

import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.Graph;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
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

    /** Tamaño para el que está dibujado el FXML (coordenadas absolutas). */
    private static final double DISENO_ANCHO = 1545;
    private static final double DISENO_ALTO = 881;

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Parent contenido = fxmlLoader.load();

        // El FXML usa posiciones absolutas sobre un lienzo de 1545x881, así que
        // en cualquier monitor más estrecho se salía por el borde derecho. En vez
        // de rehacer el layout entero, se escala el contenido para que quepa
        // siempre, manteniendo la proporción y centrado.
        Group lienzo = new Group(contenido);
        StackPane raiz = new StackPane(lienzo);
        raiz.setStyle("-fx-background-color: #1e1e1e;");

        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        double ancho = Math.min(DISENO_ANCHO, pantalla.getWidth());
        double alto = Math.min(DISENO_ALTO, pantalla.getHeight());

        Scene scene = new Scene(raiz, ancho, alto);

        Scale escala = new Scale(1, 1, 0, 0);
        contenido.getTransforms().add(escala);

        ChangeListener<Number> ajustar = (obs, viejo, nuevo) -> {
            double factor = Math.min(scene.getWidth() / DISENO_ANCHO,
                                     scene.getHeight() / DISENO_ALTO);
            escala.setX(factor);
            escala.setY(factor);
        };
        scene.widthProperty().addListener(ajustar);
        scene.heightProperty().addListener(ajustar);
        ajustar.changed(null, 0, 0);

        stage.setTitle("Iris Classifier");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
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