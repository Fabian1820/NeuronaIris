package com.example.edfinal;

import com.example.edfinal.ui.PanelInferior;
import com.example.edfinal.data.Dataset;
import com.example.edfinal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** El panel lateral resume cuántas muestras aporta cada etiqueta. */
class DistribucionTest {

    @Test
    @DisplayName("En el Iris cuenta 50 por especie y quita el prefijo")
    void cuentaElIris() {
        Map<String, Integer> c = PanelInferior.contarPorEtiqueta(GestorTxt.getIrisDataset());

        assertEquals(3, c.size());
        assertTrue(c.containsKey("setosa"), "debe quitar el prefijo Iris-: " + c.keySet());
        for (var e : c.entrySet()) assertEquals(50, e.getValue(), e.getKey());
    }

    @Test
    @DisplayName("En el dataset de ejemplo cuenta 40 por grupo")
    void cuentaElEjemplo() throws IOException {
        Map<String, Integer> c = PanelInferior.contarPorEtiqueta(
                Dataset.fromCsv("docs/ejemplo-3variables.csv"));

        assertEquals(3, c.size());
        assertEquals(120, c.values().stream().mapToInt(Integer::intValue).sum());
        for (var e : c.entrySet()) assertEquals(40, e.getValue(), e.getKey());
    }

    @Test
    @DisplayName("Un dataset sin etiquetas no rompe el resumen")
    void sinEtiquetas() throws IOException {
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("sin-etiquetas", ".csv");
        java.nio.file.Files.writeString(tmp, "1,2\n3,4\n5,6\n");
        try {
            Dataset d = Dataset.fromCsv(tmp.toString());
            assertTrue(PanelInferior.contarPorEtiqueta(d).isEmpty());
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
    }
}
