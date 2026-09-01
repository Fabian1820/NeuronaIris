package panal.data;

import panal.RandomFeaturesPicker;
import panal.SOM;
import panal.SOMNeuron;
import panal.utiles.BMUStock;
import panal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Guardar y recuperar un mapa entrenado. */
class PersistenciaTest {

    private SOM entrenarRejilla() {
        RandomFeaturesPicker.setSeed(31);
        BMUStock.clear();
        SOM som = new SOM(20, 6, 5, 0.5, 2, GestorTxt.getIrisDataset());
        som.initialize();
        som.train();
        return som;
    }

    @Test
    @DisplayName("Un mapa guardado se recupera con los mismos pesos y topología")
    void idaYVuelta() throws IOException {
        SOM original = entrenarRejilla();
        GestorTxt.writeMap(original);
        assertTrue(GestorTxt.haySavedMap());

        SOM leido = GestorTxt.loadMap();

        assertEquals(SOM.Topology.GRID, leido.getTopology());
        assertEquals(original.getRows(), leido.getRows());
        assertEquals(original.getCols(), leido.getCols());
        assertEquals(original.getEpochs(), leido.getEpochs());
        assertEquals(original.getVerticesList().size(), leido.getVerticesList().size());
        assertTrue(leido.isTrained() && leido.isInit());

        for (int i = 0; i < original.getVerticesList().size(); i++) {
            Sample a = ((SOMNeuron) original.getVerticesList().get(i)).getWeights();
            Sample b = ((SOMNeuron) leido.getVerticesList().get(i)).getWeights();
            assertEquals(a.size(), b.size());
            for (int k = 0; k < a.size(); k++) {
                assertEquals(a.get(k), b.get(k), 1e-9, "peso " + k + " de la neurona " + i);
            }
        }
    }

    @Test
    @DisplayName("El mapa recuperado sabe clasificar (antes devolvía vacío)")
    void elMapaLeidoClasifica() throws IOException {
        GestorTxt.writeMap(entrenarRejilla());
        SOM leido = GestorTxt.loadMap();

        int aciertos = 0;
        for (Sample s : leido.getDataset().getSamples()) {
            String especie = leido.classify(leido.findBMU(s));
            assertNotNull(especie);
            assertFalse(especie.isEmpty(), "un mapa cargado debe poder clasificar");
            if (s.getLabel().toLowerCase().contains(especie)) aciertos++;
        }
        double acierto = aciertos * 100.0 / leido.getDataset().size();
        assertTrue(acierto > 85.0, "acierto tras cargar: " + acierto + "%");
    }

    @Test
    @DisplayName("El fichero es texto legible con cabecera y una línea por neurona")
    void elFormatoEsInspeccionable() throws IOException {
        SOM som = entrenarRejilla();
        GestorTxt.writeMap(som);

        String texto = Files.readString(Path.of(GestorTxt.archivoDeEstado("mapa.som")));
        assertTrue(texto.startsWith("# Panal SOM"), "debe empezar con una cabecera legible");
        assertTrue(texto.contains("topology=GRID"));
        assertTrue(texto.contains("rows=6"));
        assertTrue(texto.contains("features="));

        long lineasDeNeurona = texto.lines()
                .filter(l -> !l.isBlank() && !l.startsWith("#") && l.contains(","))
                .filter(l -> Character.isDigit(l.charAt(0)))
                .count();
        assertEquals(30, lineasDeNeurona, "una línea por neurona");
    }

    @Test
    @DisplayName("También va y viene un mapa de anillo")
    void idaYVueltaDelAnillo() throws IOException {
        RandomFeaturesPicker.setSeed(31);
        BMUStock.clear();
        SOM anillo = new SOM(15, 20, 0.5, 2);
        anillo.initialize();
        anillo.train();

        GestorTxt.writeMap(anillo);
        SOM leido = GestorTxt.loadMap();

        assertEquals(SOM.Topology.RING, leido.getTopology());
        assertEquals(20, leido.getVerticesList().size());
        assertEquals(4, ((SOMNeuron) leido.getVerticesList().get(5)).getAdjacents().size());
    }
}
