package com.example.edfinal;
import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import com.example.edfinal.data.Dataset;
import com.example.edfinal.data.Sample;
import cu.edu.cujae.ceis.graph.LinkedGraph;
import cu.edu.cujae.ceis.graph.vertex.Vertex;

import java.util.*;

public class SOM extends LinkedGraph {

    private double initialLearningRate;
    private double currentLearningRate;
    private int epochs;
    public int totalNeurons;
    private int radious;
    private boolean trained;

    public double getInitialLearningRate() {
        return initialLearningRate;
    }

    public int getEpochs() {
        return epochs;
    }

    public int getTotalNeurons() {
        return totalNeurons;
    }

    public int getRadious() {
        return radious;
    }

    private boolean init;
    private final Map<Integer, String> labels = new HashMap<>();
    private Dataset dataset;

    /** Cómo se conectan las neuronas entre sí. */
    public enum Topology {
        /** Anillo 1-D: cada neurona con sus dos anteriores y dos siguientes. */
        RING,
        /** Rejilla rectangular: 4 vecinas (arriba, abajo, izquierda, derecha). */
        GRID,
        /**
         * Rejilla hexagonal: hasta 6 vecinas, con las filas impares desplazadas
         * media celda. Es la que usan las implementaciones de referencia de SOM,
         * porque reparte la vecindad de forma más uniforme: en una rejilla
         * rectangular las cuatro vecinas están a distancia 1 pero las cuatro
         * diagonales a √2, mientras que en la hexagonal las seis equidistan.
         */
        HEX
    }

    private Topology topology = Topology.RING;
    private boolean shrinkRadius = false;
    private int rows = 0;
    private int cols = 0;

    /** Mapa sobre el dataset Iris (comportamiento histórico). */
    public SOM(int epochs, int neurons, double learningRate, int radius)
    {
      this(epochs, neurons, learningRate, radius, null);
    }

    /**
     * Mapa con topología de rejilla 2-D: cada neurona se conecta con las de
     * arriba, abajo, izquierda y derecha. Es la forma canónica del SOM y la que
     * permite dibujar U-matrix y planos de componentes.
     */
    public SOM(int epochs, int rows, int cols, double learningRate, int radius, Dataset dataset)
    {
      this(epochs, rows, cols, learningRate, radius, dataset, Topology.GRID);
    }

    /** Mapa 2-D con la topología indicada: rejilla rectangular o hexagonal. */
    public SOM(int epochs, int rows, int cols, double learningRate, int radius, Dataset dataset,
               Topology topology)
    {
      this(epochs, rows * cols, learningRate, radius, dataset);
      if (rows < 2 || cols < 2) {
          throw new IllegalArgumentException("Una rejilla necesita al menos 2x2 y se pidió " + rows + "x" + cols);
      }
      if (topology != Topology.GRID && topology != Topology.HEX) {
          throw new IllegalArgumentException("Una rejilla 2-D es GRID o HEX, y se pidió " + topology);
      }
      this.topology = topology;
      this.rows = rows;
      this.cols = cols;
    }

    /**
     * Reparte n neuronas en la rejilla más cuadrada posible.
     *
     * Si n es primo se redondea hacia arriba, porque una rejilla de una sola
     * fila no sería 2-D.
     */
    public static int[] rejillaPara(int n)
    {
        int filas = (int) Math.floor(Math.sqrt(n));
        while (filas > 1 && n % filas != 0) filas--;
        int columnas = n / filas;
        if (filas < 2) {
            filas = 2;
            columnas = (int) Math.ceil(n / 2.0);
        }
        return new int[]{filas, columnas};
    }

    /** ¿El mapa está dispuesto en dos dimensiones? */
    public boolean esRejilla()
    {
        return topology == Topology.GRID || topology == Topology.HEX;
    }

    /**
     * Si el radio de vecindad se encoge con las épocas.
     *
     * El SOM canónico empieza con una vecindad ancha —para que el mapa se
     * despliegue— y la va estrechando hasta 1, para afinar. Aquí era constante.
     */
    public void setShrinkRadius(boolean v) { this.shrinkRadius = v; }
    public boolean isShrinkRadius() { return shrinkRadius; }

    /** Radio efectivo en una época: decae linealmente hasta 1 si está activado. */
    public int radiusAt(int epoch)
    {
        if (!shrinkRadius || epochs <= 1) return radious;
        double t = (epoch - 1) / (double) (epochs - 1);       // 0 en la primera, 1 en la última
        return Math.max(1, (int) Math.round(radious - t * (radious - 1)));
    }

