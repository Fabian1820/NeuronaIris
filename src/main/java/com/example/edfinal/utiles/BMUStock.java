package com.example.edfinal.utiles;

import com.example.edfinal.SOMNeuron;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * BMUs agrupadas por etiqueta.
 *
 * Antes eran tres listas fijas (setosa/versicolor/virginica). Ahora es un mapa
 * por etiqueta, para que sirva con cualquier dataset; los tres accesores de
 * siempre se mantienen porque la interfaz pinta cada especie de un color.
 */
public class BMUStock {

    private static final Map<String, ArrayList<SOMNeuron>> grupos = new LinkedHashMap<>();

    /** Lista de BMUs de una etiqueta; se crea vacía si aún no existe. */
    public static ArrayList<SOMNeuron> forLabel(String label)
    {
        return grupos.computeIfAbsent(label, k -> new ArrayList<>());
    }

    /** Etiquetas presentes, en orden de aparición. */
    public static Set<String> labels()
    {
        return grupos.keySet();
    }

    public static ArrayList<SOMNeuron> getSetosa()
    {
        return forLabel("setosa");
    }

    public static ArrayList<SOMNeuron> getVersicolor()
    {
        return forLabel("versicolor");
    }

    public static ArrayList<SOMNeuron> getVirginica()
    {
        return forLabel("virginica");
    }

    /**
     * Descarta el agrupamiento entero, etiquetas incluidas.
     *
     * No basta con vaciar las listas: si se quedaran las claves, labels()
     * seguiría anunciando etiquetas de un dataset anterior.
     */
    public static void clear()
    {
        grupos.clear();
    }
}
