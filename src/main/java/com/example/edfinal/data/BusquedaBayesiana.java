package com.example.edfinal.data;

import com.example.edfinal.SOM;
import com.example.edfinal.data.BusquedaHiperparametros.Config;
import com.example.edfinal.data.BusquedaHiperparametros.Espacio;
import com.example.edfinal.data.BusquedaHiperparametros.Resultado;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Búsqueda bayesiana de hiperparámetros por TPE (Tree-structured Parzen
 * Estimator), el algoritmo de Hyperopt y Optuna.
 *
 * La diferencia con la aleatoria es que aquí cada sorteo aprende de los
 * anteriores. Tras unas cuantas evaluaciones de arranque, se parten las
 * configuraciones ya probadas en dos montones —las que salieron bien y el
 * resto— y se estima con qué frecuencia aparece cada valor en cada montón:
 *
 *   l(x) = densidad del valor x entre las buenas
 *   g(x) = densidad del valor x entre las malas
 *
 * La siguiente candidata es la que maximiza l(x)/g(x): un valor que sale mucho
 * entre las buenas y poco entre las malas. Ese cociente es, salvo constantes,
 * el que maximiza la mejora esperada, y por eso esto es optimización bayesiana
 * de verdad y no un ranking heurístico: l y g son p(x|y), y el cociente sale de
 * aplicarle Bayes.
 *
 * Frente a un proceso gaussiano —la otra vía clásica— TPE tiene dos ventajas
 * aquí: no hay que invertir matrices ni ajustar el núcleo, y admite sin
 * violencia una variable categórica como la topología, que un GP obliga a
 * codificar a mano.
 *
 * Limitación que hereda del método: TPE modela cada hiperparámetro por separado,
 * así que no captura que, por ejemplo, un radio grande solo convenga con muchas
 * neuronas. Las dependencias entre parámetros se le escapan.
 */
public class BusquedaBayesiana {

    private BusquedaBayesiana() {}

    /** Proporción de evaluaciones que cuentan como "buenas". */
    private static final double GAMMA = 0.25;

    /** Candidatas que se sortean de l(x) antes de quedarse con una. */
    private static final int CANDIDATAS = 24;

    /** Evaluaciones al azar antes de empezar a modelar. */
    public static final int ARRANQUE_POR_DEFECTO = 10;

    /**
     * De mejor a peor. El desempate por error de cuantización no es cosmético:
     * los aciertos sobre el Iris empatan constantemente, y sin un criterio fino
     * el reparto entre buenas y malas acabaría siendo arbitrario.
     */
    private static final Comparator<Resultado> MEJOR_PRIMERO = Comparator
            .comparingDouble(Resultado::acierto).reversed()
            .thenComparingDouble(Resultado::cuantizacion);

    /**
     * Optimiza durante {@code presupuesto} evaluaciones y devuelve todo lo
     * probado, ordenado de mejor a peor.
     *
     * @param arranque  evaluaciones iniciales al azar; con menos de dos no hay
     *                  nada que partir en buenas y malas
     * @param progreso  se le avisa tras cada evaluación (puede ser null)
     */
    public static List<Resultado> buscar(Dataset datos, Espacio espacio, int presupuesto,
                                         int arranque, int pliegues, long semilla,
                                         Consumer<Resultado> progreso) {
        if (presupuesto < 2) {
            throw new IllegalArgumentException("El presupuesto no da ni para arrancar");
        }
        if (arranque < 2) {
            throw new IllegalArgumentException("Hacen falta al menos dos evaluaciones de arranque");
        }

        return buscar(datos, espacio, presupuesto, arranque, pliegues, 1, semilla, progreso);
    }

    /**
     * Igual, pero puntuando cada configuración con {@code repeticiones}
     * particiones distintas en vez de una.
     *
     * Es lo que hace que la búsqueda bayesiana sirva de algo aquí: con una sola
     * partición el ranking es medio ruido, y un modelo ajustado a ruido no
     * mejora a sortear al azar. Medido sobre el Iris a igualdad de coste —13
     * configuraciones por 3 particiones frente a 40 por una— la diferencia a
     * favor es de +0,23 puntos de acierto real (IC 95% de +0,07 a +0,39 sobre
     * 50 semillas).
     */
    public static List<Resultado> buscar(Dataset datos, Espacio espacio, int presupuesto,
                                         int arranque, int pliegues, int repeticiones,
                                         long semilla, Consumer<Resultado> progreso) {
        List<String> etiquetas = new ArrayList<>(datos.labels());
        return optimizar(espacio, presupuesto, arranque, semilla,
                c -> BusquedaHiperparametros.evaluarRepetido(
                        datos, c, pliegues, repeticiones, semilla, etiquetas),
                progreso);
    }

