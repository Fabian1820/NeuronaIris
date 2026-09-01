package panal.data;

import panal.RandomFeaturesPicker;
import panal.SOM;
import panal.SOMNeuron;
import panal.utiles.BMUStock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Barrido de hiperparámetros: prueba combinaciones y devuelve las mejores.
 *
 * Cada combinación se puntúa por **validación cruzada**, no sobre los datos con
 * los que se entrena. Sin eso el barrido premiaría a la configuración que mejor
 * memoriza, que es justo la que peor generaliza.
 */
public class BusquedaHiperparametros {

    private BusquedaHiperparametros() {}

    /** Una combinación concreta de parámetros del mapa. */
    public record Config(int epocas, int neuronas, int radio, double tasaAprendizaje,
                         SOM.Topology topologia) {

        @Override
        public String toString() {
            return String.format("%s %d neuronas · %d épocas · radio %d · lr %.2f",
                    topologia, neuronas, epocas, radio, tasaAprendizaje);
        }
    }

    /** Lo que rindió una combinación. */
    public record Resultado(Config config, double acierto, double desviacion,
                            double cuantizacion, double topografico) {}

    /**
     * Producto cartesiano de los valores dados.
     *
     * Se descartan las combinaciones imposibles —una rejilla necesita al menos 4
     * neuronas— en vez de dejar que revienten a mitad del barrido.
     */
    public static List<Config> parrilla(List<Integer> epocas, List<Integer> neuronas,
                                        List<Integer> radios, List<Double> tasas,
                                        List<SOM.Topology> topologias) {
        List<Config> out = new ArrayList<>();
        for (SOM.Topology t : topologias) {
            for (int n : neuronas) {
                if (t != SOM.Topology.RING && n < 4) continue;
                for (int e : epocas) {
                    for (int r : radios) {
                        for (double lr : tasas) out.add(new Config(e, n, r, lr, t));
                    }
                }
            }
        }
        return out;
    }

    /**
     * Evalúa cada combinación con validación cruzada y las devuelve ordenadas de
     * mejor a peor acierto.
     *
     * @param progreso se le avisa tras cada combinación (puede ser null)
     */
    public static List<Resultado> buscar(Dataset datos, List<Config> candidatos,
                                         int pliegues, long semilla,
                                         Consumer<Resultado> progreso) {
        if (candidatos.isEmpty()) {
            throw new IllegalArgumentException("No hay ninguna combinación que probar");
        }

        List<Resultado> resultados = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>(datos.labels());

        for (Config c : candidatos) {
            Resultado r = evaluar(datos, c, pliegues, semilla, etiquetas);
            resultados.add(r);
            if (progreso != null) progreso.accept(r);
        }

        resultados.sort(Comparator
                .comparingDouble(Resultado::acierto).reversed()
                // A igualdad de acierto, el mapa que mejor representa los datos.
                .thenComparingDouble(Resultado::cuantizacion));
        return resultados;
    }

    /** Puntúa una combinación promediando los pliegues. */
    public static Resultado evaluar(Dataset datos, Config c, int pliegues, long semilla,
                                    List<String> etiquetas) {
        List<Double> aciertos = new ArrayList<>();
        double sumaCuant = 0, sumaTopo = 0;
        int mapasConTopografico = 0;

        for (Dataset[] par : datos.kFold(pliegues, semilla)) {
            RandomFeaturesPicker.setSeed(semilla);
            BMUStock.clear();

            SOM som = construir(c, par[0]);
            som.initialize();
            som.train();

            aciertos.add(SOMAnalysis.accuracy(
                    SOMAnalysis.confusionMatrix(som, par[1].getSamples(), etiquetas)));

            double err = 0;
            for (Sample s : par[1].getSamples()) {
                err += ((SOMNeuron) som.findBMU(s)).euclidianDistance(s);
            }
            sumaCuant += err / par[1].size();

            if (som.esRejilla()) {
                sumaTopo += SOMAnalysis.topographicError(som, par[1].getSamples(), true) * 100;
                mapasConTopografico++;
            }
        }

        double media = aciertos.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double varianza = aciertos.stream()
                .mapToDouble(a -> (a - media) * (a - media)).average().orElse(0);

        return new Resultado(c, media, Math.sqrt(varianza),
                sumaCuant / pliegues,
                mapasConTopografico == 0 ? Double.NaN : sumaTopo / mapasConTopografico);
    }

