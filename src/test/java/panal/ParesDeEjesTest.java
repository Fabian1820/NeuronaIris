package panal;

import panal.ui.PanelDispersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** La interfaz elige qué par de variables dibuja cada gráfica según el dataset. */
class ParesDeEjesTest {

    @Test
    @DisplayName("Siempre devuelve cuatro pares, sea cual sea la dimensión")
    void siempreCuatroPares() {
        for (int dim = 1; dim <= 12; dim++) {
            assertEquals(4, PanelDispersion.paresDeVariables(dim).length, "dim=" + dim);
        }
    }

    @Test
    @DisplayName("Los índices caen siempre dentro del rango de variables")
    void indicesValidos() {
        for (int dim = 1; dim <= 12; dim++) {
            for (int[] par : PanelDispersion.paresDeVariables(dim)) {
                assertTrue(par[0] >= 0 && par[0] < dim, "dim=" + dim + " x=" + par[0]);
                assertTrue(par[1] >= 0 && par[1] < dim, "dim=" + dim + " y=" + par[1]);
            }
        }
    }

    @Test
    @DisplayName("Con cuatro variables da cuatro pares distintos")
    void cuatroVariablesCuatroParesDistintos() {
        int[][] pares = PanelDispersion.paresDeVariables(4);
        var vistos = new java.util.HashSet<String>();
        for (int[] p : pares) vistos.add(p[0] + "-" + p[1]);
        assertEquals(4, vistos.size(), "no deberían repetirse: " + vistos);

        assertArrayEquals(new int[]{0, 1}, pares[0]);
        assertArrayEquals(new int[]{0, 2}, pares[1]);
        assertArrayEquals(new int[]{0, 3}, pares[2]);
        assertArrayEquals(new int[]{1, 2}, pares[3]);
    }

    @Test
    @DisplayName("Con dos variables solo hay un par posible y se repite")
    void dosVariables() {
        for (int[] p : PanelDispersion.paresDeVariables(2)) {
            assertArrayEquals(new int[]{0, 1}, p);
        }
    }

    @Test
    @DisplayName("Con una sola variable se dibuja contra sí misma, sin salirse del rango")
    void unaVariable() {
        for (int[] p : PanelDispersion.paresDeVariables(1)) {
            assertArrayEquals(new int[]{0, 0}, p);
        }
    }
}
