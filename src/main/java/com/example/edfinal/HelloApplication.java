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
        stage.setTitle("Hello! soy clari la tank");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        //launch();
        System.out.println("edgfgdfvc");

        int[][] data = {{3,2,1,4}, {4,5,6,7}, {8,6,4,3}, {9,7,5,6}};

        System.out.println(data.length);



        System.out.println("4.3       7.9");
        int i = 20;
        while(i!=0)
        {
            System.out.println(RandomFeaturesPicker.randomSepalLength());
            i--;
        }

        System.out.println("2.0  4.4");
        i = 20;
        while(i!=0)
        {
            System.out.println(RandomFeaturesPicker.randomSepalWidth());
            i--;
        }

    }
}