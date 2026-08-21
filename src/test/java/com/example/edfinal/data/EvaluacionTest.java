package com.example.edfinal.data;

import com.example.edfinal.RandomFeaturesPicker;
import com.example.edfinal.SOM;
import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Evaluación honesta: entrenar con una parte y medir sobre la otra.
 * Hasta ahora todos los porcentajes salían de los mismos datos del
 * entrenamiento, así que eran optimistas por construcción.
 */
class EvaluacionTest {

    private static final int SEMILLAS = 20;

    @Test
    @DisplayName("La partición estratificada respeta la proporción de cada etiqueta")
    void particionEstratificada() {
        Dataset[] p = GestorTxt.getIrisDataset().split(0.7, 1);
        Dataset entrena = p[0], prueba = p[1];

        assertEquals(150, entrena.size() + prueba.size(), "no se pierde ni se duplica ninguna muestra");
        assertEquals(105, entrena.size());
        assertEquals(45, prueba.size());
        assertEquals(3, entrena.labels().size());
        assertEquals(3, prueba.labels().size());

        for (String etiqueta : prueba.labels()) {
            long n = prueba.getSamples().stream().filter(s -> etiqueta.equals(s.getLabel())).count();
            assertEquals(15, n, "cada especie debe aportar 15 muestras de prueba, y " + etiqueta + " dio " + n);
        }
    }

    @Test
    @DisplayName("Una proporción fuera de (0,1) se rechaza")
    void proporcionInvalida() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> GestorTxt.getIrisDataset().split(1.5, 1));
        assertTrue(e.getMessage().contains("entre 0 y 1"), e.getMessage());
    }

    @Test
    @DisplayName("Acierto sobre datos no vistos y matriz de confusión")
    void aciertoSobreDatosNoVistos() {
        List<String> especies = List.of("setosa", "versicolor", "virginica");
        int[][] acumulada = new int[3][3];
        double sumaEntrena = 0, sumaPrueba = 0;

        for (long semilla = 1; semilla <= SEMILLAS; semilla++) {
            Dataset[] p = GestorTxt.getIrisDataset().split(0.7, semilla);
            Dataset entrena = p[0], prueba = p[1];

            RandomFeaturesPicker.setSeed(semilla);
            BMUStock.clear();
            SOM som = new SOM(40, 8, 6, 0.5, 3, entrena);
            som.initialize();
            som.train();

            sumaEntrena += SOMAnalysis.accuracy(
                    SOMAnalysis.confusionMatrix(som, entrena.getSamples(), especies));

            int[][] c = SOMAnalysis.confusionMatrix(som, prueba.getSamples(), especies);
            sumaPrueba += SOMAnalysis.accuracy(c);
            for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) acumulada[i][j] += c[i][j];
        }

        double entrenamiento = sumaEntrena / SEMILLAS, test = sumaPrueba / SEMILLAS;
        System.out.printf("=== media de %d particiones 70/30 ===%n", SEMILLAS);
        System.out.printf("acierto en entrenamiento : %.1f%%%n", entrenamiento);
        System.out.printf("acierto en prueba        : %.1f%%   (diferencia %.1f puntos)%n",
                test, entrenamiento - test);

        System.out.println("matriz de confusión acumulada sobre prueba (fila = real, columna = predicha)");
        System.out.printf("%-12s %10s %10s %10s%n", "", "setosa", "versicolor", "virginica");
        for (int i = 0; i < 3; i++) {
            System.out.printf("%-12s %10d %10d %10d%n",
                    especies.get(i), acumulada[i][0], acumulada[i][1], acumulada[i][2]);
        }

        assertTrue(test > 85.0, "el acierto sobre datos no vistos debería seguir siendo alto: " + test);
        assertTrue(entrenamiento - test < 12.0,
                "una diferencia grande entre entrenamiento y prueba indicaría sobreajuste: "
                        + (entrenamiento - test) + " puntos");
    }

    @Test
    @DisplayName("Setosa se separa perfectamente; la confusión se concentra en las otras dos")
    void dondeFalla() {
        List<String> especies = List.of("setosa", "versicolor", "virginica");
        int[][] acumulada = new int[3][3];

        for (long semilla = 1; semilla <= SEMILLAS; semilla++) {
            Dataset[] p = GestorTxt.getIrisDataset().split(0.7, semilla);
            RandomFeaturesPicker.setSeed(semilla);
            BMUStock.clear();
            SOM som = new SOM(40, 8, 6, 0.5, 3, p[0]);
            som.initialize();
            som.train();

            int[][] c = SOMAnalysis.confusionMatrix(som, p[1].getSamples(), especies);
            for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) acumulada[i][j] += c[i][j];
        }

        int fallosSetosa = acumulada[0][1] + acumulada[0][2] + acumulada[1][0] + acumulada[2][0];
        int fallosEntreOtras = acumulada[1][2] + acumulada[2][1];

        System.out.printf("fallos que implican a setosa: %d  ·  entre versicolor y virginica: %d%n",
                fallosSetosa, fallosEntreOtras);

        assertTrue(fallosEntreOtras > fallosSetosa,
                "en Iris la confusión real está entre versicolor y virginica, no con setosa");
    }
}
