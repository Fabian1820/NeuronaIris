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
    public enum Topology { RING, GRID }

    private Topology topology = Topology.RING;
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
      this(epochs, rows * cols, learningRate, radius, dataset);
      if (rows < 2 || cols < 2) {
          throw new IllegalArgumentException("Una rejilla necesita al menos 2x2 y se pidió " + rows + "x" + cols);
      }
      this.topology = Topology.GRID;
      this.rows = rows;
      this.cols = cols;
    }

    public Topology getTopology() { return topology; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }

    /** Fila y columna de una neurona en la rejilla (null si el mapa es un anillo). */
    public int[] positionOf(SOMNeuron n)
    {
        if (topology != Topology.GRID) return null;
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
        for (Map.Entry<SOMNeuron, Integer> e : neighborhood(bmu, radious).entrySet())
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



