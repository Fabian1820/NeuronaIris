package panal.data;

import panal.SOM;
import panal.data.BusquedaHiperparametros.Config;
import panal.data.BusquedaHiperparametros.Espacio;
import panal.data.BusquedaHiperparametros.Resultado;
import panal.utiles.GestorTxt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Búsqueda bayesiana (TPE) frente a la aleatoria.
 *
 * El orden de las pruebas importa: primero se comprueba que el optimizador
 * funciona contra una función de óptimo conocido, y solo después se mide sobre
 * el Iris. Sin esa separación, un mal resultado sobre el Iris no distingue
 * entre "el algoritmo está mal" y "este problema no da para más".
 */
class BusquedaBayesianaTest {

    private Espacio espacio() {
        return BusquedaHiperparametros.espacioPorDefecto();
    }

    // ---------- contrato ----------

    @Test
    @DisplayName("Un presupuesto o un arranque que no dan ni para empezar se rechazan")
    void parametrosInvalidos() {
        var e = espacio();
        assertThrows(IllegalArgumentException.class,
                () -> BusquedaBayesiana.optimizar(e, 1, 10, 1, c -> nota(c, 0), null));
        assertThrows(IllegalArgumentException.class,
                () -> BusquedaBayesiana.optimizar(e, 20, 1, 1, c -> nota(c, 0), null));
    }

    @Test
    @DisplayName("Gasta el presupuesto exacto, sin repetir configuración y sin salirse del espacio")
    void gastaElPresupuestoEnCosasNuevas() {
        Espacio e = espacio();
        List<Resultado> res = BusquedaBayesiana.optimizar(e, 30, 10, 4,
                c -> nota(c, sintetica(c)), null);

        assertEquals(30, res.size());

        List<Config> cs = res.stream().map(Resultado::config).toList();
        assertEquals(30, new HashSet<>(cs).size(), "evaluar dos veces lo mismo es tirar presupuesto");

        for (Config c : cs) {
            assertTrue(c.epocas() >= e.epocasMin() && c.epocas() <= e.epocasMax());
            assertTrue(c.neuronas() >= e.neuronasMin() && c.neuronas() <= e.neuronasMax());
            assertTrue(c.radio() >= e.radioMin() && c.radio() <= e.radioMax());
            assertTrue(c.tasaAprendizaje() >= e.tasaMin() && c.tasaAprendizaje() <= e.tasaMax());
            assertTrue(e.topologias().contains(c.topologia()));
            assertTrue(c.neuronas() >= 4, "una rejilla necesita al menos 4 neuronas");
        }
    }

    @Test
    @DisplayName("Devuelve lo mejor primero")
    void ordenado() {
        List<Resultado> res = BusquedaBayesiana.optimizar(espacio(), 20, 10, 5,
                c -> nota(c, sintetica(c)), null);
        for (int i = 1; i < res.size(); i++) {
            assertTrue(res.get(i - 1).acierto() >= res.get(i).acierto());
        }
    }

    @Test
    @DisplayName("Con la misma semilla recorre el mismo camino")
    void reproducible() {
        assertEquals(configs(BusquedaBayesiana.optimizar(espacio(), 25, 10, 7, c -> nota(c, sintetica(c)), null)),
                configs(BusquedaBayesiana.optimizar(espacio(), 25, 10, 7, c -> nota(c, sintetica(c)), null)));
    }

    // ---------- ¿de verdad optimiza? ----------

