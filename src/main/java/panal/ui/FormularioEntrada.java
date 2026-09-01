package panal.ui;

import panal.data.Dataset;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;

/**
 * El formulario donde se teclea una muestra a mano.
 *
 * Se rehace con cada dataset: un campo por variable, con su nombre y el rango
 * observado como pista. Antes eran cuatro campos fijos con los nombres del Iris
 * escritos en el FXML.
 */
public class FormularioEntrada {

    private final GridPane rejilla;
    private final List<TextField> campos = new ArrayList<>();

    public FormularioEntrada(GridPane rejilla) {
        this.rejilla = rejilla;
    }

    /** Un campo por variable del dataset, dos por fila. */
    public void reconstruir(Dataset datos) {
        rejilla.getChildren().clear();
        campos.clear();

        String[] nombres = datos.getFeatureNames();
        double[] min = datos.getMin(), max = datos.getMax();

        for (int i = 0; i < nombres.length; i++) {
            Label etiqueta = new Label(nombres[i]);
            TextField campo = new TextField();
            campo.setAlignment(Pos.CENTER);
            campo.setPrefHeight(34);
            campo.setPromptText(String.format("%.1f – %.1f", min[i], max[i]));
            campos.add(campo);

            int fila = (i / 2) * 2, columna = i % 2;
            rejilla.add(etiqueta, columna, fila);
            rejilla.add(campo, columna, fila + 1);
        }
    }

    /**
     * Lo tecleado, o {@code null} si algún campo no es un número.
     *
     * Devolver null en vez de lanzar deja la decisión de qué enseñar al usuario
     * en quien tiene el diálogo a mano.
     */
    public double[] valores() {
        double[] out = new double[campos.size()];
        for (int i = 0; i < campos.size(); i++) {
            try {
                out[i] = Double.parseDouble(campos.get(i).getText().trim());
            } catch (RuntimeException e) {
                return null;
            }
        }
        return out;
    }
}
