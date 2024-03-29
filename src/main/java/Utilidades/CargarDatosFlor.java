package Utilidades;

public class CargarDatosFlor {
    public List<Flor> cargarDatosFlor(String rutaArchivo) throws IOException {
        return Files.lines(Paths.get(rutaArchivo))
                .map(linea -> linea.split(","))
                .map(partes -> new Flor(
                        Arrays.stream(partes, 0, 4).mapToDouble(Double::parseDouble).toArray(),
                        partes[4]))
                .collect(Collectors.toList());
    }
}