    @Test
    @DisplayName("Sobre una función con óptimo conocido le saca ventaja clara al azar")
    void optimizaCuandoHaySenal() {
        Espacio e = espacio();
        int semillas = 30, presupuesto = 40;
        double sumaTpe = 0, sumaAzar = 0;
        int gana = 0;

        for (long s = 1; s <= semillas; s++) {
            double tpe = BusquedaBayesiana
                    .optimizar(e, presupuesto, 10, s, c -> nota(c, sintetica(c)), null)
                    .get(0).acierto();

            double azar = BusquedaHiperparametros.muestrear(e, presupuesto, s).stream()
                    .mapToDouble(BusquedaBayesianaTest::sintetica).max().orElse(0);

            sumaTpe += tpe;
            sumaAzar += azar;
            if (tpe > azar) gana++;
        }

        double mediaTpe = sumaTpe / semillas, mediaAzar = sumaAzar / semillas;
        System.out.printf("función sintética (óptimo 100): azar %.1f · TPE %.1f · gana %d de %d%n",
                mediaAzar, mediaTpe, gana, semillas);

        assertTrue(mediaTpe > mediaAzar + 3.0,
                "si hay estructura que aprender, TPE debería despegarse: "
                        + mediaTpe + " vs " + mediaAzar);
        assertTrue(gana >= semillas * 3 / 4, "debería ganar en la gran mayoría de semillas: " + gana);
    }

    // ---------- y sobre el Iris ----------

    @Test
    @DisplayName("En el Iris el ruido de la métrica es del tamaño del margen que hay que ganar")
    void elRuidoTapaLaSenal() {
        Dataset iris = GestorTxt.getIrisDataset();
        List<String> etiquetas = new ArrayList<>(iris.labels());
        Config c = BusquedaHiperparametros.parrillaPorDefecto().get(0);

        List<Double> repeticiones = new ArrayList<>();
        for (long s = 1; s <= 12; s++) {
            repeticiones.add(BusquedaHiperparametros.evaluar(iris, c, 5, s, etiquetas).acierto());
        }
        double media = repeticiones.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double ruido = Math.sqrt(repeticiones.stream()
                .mapToDouble(x -> (x - media) * (x - media)).average().orElse(0));

        List<Double> parrilla = BusquedaHiperparametros
                .buscar(iris, BusquedaHiperparametros.parrillaPorDefecto(), 5, 1, null)
                .stream().map(Resultado::acierto).sorted().toList();
        double mejor = parrilla.get(parrilla.size() - 1);
        double mediana = parrilla.get(parrilla.size() / 2);

        System.out.printf("misma config, 12 particiones: %.1f a %.1f (sd %.2f)%n",
                repeticiones.stream().mapToDouble(Double::doubleValue).min().orElse(0),
                repeticiones.stream().mapToDouble(Double::doubleValue).max().orElse(0), ruido);
        System.out.printf("margen real de la parrilla: mediana %.1f -> mejor %.1f (%.2f puntos)%n",
                mediana, mejor, mejor - mediana);

        // Este es el hallazgo: reevaluar la misma configuración se mueve tanto
        // como separa a una configuración mediana de la mejor de las 162. Por
        // eso ningún buscador que ordene por acierto puede afinar mucho más.
        assertTrue(ruido > (mejor - mediana) * 0.4,
                "el ruido debería ser comparable al margen: " + ruido + " vs " + (mejor - mediana));
    }

    @Test
    @DisplayName("En el Iris TPE no le gana al azar: no hay estructura que aprender")
    void enElIrisEmpataConElAzar() {
        Dataset iris = GestorTxt.getIrisDataset();
        Espacio e = espacio();
        int semillas = 10, presupuesto = 40;
        double sumaTpe = 0, sumaAzar = 0;

        for (long s = 1; s <= semillas; s++) {
            sumaTpe += BusquedaBayesiana.buscar(iris, e, presupuesto, 10, 5, s, null).get(0).acierto();
            sumaAzar += BusquedaHiperparametros.buscarAleatorio(iris, e, presupuesto, 5, s, null)
                    .get(0).acierto();
        }
        double tpe = sumaTpe / semillas, azar = sumaAzar / semillas;
        System.out.printf("Iris, %d evaluaciones: azar %.2f%% · TPE %.2f%% (%+.2f)%n",
                presupuesto, azar, tpe, tpe - azar);

        // No se le exige ganar —medido, no gana— pero sí no ser peor: si TPE
        // quedara claramente por debajo, sería que está persiguiendo ruido.
        assertTrue(tpe > azar - 0.5,
                "TPE no debería quedar por debajo del azar: " + tpe + " vs " + azar);
    }

