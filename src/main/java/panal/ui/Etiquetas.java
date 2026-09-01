package panal.ui;

/** Cómo se enseñan las etiquetas del dataset en pantalla. */
public final class Etiquetas {

    private Etiquetas() {}

    /**
     * Se queda con lo que va detrás del último guion: "Iris-setosa" → "setosa".
     *
     * Los datasets suelen traer la clase con el nombre del conjunto por delante,
     * que en una leyenda solo estorba porque se repite en todas.
     */
    public static String corta(String etiqueta) {
        if (etiqueta == null) return "";
        String t = etiqueta.trim().toLowerCase();
        int guion = t.lastIndexOf('-');
        return guion >= 0 ? t.substring(guion + 1) : t;
    }
}
