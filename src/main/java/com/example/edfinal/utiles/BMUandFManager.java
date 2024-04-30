package com.example.edfinal.utiles;

import com.example.edfinal.Flower;
import com.example.edfinal.SOMNeuron;

import java.util.ArrayList;

public class BMUandFManager {
    private static ArrayList<BMUandFlowers> lista;

    public static ArrayList<BMUandFlowers> getLista()
    {
        if(lista==null) {
            lista=new ArrayList<>();
        }
        return lista;
    }

    public static void cleanList()
    {
        getLista().clear();
    }

    private static int checkIfIn(SOMNeuron n)
    {
        int pos =-1;
        boolean found=false;

        for(int i=0;i<lista.size() && !found;i++)
        {
            if(lista.get(i).getBmu().equals(n))
            {
                pos = i;
                found=true;
            }
        }

        return pos;
    }

    public static void addToList(SOMNeuron bmu, Flower f,String type)
    {
        int pos = checkIfIn(bmu);
        if(pos!=-1)
        {
            lista.get(pos).addFlower(f);
        }
        else
            lista.add(new BMUandFlowers(bmu, f, type));
    }

}