    @Test
    @DisplayName("A igual coste, bajando el ruido TPE sí le gana al azar")
    void conMenosRuidoLeGanaAlAzar() {
        Dataset iris = GestorTxt.getIrisDataset();
        List<String> etiquetas = new ArrayList<>(iris.labels());
        Espacio e = espacio();

        // Mismo gasto en ambos: 13 configuraciones por 3 particiones cada una.
        int semillas = 15, presupuesto = 13, repeticiones = 3;
        double sumaTpe = 0, sumaAzar = 0;
        int gana = 0, pierde = 0;

        for (long s = 1; s <= semillas; s++) {
            Config porTpe = BusquedaBayesiana
                    .buscar(iris, e, presupuesto, 5, 5, repeticiones, s, null)
                    .get(0).config();

            Config porAzar = null;
            double mejor = -1;
            for (Config c : BusquedaHiperparametros.muestrear(e, presupuesto, s)) {
                double v = BusquedaHiperparametros
                        .evaluarRepetido(iris, c, 5, repeticiones, s, etiquetas).acierto();
                if (v > mejor) { mejor = v; porAzar = c; }
            }

            // Juicio con particiones que ningún buscador usó: si se puntuara con
            // las mismas, se estaría premiando al que mejor explotó su suerte.
            double notaTpe = juzgar(iris, porTpe, etiquetas);
            double notaAzar = juzgar(iris, porAzar, etiquetas);
            sumaTpe += notaTpe;
            sumaAzar += notaAzar;
            if (notaTpe > notaAzar + 1e-9) gana++;
            else if (notaTpe < notaAzar - 1e-9) pierde++;
        }

        double tpe = sumaTpe / semillas, azar = sumaAzar / semillas;
        System.out.printf("Iris a igual coste (13 x 3 particiones), juzgado aparte:%n");
        System.out.printf("  azar %.2f%% · TPE %.2f%% (%+.2f) · TPE gana %d, pierde %d de %d%n",
                azar, tpe, tpe - azar, gana, pierde, semillas);

        // Sobre 50 semillas la diferencia medida fue +0,23 puntos, IC 95% de
        // +0,07 a +0,39. Aquí se comprueba lo que aguanta con menos semillas:
        // que gane más veces de las que pierde.
        assertTrue(gana > pierde,
                "debería ganar más veces de las que pierde: " + gana + " vs " + pierde);
        assertTrue(tpe > azar - 0.1, "y no quedar por debajo de media: " + tpe + " vs " + azar);
    }

    // ---------- utilidades ----------

    /** Función suave con un único óptimo, para comprobar que el motor converge. */
    private static double sintetica(Config c) {
        double d = Math.abs(c.epocas() - 70) / 60.0
                + Math.abs(c.neuronas() - 30) / 74.0
                + Math.abs(c.radio() - 3) / 2.0
                + Math.abs(c.tasaAprendizaje() - 0.85) / 0.7;
        if (c.topologia() != SOM.Topology.HEX) d += 0.5;
        return 100 - 20 * d;
    }

    private static Resultado nota(Config c, double valor) {
        return new Resultado(c, valor, 0, 0, 0);
    }

    /** Nota independiente: particiones 101 a 110, que la búsqueda no usa. */
    private static double juzgar(Dataset iris, Config c, List<String> etiquetas) {
        double suma = 0;
        for (long s = 101; s <= 110; s++) {
            suma += BusquedaHiperparametros.evaluar(iris, c, 5, s, etiquetas).acierto();
        }
        return suma / 10;
    }

    private static List<Config> configs(List<Resultado> res) {
        return res.stream().map(Resultado::config).toList();
    }
}