    /**
     * Puntúa una combinación repitiendo la validación cruzada con particiones
     * distintas y promediando.
     *
     * Sale caro —cuesta {@code repeticiones} veces más— pero baja el ruido de la
     * medición, y sobre el Iris ese ruido es del mismo tamaño que el margen que
     * separa a una configuración mediana de la mejor. Con una sola partición, el
     * ranking premia a la que le tocó la partición favorable.
     *
     * La desviación que se devuelve sigue siendo la de pliegue a pliegue, que es
     * lo que muestra la interfaz; se promedia entre repeticiones.
     */
    public static Resultado evaluarRepetido(Dataset datos, Config c, int pliegues,
                                            int repeticiones, long semilla,
                                            List<String> etiquetas) {
        if (repeticiones < 1) {
            throw new IllegalArgumentException("Hay que evaluar al menos una vez");
        }
        double acierto = 0, desviacion = 0, cuantizacion = 0, topografico = 0;
        int conTopografico = 0;

        for (int i = 0; i < repeticiones; i++) {
            Resultado r = evaluar(datos, c, pliegues, semilla + i, etiquetas);
            acierto += r.acierto();
            desviacion += r.desviacion();
            cuantizacion += r.cuantizacion();
            if (!Double.isNaN(r.topografico())) {
                topografico += r.topografico();
                conTopografico++;
            }
        }
        return new Resultado(c, acierto / repeticiones, desviacion / repeticiones,
                cuantizacion / repeticiones,
                conTopografico == 0 ? Double.NaN : topografico / conTopografico);
    }

    /** Crea el mapa que describe una configuración. */
    public static SOM construir(Config c, Dataset datos) {
        if (c.topologia() == SOM.Topology.RING) {
            return new SOM(c.epocas(), c.neuronas(), c.tasaAprendizaje(), c.radio(), datos);
        }
        int[] rejilla = SOM.rejillaPara(c.neuronas());
        return new SOM(c.epocas(), rejilla[0], rejilla[1], c.tasaAprendizaje(), c.radio(),
                datos, c.topologia());
    }

    // ---------- búsqueda aleatoria ----------

    /**
     * Rangos de los que sortear configuraciones.
     *
     * A diferencia de la parrilla, no fija los valores de antemano: la tasa de
     * aprendizaje puede salir 0.37 y no solo 0.3 o 0.5.
     */
    public record Espacio(int epocasMin, int epocasMax,
                          int neuronasMin, int neuronasMax,
                          int radioMin, int radioMax,
                          double tasaMin, double tasaMax,
                          List<SOM.Topology> topologias) {

        public Espacio {
            if (epocasMin > epocasMax || neuronasMin > neuronasMax
                    || radioMin > radioMax || tasaMin > tasaMax) {
                throw new IllegalArgumentException("Algún rango tiene el mínimo por encima del máximo");
            }
            if (topologias.isEmpty()) {
                throw new IllegalArgumentException("Hace falta al menos una topología");
            }
        }
    }

    /**
     * Sortea n configuraciones distintas del espacio.
     *
     * Se descartan las repetidas: con un presupuesto de n evaluaciones, gastar
     * dos en la misma combinación es tirar una.
     */
    public static List<Config> muestrear(Espacio e, int n, long semilla) {
        if (n < 1) throw new IllegalArgumentException("Hay que sortear al menos una configuración");

        java.util.Random rand = new java.util.Random(semilla);
        java.util.LinkedHashSet<Config> vistas = new java.util.LinkedHashSet<>();

        // Cota de intentos: si el espacio es pequeño puede no haber n distintas.
        int intentos = 0, maxIntentos = n * 50;
        while (vistas.size() < n && intentos++ < maxIntentos) {
            SOM.Topology t = e.topologias().get(rand.nextInt(e.topologias().size()));
            int neuronas = entero(rand, e.neuronasMin(), e.neuronasMax());
            if (t != SOM.Topology.RING && neuronas < 4) continue;

            double tasa = Math.round((e.tasaMin()
                    + rand.nextDouble() * (e.tasaMax() - e.tasaMin())) * 100) / 100.0;

            vistas.add(new Config(entero(rand, e.epocasMin(), e.epocasMax()), neuronas,
                    entero(rand, e.radioMin(), e.radioMax()), tasa, t));
        }
        return new ArrayList<>(vistas);
    }

    private static int entero(java.util.Random rand, int min, int max) {
        return min + (max == min ? 0 : rand.nextInt(max - min + 1));
    }

    /** Barrido con n configuraciones sorteadas en vez de una parrilla fija. */
    public static List<Resultado> buscarAleatorio(Dataset datos, Espacio espacio, int n,
                                                  int pliegues, long semilla,
                                                  Consumer<Resultado> progreso) {
        return buscar(datos, muestrear(espacio, n, semilla), pliegues, semilla, progreso);
    }

    /** Espacio por defecto, equivalente en cobertura a la parrilla por defecto. */
    public static Espacio espacioPorDefecto() {
        return new Espacio(20, 80, 16, 90, 1, 3, 0.2, 0.9,
                List.of(SOM.Topology.GRID, SOM.Topology.HEX));
    }

    /** Parrilla por defecto: un barrido razonable sin tardar una eternidad. */
    public static List<Config> parrillaPorDefecto() {
        return parrilla(
                List.of(20, 40, 80),
                List.of(24, 48, 80),
                List.of(1, 2, 3),
                List.of(0.3, 0.5, 0.8),
                List.of(SOM.Topology.GRID, SOM.Topology.HEX));
    }
}