    public Topology getTopology() { return topology; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }

    /** Fila y columna de una neurona en la rejilla (null si el mapa es un anillo). */
    public int[] positionOf(SOMNeuron n)
    {
        if (!esRejilla()) return null;
        int idx = this.verticesList.indexOf(n);
        return idx < 0 ? null : new int[]{ idx / cols, idx % cols };
    }

    /** Mapa sobre cualquier dataset. Si es null se usa el Iris de GestorTxt. */
    public SOM(int epochs, int neurons, double learningRate, int radius, Dataset dataset)
    {
      super();
      this.init=false;
      this.trained=false;
      this.epochs=epochs;
      this.totalNeurons=neurons;
      this.initialLearningRate=learningRate;
      this.radious=radius;
      this.dataset=dataset;
    }

    /** El dataset del mapa; se resuelve perezosamente al Iris si no se dio otro. */
    public Dataset getDataset()
    {
        if (dataset == null) dataset = GestorTxt.getIrisDataset();
        return dataset;
    }


    public void setTrained(boolean b)
    {
        this.trained=b;
    }

    /** Permite entrenar sobre un conjunto distinto al de GestorTxt (experimentos, validación). */
    public void setCurrentLearningRate(double lr)
    {
        this.currentLearningRate = lr;
    }

    public boolean isTrained()
    {
        return this.trained;
    }

    public boolean isInit()
    {
        return this.init;
    }

    public void initialize()
    {
        Dataset d = getDataset();
        double[] min = d.getMin(), max = d.getMax();
        boolean esIris = d.dimension() == 4;

        for(int i=1;i<totalNeurons+1;i++) {
            Sample pesos = Sample.random(min, max, RandomFeaturesPicker.getRandom());
            // Para el caso Iris se envuelve en Flower: la interfaz lee las
            // medidas por nombre y hace casts a Flower en varios sitios.
            this.getVerticesList().add(new SOMNeuron(i, esIris ? Flower.from(pesos) : pesos));
        }
        makeConnections();
        this.init=true;
    }

    public void makeConnections()
    {
        if (topology == Topology.HEX) { makeHexConnections(); return; }
        if (topology == Topology.GRID) { makeGridConnections(); return; }
        makeRingConnections();
    }

