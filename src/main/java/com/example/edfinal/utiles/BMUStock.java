package com.example.edfinal.utiles;

import com.example.edfinal.SOMNeuron;

import java.util.ArrayList;

public class BMUStock {
    private static ArrayList<SOMNeuron> setosa;
    private static ArrayList<SOMNeuron> versicolor;
    private static ArrayList<SOMNeuron> virginica;

    public static ArrayList<SOMNeuron> getSetosa()
    {
        if(setosa==null)
        {
            setosa = new ArrayList<>();
        }
        return setosa;
    }

    public static ArrayList<SOMNeuron> getVersicolor()
    {
        if(versicolor==null)
        {
            versicolor = new ArrayList<>();
        }
        return versicolor;
    }

    public static ArrayList<SOMNeuron> getVirginica()
    {
        if(virginica==null)
        {
            virginica = new ArrayList<>();
        }
        return virginica;
    }
}
