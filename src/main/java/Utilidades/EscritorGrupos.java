package Utilidades;

public class EscritorGrupos {
    public void escribirGrupos(List<Neurona> neuronas, String rutaArchivo) throws IOException {
        BufferedWriter escritor = new BufferedWriter(new FileWriter(rutaArchivo));
        for (Neurona neurona : neuronas) {
            escritor.write("Pesos de la neurona: " + Arrays.toString(neurona.obtenerPesos()));
            escritor.newLine();
            Map<String, Long> conteoTipoFlor = neurona.obtenerFloresAsignadas().stream()
                    .collect(Collectors.groupingBy(Flor::obtenerTipoFlor, Collectors.counting()));
            String tipoFlorMasComun = Collections.max(conteoTipoFlor.entrySet(), Map.Entry.comparingByValue()).getKey();
            escritor.write("Tipo de flor más común: " + tipoFlorMasComun);
            escritor.newLine();
        }
        escritor.close();
    }
}
