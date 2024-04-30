package com.example.edfinal.utiles;

import com.example.edfinal.Flower;
import com.example.edfinal.SOM;
import com.example.edfinal.SOMNeuron;

import java.util.ArrayList;

public class BMUandFlowers {
    public SOMNeuron bmu;
    public ArrayList<Flower> flores;

    public String type;

    public BMUandFlowers(SOMNeuron bmu, Flower f, String type)
    {
        this.bmu=bmu;
        flores= new ArrayList<>();
        flores.add(f);
        this.type=type;
    }
    public SOMNeuron getBmu()
    {
        return this.bmu;
    }
    public void addFlower(Flower f)
    {
        this.flores.add(f);
    }
}
