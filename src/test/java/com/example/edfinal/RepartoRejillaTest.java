package com.example.edfinal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El botón 2-D MAP reparte las neuronas del formulario en una rejilla. */
class RepartoRejillaTest {

    @Test
    @DisplayName("Un número compuesto se reparte exacto y lo más cuadrado posible")
    void repartoExacto() {
        assertArrayEquals(new int[]{5, 6}, SOM.rejillaPara(30));
        assertArrayEquals(new int[]{6, 6}, SOM.rejillaPara(36));
        assertArrayEquals(new int[]{4, 5}, SOM.rejillaPara(20));
        assertArrayEquals(new int[]{2, 2}, SOM.rejillaPara(4));
    }

    @Test
    @DisplayName("Un número primo no deja una rejilla degenerada de una fila")
    void repartoDePrimos() {
        for (int primo : new int[]{7, 11, 13, 29, 31}) {
            int[] r = SOM.rejillaPara(primo);
            assertTrue(r[0] >= 2 && r[1] >= 2,
                    primo + " dio una rejilla de " + r[0] + "x" + r[1] + ", que no es 2-D");
        }
    }

    @Test
    @DisplayName("La rejilla resultante siempre es válida para el constructor de SOM")
    void siempreConstruible() {
        for (int n = 4; n <= 120; n++) {
            int[] r = SOM.rejillaPara(n);
            assertTrue(r[0] >= 2 && r[1] >= 2, "n=" + n + " -> " + r[0] + "x" + r[1]);
            assertTrue(r[0] * r[1] >= n,
                    "n=" + n + ": la rejilla no debe perder neuronas (" + r[0] * r[1] + ")");
        }
    }
}
