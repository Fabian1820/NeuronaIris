package Modelos;

public class SOM {
    private List<Neurona> neuronas;
    private int M; // Máximo número de neuronas.
    private double L0; // Factor de aprendizaje inicial.
    private int J; // Número máximo de iteraciones.

    public SOM(int M, double L0, int J) {
        this.M = M;
        this.L0 = L0;
        this.J = J;
        this.neuronas = new ArrayList<>();
        for (int i = 0; i < M; i++) {
            neuronas.add(new Neurona());
        }
    }

    private double distanciaEuclidiana(double[] vectorA, double[] vectorB) {
        double suma = 0;
        for (int i = 0; i < vectorA.length; i++) {
            suma += Math.pow(vectorA[i] - vectorB[i], 2);
        }
        return Math.sqrt(suma);
    }

    private double funcionVecindad(int indiceBMU, int indiceNeurona, int iteracion) {
        double theta = (double) M / (2 * iteracion);
        double distancia = distanciaEuclidiana(neuronas.get(indiceBMU).obtenerPesos(), neuronas.get(indiceNeurona).obtenerPesos());
        return Math.exp(-Math.pow(distancia, 2) / (2 * Math.pow(theta, 2)));
    }

    private double tasaAprendizaje(int iteracion) {
        return L0 * Math.exp(-iteracion / (double) J);
    }

    public void entrenar(List<Flor> flores) {
        boolean cambios;
        int iteracion = 0;
        do {
            cambios = false;
            for (Flor flor : flores) {
                Neurona BMU = encontrarBMU(flor);
                int indiceBMU = neuronas.indexOf(BMU);
                for (int i = 0; i < neuronas.size(); i++) {
                    double eta = funcionVecindad(indiceBMU, i, iteracion + 1);
                    double L = tasaAprendizaje(iteracion + 1);
                    Neurona neurona = neuronas.get(i);
                    double[] pesosAntiguos = neurona.getPesos().clone();
                    neurona.ajustarPesos(flor, L, eta);
                    if (!Arrays.equals(pesosAntiguos, neurona.getPesos())) {
                        cambios = true;
                    }
                }
            }
            iteracion++;
        } while (cambios && iteracion < J);
    }

    private Neurona encontrarBMU(Flor flor) {
        Neurona BMU = null;
        double distanciaMinima = Double.MAX_VALUE;
        for (Neurona neurona : neuronas) {
            double distancia = distanciaEuclidiana(flor.getCaracteristicas(), neurona.getPesos());
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                BMU = neurona;
            }
        }
        return BMU;
    }

    public List<Neurona> getNeuronas() {
        return neuronas;
    }
}
