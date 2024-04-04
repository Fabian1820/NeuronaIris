package com.example.edfinal.utiles;

import com.example.edfinal.Flower;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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

        BufferedReader bufer;
        try {
            bufer = new BufferedReader(new FileReader(GestorTxt.getPath()));

            String linea;
            try {
                while((linea=bufer.readLine())!=null)
                {
                    String[] partes = linea.split(",");

                    System.out.println(partes.length);
                    double sepalLength = Double.parseDouble(partes[0]);
                    double sepalWidth = Double.parseDouble(partes[1]);
                    double petalLength = Double.parseDouble(partes[2]);
                    double petalWidth = Double.parseDouble(partes[3]);

                    Flower f = new Flower(sepalLength, sepalWidth, petalLength, petalWidth);
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
}
