package com.example.edfinal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello! soy fab :) el tank");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
        SOM som = new SOM(10, 0.1, 100);

        CargadorDatosFlor cargador = new CargadorDatosFlor();

        List<Flor> flores = null;
        try {
            flores = cargador.cargarDatosFlor("ruta/a/tus/datos.txt");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        som.entrenar(flores);

        EscritorGrupos escritor = new EscritorGrupos();
        try {
            escritor.escribirGrupos(som.obtenerNeuronas(), "ruta/a/tu/salida.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        ContadorGrupos contador = new ContadorGrupos();
        int cantidadGrupos = contador.contarGrupos(som.obtenerNeuronas());
        System.out.println("Cantidad deE grupos formados: " + cantidadGrupos);



        try {
            Files.write(Paths.get("ruta/a/tu/configuracion.txt"), Collections.singleton(String.valueOf(cantidadGrupos)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}