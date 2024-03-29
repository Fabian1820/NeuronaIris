package Modelos;

public class Flor {
    private double[] caracteristicas;
    private String tipoFlor;

    public Flor(double[] caracteristicas, String tipoFlor) {
        this.caracteristicas = caracteristicas;
        this.tipoFlor = tipoFlor;
    }

    public double[] getCaracteristicas() {
        return caracteristicas;
    }

    public String getTipoFlor() {
        return tipoFlor;
    }
}
