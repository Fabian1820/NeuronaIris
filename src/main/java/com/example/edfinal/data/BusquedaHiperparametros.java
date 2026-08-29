package com.example.edfinal.data;

import com.example.edfinal.RandomFeaturesPicker;
import com.example.edfinal.SOM;
import com.example.edfinal.SOMNeuron;
import com.example.edfinal.utiles.BMUStock;

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

    /** Crea el mapa que describe una configuración. */
    public static SOM construir(Config c, Dataset datos) {
        if (c.topologia() == SOM.Topology.RING) {
            return new SOM(c.epocas(), c.neuronas(), c.tasaAprendizaje(), c.radio(), datos);
        }
        int[] rejilla = SOM.rejillaPara(c.neuronas());
        return new SOM(c.epocas(), rejilla[0], rejilla[1], c.tasaAprendizaje(), c.radio(),
                datos, c.topologia());
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
