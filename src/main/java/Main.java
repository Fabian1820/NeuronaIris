public class Main {
    public static void main(String[] args) {

        SOM som = new SOM(10, 0.1, 100);

        CargadorDatosFlor cargador = new CargadorDatosFlor();

        List<Flor> flores = null;
        try {
            flores = cargador.cargarDatosFlor("ruta/a/tus/datos.txt");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        FABIAN FDEZ, [24/03/2024 19:41]

        som.entrenar(flores);

        EscritorGrupos escritor = new EscritorGrupos();
        try {
            escritor.escribirGrupos(som.obtenerNeuronas(), "ruta/a/tu/salida.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        ContadorGrupos contador = new ContadorGrupos();
        int cantidadGrupos = contador.contarGrupos(som.obtenerNeuronas());
        System.out.println("Cantidad de grupos formadosS: " + cantidadGrupos);

        FABIAN FDEZ, [24/03/2024 19:42]

        try {
            Files.write(Paths.get("ruta/a/tu/configuracion.txt"), Collections.singleton(String.valueOf(cantidadGrupos)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

