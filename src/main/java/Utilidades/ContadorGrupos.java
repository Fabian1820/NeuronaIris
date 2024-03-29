package Utilidades;

public class ContadorGrupos {
    public int contarGrupos(List<Neurona> neuronas) {
        return (int) neuronas.stream().filter(n -> !n.getFloresAsignadas().isEmpty()).count();
    }
}