    /**
     * El optimizador en crudo, sobre cualquier función que puntúe una
     * configuración.
     *
     * Separado de {@link #buscar} para poder comprobarlo contra funciones de
     * óptimo conocido: si no se puede medir aparte, no hay forma de saber si un
     * mal resultado es del algoritmo o del problema.
     */
    public static List<Resultado> optimizar(Espacio espacio, int presupuesto, int arranque,
                                            long semilla, Function<Config, Resultado> objetivo,
                                            Consumer<Resultado> progreso) {
        if (presupuesto < 2) {
            throw new IllegalArgumentException("El presupuesto no da ni para arrancar");
        }
        if (arranque < 2) {
            throw new IllegalArgumentException("Hacen falta al menos dos evaluaciones de arranque");
        }

        Random rand = new Random(semilla);
        List<Resultado> historia = new ArrayList<>();
        Set<Config> probadas = new LinkedHashSet<>();

        for (Config c : BusquedaHiperparametros.muestrear(espacio,
                Math.min(arranque, presupuesto), semilla)) {
            probadas.add(c);
            Resultado r = objetivo.apply(c);
            historia.add(r);
            if (progreso != null) progreso.accept(r);
        }

        while (historia.size() < presupuesto) {
            Config c = proponer(historia, espacio, probadas, rand);
            probadas.add(c);
            Resultado r = objetivo.apply(c);
            historia.add(r);
            if (progreso != null) progreso.accept(r);
        }

        historia.sort(MEJOR_PRIMERO);
        return historia;
    }

    /** Presupuesto y arranque por defecto. */
    public static List<Resultado> buscar(Dataset datos, Espacio espacio, int presupuesto,
                                         int pliegues, long semilla,
                                         Consumer<Resultado> progreso) {
        return buscar(datos, espacio, presupuesto, ARRANQUE_POR_DEFECTO, pliegues, semilla, progreso);
    }

    // ---------- el motor ----------

    /**
     * Parte lo probado en buenas y malas, sortea candidatas del modelo de las
     * buenas y devuelve la que mejor cociente l/g tiene.
     */
    static Config proponer(List<Resultado> historia, Espacio espacio,
                           Set<Config> probadas, Random rand) {
        List<Resultado> ordenadas = new ArrayList<>(historia);
        ordenadas.sort(MEJOR_PRIMERO);

        // Al menos una buena y una mala: si no, uno de los dos modelos se queda
        // vacío y el cociente deja de significar nada.
        int corte = (int) Math.round(GAMMA * ordenadas.size());
        corte = Math.max(1, Math.min(ordenadas.size() - 1, corte));

        List<Config> buenas = new ArrayList<>();
        List<Config> malas = new ArrayList<>();
        for (int i = 0; i < ordenadas.size(); i++) {
            (i < corte ? buenas : malas).add(ordenadas.get(i).config());
        }

        Modelo l = new Modelo(buenas, espacio);
        Modelo g = new Modelo(malas, espacio);

        Config mejor = null;
        double mejorRatio = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < CANDIDATAS; i++) {
            Config c = l.muestrear(rand);
            if (probadas.contains(c)) continue;      // ya sabemos lo que rinde
            double ratio = l.logDensidad(c) - g.logDensidad(c);
            if (ratio > mejorRatio) {
                mejorRatio = ratio;
                mejor = c;
            }
        }