    /** Rejilla 2-D: cada neurona con su vecina de arriba, abajo, izquierda y derecha. */
    private void makeGridConnections()
    {
        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                int actual = r * cols + c;
                if (c + 1 < cols) this.insertEdgeNDG(actual, r * cols + (c + 1));
                if (r + 1 < rows) this.insertEdgeNDG(actual, (r + 1) * cols + c);
            }
        }
    }

    /**
     * Rejilla hexagonal en coordenadas desplazadas "odd-r": las filas impares se
     * corren media celda a la derecha, así que cada neurona toca hasta seis.
     *
     * Solo se insertan las aristas hacia la derecha y hacia abajo; como son no
     * dirigidas, eso ya conecta el par entero y evita duplicarlas.
     */
    private void makeHexConnections()
    {
        for (int r = 0; r < rows; r++)
        {
            boolean filaImpar = (r % 2) == 1;
            for (int c = 0; c < cols; c++)
            {
                int actual = r * cols + c;

                // Vecina de la derecha, en la misma fila
                if (c + 1 < cols) this.insertEdgeNDG(actual, r * cols + (c + 1));

                if (r + 1 < rows)
                {
                    // Las dos de la fila de abajo. En las filas pares quedan en
                    // c-1 y c; en las impares, en c y c+1.
                    int izquierda = filaImpar ? c : c - 1;
                    int derecha = filaImpar ? c + 1 : c;
                    if (izquierda >= 0 && izquierda < cols) this.insertEdgeNDG(actual, (r + 1) * cols + izquierda);
                    if (derecha >= 0 && derecha < cols) this.insertEdgeNDG(actual, (r + 1) * cols + derecha);
                }
            }
        }
    }

    //This method guaranties that the first two neurons in the edges list of each
    //neuron to be the two previous ones and the other two be the two following neurons
    private void makeRingConnections()
    {   int i;

        this.insertEdgeDG(0,this.verticesList.size()-2);
        this.insertEdgeDG(0,this.verticesList.size()-1);
        this.insertEdgeDG(1,this.verticesList.size()-1);

        for(i=0;i<this.verticesList.size()-2;i++)
        {
            this.insertEdgeNDG(i,i+1);
            this.insertEdgeNDG(i, i+2);
        }

        this.insertEdgeNDG(i, i+1); //Esta seria la conexion de la penultima neurona con la ultima y la primera
        this.insertEdgeDG(i, 0);
        this.insertEdgeDG(++i, 0); //Esta seria la conexion de la ultima neurona con las dos primeras
        this.insertEdgeDG(i, 1);
    }
    public SOMNeuron findBMU(Sample flower)
    {
        SOMNeuron BMU = null;
        double shortestED = Double.MAX_VALUE;
        Iterator<Vertex> iter = this.verticesList.iterator();
        double currentDist = 0.0;
        while(iter.hasNext())
        {
            SOMNeuron current = (SOMNeuron) iter.next();
            currentDist = current.euclidianDistance(flower);
            if(currentDist<=shortestED)
                {
                    shortestED=currentDist;
                    BMU=current;
                }

        }

        return BMU;
    }

    public ArrayList<SOMNeuron> findBMUConLista(Sample flower)
    {
        ArrayList<SOMNeuron> BMU = new ArrayList<SOMNeuron>();
        double shortestED = Double.MAX_VALUE;
        Iterator<Vertex> iter = this.verticesList.iterator();
        double currentDist = 0.0;
        while(iter.hasNext())
        {
            SOMNeuron current = (SOMNeuron) iter.next();
            currentDist = current.euclidianDistance(flower);
            if(currentDist<shortestED)
            {
                shortestED=currentDist;
                BMU.clear();
                BMU.add(current);
            }
            else
                if(currentDist==shortestED)
                {
                    BMU.add(current);
                }

        }

        return BMU;
    }

    public void setInit(boolean b)
    {
        this.init=b;
    }
    /**
     * Agrupa la BMU de cada muestra por su etiqueta real.
     *
     * Antes esto recorría los índices 0-49, 50-99 y 100-149 dando por hecho que
     * el fichero eran 150 flores ordenadas por especie: cargar cualquier otro
     * dataset lo rompía en silencio.
     */
    public void groupBmus(List<Sample> dataBase)
    {
        // Un agrupamiento nuevo sustituye al anterior: sin esto, reentrenar
        // duplicaba las BMUs (150 -> 300) y la clasificación quedaba sucia.
        BMUStock.clear();
        for (Sample s : dataBase)
        {
            if (s.getLabel() == null) continue;
            BMUStock.forLabel(especie(s.getLabel())).add(this.findBMU(s));
        }
    }

    /**
     * Etiqueta cada neurona con la especie mayoritaria entre las muestras que gana.
     * Es más fiable que mirar en qué grupo del BMUStock cayó: una neurona que gana
     * flores de dos especies se queda con la que más veces la eligió, en vez de con
     * la primera que aparezca en la lista.
     */
    public void labelNeurons(List<Sample> dataBase)
    {
        Map<Integer, Map<String, Integer>> votos = new HashMap<>();
        for (Sample f : dataBase)
        {
            if (f.getLabel() == null) continue;
            int id = findBMU(f).getId();
            votos.computeIfAbsent(id, k -> new HashMap<>()).merge(especie(f.getLabel()), 1, Integer::sum);
        }

        labels.clear();
        for (Map.Entry<Integer, Map<String, Integer>> e : votos.entrySet())
        {
            String ganadora = e.getValue().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");
            if (!ganadora.isEmpty()) labels.put(e.getKey(), ganadora);
        }
    }

    /** "Iris-setosa" -> "setosa" */
    private String especie(String tipo)
    {
        String t = tipo.trim().toLowerCase();
        int guion = t.lastIndexOf('-');
        return guion >= 0 ? t.substring(guion + 1) : t;
    }

    public String classify(SOMNeuron bmu)
    {
        String etiqueta = labels.get(bmu.getId());
        if (etiqueta != null) return etiqueta;

        // Neurona muerta (no ganó ninguna muestra): se usa la neurona etiquetada
        // más parecida. Recorre la lista una vez, sin riesgo de ciclo infinito.
        Sample pesos = bmu.getWeights();
        String mejor = "";
        double menorDistancia = Double.MAX_VALUE;
        for (Vertex v : this.verticesList)
        {
            SOMNeuron n = (SOMNeuron) v;
            String suEtiqueta = labels.get(n.getId());
            if (suEtiqueta == null) continue;
            double d = n.euclidianDistance(pesos);
            if (d < menorDistancia)
            {
                menorDistancia = d;
                mejor = suEtiqueta;
            }
        }
        return mejor;
    }


    public SOMNeuron findNearest(SOMNeuron bmu)
    {
        Iterator<Vertex> iter = this.verticesList.iterator();
        SOMNeuron newBMU = null;
        double shortest = Double.MAX_VALUE;
        double distance = 0.0;
        Sample f = bmu.getWeights();
        while (iter.hasNext())
        {
            SOMNeuron n = (SOMNeuron) iter.next();
            if(!n.equals(bmu))
            {
                distance=n.euclidianDistance(f);
                if (distance<=shortest)
                {
                    shortest=distance;
                    newBMU=n;
                }
            }
        }

        return newBMU;
    }

    public void train()
    {
        for(int i=1;i<=epochs;i++)
        {
            this.currentLearningRate= learningRate(i);
            train2(getDataset().getSamples(), i);
        }
        this.trained=true;
        groupBmus(getDataset().getSamples());
        labelNeurons(getDataset().getSamples());
    }

    public void train2(List<Sample> dataBase, int currentEpoch)
    {
        for (Sample flower : dataBase) {
            SOMNeuron bmu = this.findBMU(flower);
            updateBmuAndAdjacents(bmu, flower, currentEpoch);
        }
    }

    /**
     * Actualiza la BMU y su vecindad.
     *
     * La vecindad se calcula recorriendo el grafo en anchura desde la BMU, así
     * que la distancia es el número de saltos y sirve igual para el anillo que
     * para la rejilla. Antes esto caminaba a izquierda y derecha con
     * getFirst()/getLast(), que solo tiene sentido en un anillo.
     */
    public void updateBmuAndAdjacents(SOMNeuron bmu, Sample flower, int currentEpoch)
    {
        for (Map.Entry<SOMNeuron, Integer> e : neighborhood(bmu, radiusAt(currentEpoch)).entrySet())
        {
            double influencia = influenceRate(e.getValue(), currentEpoch);
            e.getKey().updateWeight(influencia, this.currentLearningRate, flower);
        }
    }

    /**
     * Neuronas a como mucho {@code radio} saltos de la de partida, con su
     * distancia. Incluye la propia neurona a distancia 0.
     */
    public Map<SOMNeuron, Integer> neighborhood(SOMNeuron desde, int radio)
    {
        Map<SOMNeuron, Integer> distancias = new LinkedHashMap<>();
        distancias.put(desde, 0);

        Deque<SOMNeuron> cola = new ArrayDeque<>();
        cola.add(desde);

        while (!cola.isEmpty())
        {
            SOMNeuron actual = cola.poll();
            int d = distancias.get(actual);
            if (d >= radio) continue;

            for (Vertex v : actual.getAdjacents())
            {
                SOMNeuron vecino = (SOMNeuron) v;
                if (!distancias.containsKey(vecino))
                {
                    distancias.put(vecino, d + 1);
                    cola.add(vecino);
                }
            }
        }
        return distancias;
    }

    public void updateBMU(SOMNeuron bmu, Sample f, int currentEpch, int distance){
        double influenceRate = influenceRate(distance, currentEpch);
        bmu.updateWeight(influenceRate, this.currentLearningRate, f);
    }

    public void updateGroup(ArrayList<SOMNeuron> toUpdate, int distance, Sample flower, int currentEpoch)
    {
        double influenceRate = influenceRate(distance, currentEpoch);
        for(SOMNeuron n : toUpdate)
        {
            n.updateWeight(influenceRate, this.currentLearningRate, flower);
        }
    }

    public double influenceRate(int distance, int currentEpoch)
    {
        return Math.exp((-Math.pow(distance,2))/(2*((double)totalNeurons/(2*currentEpoch))));
    }

    public double learningRate(int currentEpoch)
    {
        return (initialLearningRate * Math.exp((double) -currentEpoch / epochs));
    }

    public ArrayList<SOMNeuron> checkNotUpdated(ArrayList<SOMNeuron> updated, LinkedList<Vertex> adjacents)
    {
        ArrayList<SOMNeuron> toUpdate = new ArrayList<>();

        Iterator<Vertex> iter = adjacents.iterator();

        while(iter.hasNext())
        {
            SOMNeuron n = (SOMNeuron) iter.next();
            if(!updated.contains(n))
            {
                toUpdate.add(n);
            }
        }
        return toUpdate;
    }
}



