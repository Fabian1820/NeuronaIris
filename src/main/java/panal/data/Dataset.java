package panal.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * Conjunto de muestras con sus nombres de variable y el rango de cada una.
 *
 * Carga cualquier CSV numérico: toma como variables todas las columnas que
 * parsean a número y como etiqueta la última columna que no lo hace. Así el
 * proyecto deja de estar atado a las cuatro medidas del Iris.
 */
public class Dataset {

    private final List<Sample> samples;
    private final String[] featureNames;
    private final double[] min;
    private final double[] max;

    public Dataset(List<Sample> samples, String[] featureNames) {
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("El dataset está vacío");
        }
        int dim = samples.get(0).size();
        for (Sample s : samples) {
            if (s.size() != dim) {
                throw new IllegalArgumentException(
                        "Todas las muestras deben tener la misma dimensión: se esperaba "
                                + dim + " y llegó una de " + s.size());
            }
        }

        this.samples = samples;
        this.featureNames = featureNames != null && featureNames.length == dim
                ? featureNames
                : nombresPorDefecto(dim);

        this.min = new double[dim];
        this.max = new double[dim];
        for (int i = 0; i < dim; i++) {
            min[i] = Double.MAX_VALUE;
            max[i] = -Double.MAX_VALUE;
        }
        for (Sample s : samples) {
            for (int i = 0; i < dim; i++) {
                min[i] = Math.min(min[i], s.get(i));
                max[i] = Math.max(max[i], s.get(i));
            }
        }
    }

    private static String[] nombresPorDefecto(int dim) {
        String[] n = new String[dim];
        for (int i = 0; i < dim; i++) n[i] = "var" + (i + 1);
        return n;
    }

    // ---------- carga ----------

    public static Dataset fromCsv(String path) throws IOException {
        try (Reader r = new FileReader(path)) {
            return fromReader(r, null);
        }
    }

    public static Dataset fromCsv(String path, String[] featureNames) throws IOException {
        try (Reader r = new FileReader(path)) {
            return fromReader(r, featureNames);
        }
    }

    static Dataset fromReader(Reader reader, String[] featureNames) throws IOException {
        List<Sample> muestras = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            String linea;
            int numeroDeLinea = 0;
            while ((linea = br.readLine()) != null) {
                numeroDeLinea++;
                if (linea.isBlank()) continue;

                String[] partes = linea.split(",");
                List<Double> valores = new ArrayList<>();
                String etiqueta = null;

                for (String parte : partes) {
                    String p = parte.trim();
                    if (p.isEmpty()) continue;
                    try {
                        valores.add(Double.parseDouble(p));
                    } catch (NumberFormatException e) {
                        etiqueta = p;   // columna no numérica: se usa como etiqueta
                    }
                }

                if (valores.isEmpty()) continue;   // cabecera u otra línea sin datos

                double[] v = new double[valores.size()];
                for (int i = 0; i < v.length; i++) v[i] = valores.get(i);
                muestras.add(new Sample(v, etiqueta));
            }
        }
        return new Dataset(muestras, featureNames);
    }

    // ---------- consulta ----------

    public List<Sample> getSamples() {
        return samples;
    }

    public int size() {
        return samples.size();
    }

    public int dimension() {
        return samples.get(0).size();
    }

    public String[] getFeatureNames() {
        return featureNames;
    }

    public double[] getMin() {
        return min.clone();
    }

    public double[] getMax() {
        return max.clone();
    }

    /** Etiquetas presentes, en orden de aparición. */
    public Set<String> labels() {
        Set<String> out = new LinkedHashSet<>();
        for (Sample s : samples) {
            if (s.getLabel() != null) out.add(s.getLabel());
        }
        return out;
    }

    /**
     * Parte el dataset en entrenamiento y prueba respetando la proporción de
     * cada etiqueta (muestreo estratificado).
     *
     * Medir el acierto sobre los mismos datos con los que se entrenó siempre da
     * un número optimista; esto permite medirlo sobre muestras no vistas.
     *
     * @return dos datasets: [entrenamiento, prueba]
     */
    public Dataset[] split(double proporcionEntrenamiento, long semilla) {
        if (proporcionEntrenamiento <= 0 || proporcionEntrenamiento >= 1) {
            throw new IllegalArgumentException(
                    "La proporción de entrenamiento debe estar entre 0 y 1, y llegó "
                            + proporcionEntrenamiento);
        }

        Map<String, List<Sample>> porEtiqueta = new LinkedHashMap<>();
        for (Sample s : samples) {
            porEtiqueta.computeIfAbsent(String.valueOf(s.getLabel()), k -> new ArrayList<>()).add(s);
        }

        java.util.Random rand = new java.util.Random(semilla);
        List<Sample> entrena = new ArrayList<>(), prueba = new ArrayList<>();

        for (List<Sample> grupo : porEtiqueta.values()) {
            List<Sample> copia = new ArrayList<>(grupo);
            java.util.Collections.shuffle(copia, rand);
            int corte = (int) Math.round(copia.size() * proporcionEntrenamiento);
            corte = Math.max(1, Math.min(copia.size() - 1, corte));
            entrena.addAll(copia.subList(0, corte));
            prueba.addAll(copia.subList(corte, copia.size()));
        }

        return new Dataset[]{
                new Dataset(entrena, featureNames),
                new Dataset(prueba, featureNames)
        };
    }

    /**
     * Validación cruzada estratificada de k pliegues.
     *
     * Reparte cada etiqueta por turnos entre los k pliegues, así que todos
     * conservan la proporción de clases. Cada muestra se usa exactamente una vez
     * como prueba, cosa que las particiones 70/30 repetidas no garantizan: ahí
     * una muestra puede caer en prueba muchas veces y otra ninguna.
     *
     * @return k pares {entrenamiento, prueba}
     */
    public Dataset[][] kFold(int k, long semilla) {
        if (k < 2) {
            throw new IllegalArgumentException("La validación cruzada necesita al menos 2 pliegues, y se pidió " + k);
        }

        Map<String, List<Sample>> porEtiqueta = new LinkedHashMap<>();
        for (Sample s : samples) {
            porEtiqueta.computeIfAbsent(String.valueOf(s.getLabel()), x -> new ArrayList<>()).add(s);
        }

        int menor = porEtiqueta.values().stream().mapToInt(List::size).min().orElse(0);
        if (k > menor) {
            throw new IllegalArgumentException("No caben " + k + " pliegues: la etiqueta menos"
                    + " frecuente solo tiene " + menor + " muestras");
        }

        // Reparto por turnos dentro de cada etiqueta: mantiene la proporción.
        List<List<Sample>> pliegues = new ArrayList<>();
        for (int i = 0; i < k; i++) pliegues.add(new ArrayList<>());

        java.util.Random rand = new java.util.Random(semilla);
        for (List<Sample> grupo : porEtiqueta.values()) {
            List<Sample> copia = new ArrayList<>(grupo);
            java.util.Collections.shuffle(copia, rand);
            for (int i = 0; i < copia.size(); i++) pliegues.get(i % k).add(copia.get(i));
        }

        Dataset[][] out = new Dataset[k][2];
        for (int i = 0; i < k; i++) {
            List<Sample> entrena = new ArrayList<>();
            for (int j = 0; j < k; j++) if (j != i) entrena.addAll(pliegues.get(j));
            out[i][0] = new Dataset(entrena, featureNames);
            out[i][1] = new Dataset(pliegues.get(i), featureNames);
        }
        return out;
    }

    /**
     * Copia con cada variable escalada a [0,1].
     *
     * Sin esto, la variable de mayor recorrido domina la distancia euclidiana:
     * en Iris el largo del pétalo se lleva el 70% y el ancho del sépalo el 3%.
     * En Iris no mejora el acierto (esa variable es justo la discriminante),
     * pero en un dataset cualquiera es la diferencia entre medir de verdad y
     * medir solo la columna de números más grandes.
     */
    public Dataset normalized() {
        List<Sample> out = new ArrayList<>(samples.size());
        for (Sample s : samples) {
            double[] v = new double[dimension()];
            for (int i = 0; i < v.length; i++) {
                double rango = max[i] - min[i];
                v[i] = rango == 0 ? 0 : (s.get(i) - min[i]) / rango;
            }
            out.add(new Sample(v, s.getLabel()));
        }
        return new Dataset(out, featureNames);
    }
}
