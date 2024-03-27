package Modelos;

public class Neurona {
    private double[] pesos;
    private List<Flor> floresAsignadas;

    public Neurona() {
        this.pesos = new double[4];
        this.floresAsignadas = new ArrayList<>();
        for (int i = 0; i < pesos.length; i++) {
            pesos[i] = Math.random();
        }
    }

    public void ajustarPesos(Flor flor, double tasaAprendizaje, double funcionVecindad) {
        double[] caracteristicasFlor = flor.getCaracteristicas();
        for (int i = 0; i < pesos.length; i++) {
            pesos[i] += tasaAprendizaje * funcionVecindad * (caracteristicasFlor[i] - pesos[i]);
        }
        floresAsignadas.add(flor);
    }

    public double[] getPesos() {
        return pesos;
    }

    public List<Flor> getFloresAsignadas() {
        return floresAsignadas;
    }
}