        // Todas las candidatas repetidas: se cae a un sorteo ciego antes que
        // gastar la evaluación en algo ya medido.
        if (mejor == null) {
            for (int i = 0; i < 200 && mejor == null; i++) {
                Config c = l.muestrearUniforme(rand);
                if (!probadas.contains(c)) mejor = c;
            }
        }
        return mejor != null ? mejor : l.muestrearUniforme(rand);
    }

    /**
     * Densidad estimada sobre un montón de configuraciones.
     *
     * Los parámetros numéricos se modelan con un núcleo gaussiano por
     * observación más una gaussiana ancha centrada en el rango, que hace de
     * previa: sin ella el modelo colapsaría sobre las primeras observaciones y
     * dejaría de explorar. La topología, al ser categórica, va con conteos
     * suavizados (Laplace), que es la posterior de una Dirichlet uniforme.
     */
    private static final class Modelo {

        private final Espacio espacio;
        private final Nucleo epocas, neuronas, radio, tasa;
        private final Map<SOM.Topology, Double> topologias = new HashMap<>();

        Modelo(List<Config> obs, Espacio espacio) {
            this.espacio = espacio;
            this.epocas = new Nucleo(obs, Config::epocas, espacio.epocasMin(), espacio.epocasMax(), true);
            this.neuronas = new Nucleo(obs, Config::neuronas, espacio.neuronasMin(), espacio.neuronasMax(), true);
            this.radio = new Nucleo(obs, Config::radio, espacio.radioMin(), espacio.radioMax(), true);
            this.tasa = new Nucleo(obs, Config::tasaAprendizaje, espacio.tasaMin(), espacio.tasaMax(), false);

            int k = espacio.topologias().size();
            Map<SOM.Topology, Integer> cuenta = new HashMap<>();
            for (Config c : obs) cuenta.merge(c.topologia(), 1, Integer::sum);
            for (SOM.Topology t : espacio.topologias()) {
                topologias.put(t, (cuenta.getOrDefault(t, 0) + 1.0) / (obs.size() + k));
            }
        }

        Config muestrear(Random rand) {
            SOM.Topology t = sortearTopologia(rand);
            return construir((int) epocas.muestrear(rand), (int) neuronas.muestrear(rand),
                    (int) radio.muestrear(rand), redondear(tasa.muestrear(rand)), t);
        }

        /** Sorteo ciego dentro del espacio, sin mirar lo aprendido. */
        Config muestrearUniforme(Random rand) {
            return construir(
                    espacio.epocasMin() + rand.nextInt(espacio.epocasMax() - espacio.epocasMin() + 1),
                    espacio.neuronasMin() + rand.nextInt(espacio.neuronasMax() - espacio.neuronasMin() + 1),
                    espacio.radioMin() + rand.nextInt(espacio.radioMax() - espacio.radioMin() + 1),
                    redondear(espacio.tasaMin() + rand.nextDouble() * (espacio.tasaMax() - espacio.tasaMin())),
                    espacio.topologias().get(rand.nextInt(espacio.topologias().size())));
        }

        private Config construir(int e, int n, int r, double lr, SOM.Topology t) {
            // Una rejilla no se sostiene con menos de cuatro neuronas.
            if (t != SOM.Topology.RING) n = Math.max(4, n);
            return new Config(e, n, r, lr, t);
        }

        private SOM.Topology sortearTopologia(Random rand) {
            double u = rand.nextDouble(), acumulado = 0;
            for (SOM.Topology t : espacio.topologias()) {
                acumulado += topologias.get(t);
                if (u <= acumulado) return t;
            }
            return espacio.topologias().get(espacio.topologias().size() - 1);
        }

        /** log l(x) sumando los parámetros: TPE los trata como independientes. */
        double logDensidad(Config c) {
            return Math.log(epocas.densidad(c.epocas()))
                    + Math.log(neuronas.densidad(c.neuronas()))
                    + Math.log(radio.densidad(c.radio()))
                    + Math.log(tasa.densidad(c.tasaAprendizaje()))
                    + Math.log(topologias.getOrDefault(c.topologia(), 1e-9));
        }

        private static double redondear(double x) {
            return Math.round(x * 100) / 100.0;
        }
    }

    /** Mezcla de gaussianas sobre un parámetro numérico. */
    private static final class Nucleo {

        private final double[] centros;
        private final double[] anchos;
        private final double min, max;
        private final boolean entero;

        Nucleo(List<Config> obs, ToDoubleFunction<Config> valor, double min, double max, boolean entero) {
            this.min = min;
            this.max = max;
            this.entero = entero;

            int m = obs.size();
            double rango = Math.max(max - min, 1e-9);
            // Con pocas observaciones el núcleo tiene que ser ancho, o el modelo
            // se cierra sobre ellas antes de haber explorado nada.
            double ancho = Math.max(rango / (2.0 * Math.sqrt(m + 1.0)),
                    entero ? 0.5 : rango / 50.0);

            this.centros = new double[m + 1];
            this.anchos = new double[m + 1];
            for (int i = 0; i < m; i++) {
                centros[i] = valor.applyAsDouble(obs.get(i));
                anchos[i] = ancho;
            }
            centros[m] = (min + max) / 2;   // la previa
            anchos[m] = rango / 2;
        }

        double densidad(double x) {
            double suma = 0;
            for (int i = 0; i < centros.length; i++) {
                double z = (x - centros[i]) / anchos[i];
                suma += Math.exp(-0.5 * z * z) / (anchos[i] * Math.sqrt(2 * Math.PI));
            }
            return Math.max(suma / centros.length, 1e-12);
        }

        double muestrear(Random rand) {
            int i = rand.nextInt(centros.length);
            double x = centros[i] + rand.nextGaussian() * anchos[i];
            x = Math.max(min, Math.min(max, x));
            return entero ? Math.round(x) : x;
        }
    }
}
