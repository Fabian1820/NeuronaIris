package com.example.edfinal.data;

import com.example.edfinal.SOM;
import com.example.edfinal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Búsqueda aleatoria frente a la parrilla completa: ¿llega al mismo sitio con
 * menos evaluaciones? Se mide en vez de darlo por sabido.
 */
class BusquedaAleatoriaTest {

    private BusquedaHiperparametros.Espacio espacio() {
        return BusquedaHiperparametros.espacioPorDefecto();
    }

    @Test
    @DisplayName("Sortea el número pedido de configuraciones, todas distintas")
    void muestraSinRepetidos() {
        List<BusquedaHiperparametros.Config> cs =
                BusquedaHiperparametros.muestrear(espacio(), 40, 1);

        assertEquals(40, cs.size());
        assertEquals(40, new HashSet<>(cs).size(), "no debería repetir combinaciones");
    }

    @Test
    @DisplayName("Todo lo sorteado cae dentro de los rangos del espacio")
    void dentroDeRango() {
        var e = espacio();
        for (var c : BusquedaHiperparametros.muestrear(e, 60, 2)) {
            assertTrue(c.epocas() >= e.epocasMin() && c.epocas() <= e.epocasMax());
            assertTrue(c.neuronas() >= e.neuronasMin() && c.neuronas() <= e.neuronasMax());
            assertTrue(c.radio() >= e.radioMin() && c.radio() <= e.radioMax());
            assertTrue(c.tasaAprendizaje() >= e.tasaMin() && c.tasaAprendizaje() <= e.tasaMax());
            assertTrue(e.topologias().contains(c.topologia()));
            assertTrue(c.neuronas() >= 4, "una rejilla necesita al menos 4 neuronas");
        }
    }

    @Test
    @DisplayName("Con la misma semilla salen las mismas configuraciones")
    void reproducible() {
        assertEquals(BusquedaHiperparametros.muestrear(espacio(), 25, 9),
                BusquedaHiperparametros.muestrear(espacio(), 25, 9));
        assertNotEquals(BusquedaHiperparametros.muestrear(espacio(), 25, 9),
                BusquedaHiperparametros.muestrear(espacio(), 25, 10));
    }

    @Test
    @DisplayName("Un espacio mal definido o un presupuesto de cero se rechazan")
    void parametrosInvalidos() {
        var e1 = assertThrows(IllegalArgumentException.class,
                () -> new BusquedaHiperparametros.Espacio(80, 20, 16, 90, 1, 3, 0.2, 0.9,
                        List.of(SOM.Topology.HEX)));
        assertTrue(e1.getMessage().contains("mínimo"), e1.getMessage());

        var e2 = assertThrows(IllegalArgumentException.class,
                () -> new BusquedaHiperparametros.Espacio(20, 80, 16, 90, 1, 3, 0.2, 0.9, List.of()));
        assertTrue(e2.getMessage().contains("topología"), e2.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> BusquedaHiperparametros.muestrear(espacio(), 0, 1));
    }

    @Test
    @DisplayName("La aleatoria explora valores que la parrilla no contempla")
    void exploraFueraDeLaParrilla() {
        Set<Double> tasasParrilla = new HashSet<>();
        for (var c : BusquedaHiperparametros.parrillaPorDefecto()) tasasParrilla.add(c.tasaAprendizaje());

        Set<Double> nuevas = new HashSet<>();
        for (var c : BusquedaHiperparametros.muestrear(espacio(), 50, 3)) {
            if (!tasasParrilla.contains(c.tasaAprendizaje())) nuevas.add(c.tasaAprendizaje());
        }
        assertFalse(nuevas.isEmpty(),
                "la gracia de la aleatoria es probar tasas que la parrilla no tiene");
    }

    @Test
    @DisplayName("¿Con cuántas evaluaciones alcanza la aleatoria a la parrilla completa?")
    void cuantasEvaluacionesHacenFalta() {
        Dataset iris = GestorTxt.getIrisDataset();

        var parrilla = BusquedaHiperparametros.parrillaPorDefecto();
        double mejorParrilla = BusquedaHiperparametros
                .buscar(iris, parrilla, 5, 1, null).get(0).acierto();

        System.out.printf("=== parrilla completa: %d evaluaciones -> %.1f%% ===%n",
                parrilla.size(), mejorParrilla);
        System.out.println("presupuesto | mejor acierto (media de 5 semillas) | veces que iguala o supera");

        for (int presupuesto : new int[]{10, 20, 40}) {
            List<Double> mejores = new ArrayList<>();
            int alcanza = 0;
            for (long semilla = 1; semilla <= 5; semilla++) {
                double mejor = BusquedaHiperparametros
                        .buscarAleatorio(iris, espacio(), presupuesto, 5, semilla, null)
                        .get(0).acierto();
                mejores.add(mejor);
                if (mejor >= mejorParrilla - 1e-9) alcanza++;
            }
            double media = mejores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            System.out.printf("    %3d      |            %.1f%%                 |      %d de 5%n",
                    presupuesto, media, alcanza);
        }

        // Con 40 sorteos —una cuarta parte de la parrilla— debería quedarse cerca.
        double con40 = BusquedaHiperparametros
                .buscarAleatorio(iris, espacio(), 40, 5, 1, null).get(0).acierto();
        assertTrue(con40 > mejorParrilla - 3.0,
                "con 40 evaluaciones debería acercarse: " + con40 + " vs " + mejorParrilla);
    }
}
