package panal.utiles;

import panal.Flower;
import panal.data.Dataset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import panal.data.Sample;
import panal.SOM;
import panal.SOMNeuron;
import cu.edu.cujae.ceis.graph.vertex.Vertex;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class GestorTxt {
    /** El dataset viaja dentro del jar, no en una ruta relativa al directorio de trabajo. */
    private static final String RECURSO_IRIS = "/panal/iris.data";

    /**
     * Carpeta donde la app guarda su estado (mapas y configuraciones).
     *
     * Antes se escribía en el directorio de trabajo, que en una aplicación
     * empaquetada puede ser de solo lectura o cualquier sitio.
     */
    public static File carpetaDeEstado()
    {
        File dir = new File(System.getProperty("user.home"), ".panal-som");
        if (!dir.isDirectory()) dir.mkdirs();
        return dir;
    }

    /** Ruta de un fichero de estado dentro de la carpeta de la app. */
    public static String archivoDeEstado(String nombre)
    {
        return new File(carpetaDeEstado(), nombre).getAbsolutePath();
    }

    private static ArrayList<Flower> flowersDataBase;
    private static ArrayList<Flower> lista;

    private static Dataset irisDataset;

    public static ArrayList<Flower> getDataBase()
    {
        if(flowersDataBase==null)
        {
            flowersDataBase = loadIris();
        }
        return flowersDataBase;
    }

    /** Lee el Iris del classpath, así funciona igual desde el IDE que dentro del jar. */
    private static ArrayList<Flower> loadIris()
    {
        InputStream in = GestorTxt.class.getResourceAsStream(RECURSO_IRIS);
        if (in == null)
        {
            throw new IllegalStateException("No se encuentra el dataset en " + RECURSO_IRIS);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            return parse(br);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not read the dataset", e);
        }
    }

    /** El Iris como Dataset genérico (lo que consume el mapa). */
    public static Dataset getIrisDataset()
    {
        if (irisDataset == null)
        {
            irisDataset = new Dataset(new ArrayList<Sample>(getDataBase()), Flower.FEATURE_NAMES);
        }
        return irisDataset;
    }

    /** Carga cualquier CSV numérico como dataset. */
    public static Dataset loadDataset(String ruta) throws IOException
    {
        return Dataset.fromCsv(ruta);
    }

    public static ArrayList<Flower> getFile(String path)
    {
        lista = load(path);

        return lista;
    }



    /** Lee flores de un fichero suelto elegido por el usuario. */
    private static ArrayList<Flower> load(String path){
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return parse(br);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /** Una flor por línea: cuatro medidas y la especie, separadas por comas. */
    private static ArrayList<Flower> parse(BufferedReader bufer) throws IOException {
        ArrayList<Flower> flowers = new ArrayList<>();
        String linea;
        while((linea=bufer.readLine())!=null)
        {
            if (linea.isBlank()) continue;
            String[] partes = linea.split(",");
            if (partes.length < 5) continue;

            double sepalLength = Double.parseDouble(partes[0]);
            double sepalWidth = Double.parseDouble(partes[1]);
            double petalLength = Double.parseDouble(partes[2]);
            double petalWidth = Double.parseDouble(partes[3]);
            String type = partes[4];

            flowers.add(new Flower(sepalLength, sepalWidth, petalLength, petalWidth, type));
        }
        return flowers;
    }

    /** Nombre del fichero donde se guarda el mapa entrenado. */
    private static final String MAPA = "mapa.som";

    /**
     * Guarda el mapa en texto plano.
     *
     * El formato anterior era binario y escribía exactamente cuatro doubles por
     * neurona, así que solo servía para el Iris y no había forma de mirarlo. Este
     * es inspeccionable con cualquier editor, admite cualquier número de
     * variables y conserva la topología.
     */
    public static void writeMap(SOM map) throws IOException {
        try (PrintWriter pw = new PrintWriter(archivoDeEstado(MAPA), StandardCharsets.UTF_8))
        {
            pw.println("# Panal SOM · mapa autoorganizado");
            pw.println("version=1");
            pw.println("topology=" + map.getTopology());
            pw.println("rows=" + map.getRows());
            pw.println("cols=" + map.getCols());
            pw.println("epochs=" + map.getEpochs());
            pw.println("neurons=" + map.getTotalNeurons());
            pw.println("learningRate=" + map.getInitialLearningRate());
            pw.println("radius=" + map.getRadious());
            pw.println("features=" + String.join(",", map.getDataset().getFeatureNames()));
            pw.println("# id,peso1,peso2,...");

            for (Vertex v : map.getVerticesList())
            {
                SOMNeuron n = (SOMNeuron) v;
                StringBuilder sb = new StringBuilder().append(n.getId());
                Sample w = n.getWeights();
                for (int i = 0; i < w.size(); i++) sb.append(',').append(w.get(i));
                pw.println(sb);
            }
        }
    }

    /** ¿Hay un mapa guardado? */
    public static boolean haySavedMap() {
        File f = new File(archivoDeEstado(MAPA));
        return f.isFile() && f.length() > 0;
    }

    public static SOM loadMap() throws IOException {
        Map<String, String> cab = new HashMap<>();
        List<Sample> pesos = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivoDeEstado(MAPA)), StandardCharsets.UTF_8)))
        {
            String linea;
            while ((linea = br.readLine()) != null)
            {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                int igual = linea.indexOf('=');
                if (igual > 0 && !linea.contains(","))
                {
                    cab.put(linea.substring(0, igual), linea.substring(igual + 1));
                    continue;
                }
                if (igual > 0 && linea.startsWith("features="))
                {
                    cab.put("features", linea.substring(igual + 1));
                    continue;
                }

                String[] partes = linea.split(",");
                ids.add(Integer.parseInt(partes[0].trim()));
                double[] w = new double[partes.length - 1];
                for (int i = 0; i < w.length; i++) w[i] = Double.parseDouble(partes[i + 1].trim());
                pesos.add(new Sample(w));
            }
        }

        if (pesos.isEmpty()) throw new IOException("El fichero de mapa no tiene neuronas");

        int epochs = Integer.parseInt(cab.getOrDefault("epochs", "1"));
        double lr = Double.parseDouble(cab.getOrDefault("learningRate", "0.5"));
        int radius = Integer.parseInt(cab.getOrDefault("radius", "1"));
        SOM.Topology topologia;
        try {
            topologia = SOM.Topology.valueOf(cab.getOrDefault("topology", "RING"));
        } catch (IllegalArgumentException e) {
            topologia = SOM.Topology.RING;
        }
        boolean rejilla = topologia != SOM.Topology.RING;
        int rows = Integer.parseInt(cab.getOrDefault("rows", "0"));
        int cols = Integer.parseInt(cab.getOrDefault("cols", "0"));

        SOM m = rejilla
                ? new SOM(epochs, rows, cols, lr, radius, getIrisDataset(), topologia)
                : new SOM(epochs, pesos.size(), lr, radius, getIrisDataset());

        boolean esIris = pesos.get(0).size() == 4;
        for (int i = 0; i < pesos.size(); i++)
        {
            Sample w = pesos.get(i);
            m.getVerticesList().add(new SOMNeuron(ids.get(i), esIris ? Flower.from(w) : w));
        }
        m.makeConnections();
        m.setInit(true);
        m.setTrained(true);

        // Sin esto el mapa cargado no sabe a qué especie corresponde cada neurona
        // y clasificar devolvía vacío.
        m.groupBmus(m.getDataset().getSamples());
        m.labelNeurons(m.getDataset().getSamples());
        return m;
    }

    public static void writeItemB() {
        try {
            PrintWriter pw = new PrintWriter(archivoDeEstado("incisoB.txt"));
            for(BMUandFlowers baf : BMUandFManager.getLista())
            {
                pw.write("BMU: " + baf.bmu.getId()+" ");
                double sepalLength = ((Flower)baf.bmu.getInfo()).getSepalLength();
                double sepalWidth = ((Flower)baf.bmu.getInfo()).getSepalWidth();
                double petalLength = ((Flower)baf.bmu.getInfo()).getPetalLength();
                double petalWidth = ((Flower)baf.bmu.getInfo()).getPetalWidth();
                pw.print(sepalLength +","+ sepalWidth +","+ petalLength +","+ petalWidth +"  ");
                pw.println(baf.type);
                for(Flower f : baf.flores)
                {
                    double sepalLengthF = f.getSepalLength();
                    double sepalWidthF = f.getSepalWidth();
                    double petalLengthF = f.getPetalLength();
                    double petalWidthF = f.getPetalWidth();
                    pw.println(sepalLengthF +","+ sepalWidthF +","+ petalLengthF +","+ petalWidthF);
                }
                pw.println();
            }
            pw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    public static void writeInConfig(SOMNeuron n) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(archivoDeEstado("Configuration.dat"),"rw");
        raf.skipBytes(20);
        long pos = raf.getFilePointer();
        int cantIdent = raf.readInt();
        if(cantIdent!=0)
            raf.skipBytes(36*cantIdent);
        raf.writeInt(n.getId());
        raf.writeDouble(((Flower)n.getInfo()).getSepalLength());
        raf.writeDouble(((Flower)n.getInfo()).getSepalWidth());
        raf.writeDouble(((Flower)n.getInfo()).getPetalLength());
        raf.writeDouble(((Flower)n.getInfo()).getPetalWidth());
        raf.seek(pos);
        raf.writeInt(++cantIdent);
        raf.close();
    }

    public static void writeHeaderConfig(SOM map) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(archivoDeEstado("Configuration.dat"),"rw");
        raf.setLength(0);
        raf.writeInt(map.getEpochs());
        raf.writeInt(map.getTotalNeurons());
        raf.writeDouble(map.getInitialLearningRate());
        raf.writeInt(map.getRadious());
        raf.writeInt(0);

        raf.close();
    }

    public static ArrayList<SOMNeuron> readConfig() throws IOException {
        RandomAccessFile raf = new RandomAccessFile(archivoDeEstado("Configuration.dat"),"rw");
        ArrayList<SOMNeuron> list = new ArrayList<>();
        raf.skipBytes(20);
        int cant = raf.readInt();
        while (cant>0)
        {
            int id = raf.readInt();
            double sepalLength = raf.readDouble();
            double sepalWidth = raf.readDouble();
            double petalLength = raf.readDouble();
            double petalWidth = raf.readDouble();
            list.add(new SOMNeuron(id, new Flower(sepalLength,sepalWidth,petalLength,petalWidth)));
            cant--;
        }
        raf.close();
        return list;

    }
}
