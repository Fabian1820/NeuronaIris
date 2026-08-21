package com.example.edfinal;

import com.example.edfinal.utiles.BMUStock;
import com.example.edfinal.utiles.GestorTxt;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SOMTest {

    @BeforeEach
    void limpiarStock() {
        BMUStock.getSetosa().clear();
        BMUStock.getVersicolor().clear();
        BMUStock.getVirginica().clear();
    }

    // ---------- dataset ----------

    @Test
    @DisplayName("El dataset iris carga 150 flores, 50 de cada especie y en orden")
    void datasetCargaCompleto() {
        ArrayList<Flower> db = GestorTxt.getDataBase();
        assertEquals(150, db.size(), "el iris.data debe tener 150 muestras");

        assertTrue(db.subList(0, 50).stream().allMatch(f -> f.getType().contains("setosa")));
        assertTrue(db.subList(50, 100).stream().allMatch(f -> f.getType().contains("versicolor")));
        assertTrue(db.subList(100, 150).stream().allMatch(f -> f.getType().contains("virginica")));
    }

    @Test
    @DisplayName("Las medidas del dataset están en rangos verosímiles")
    void datasetTieneMedidasValidas() {
        for (Flower f : GestorTxt.getDataBase()) {
            assertTrue(f.getSepalLength() > 0 && f.getSepalLength() < 10);
            assertTrue(f.getSepalWidth() > 0 && f.getSepalWidth() < 10);
            assertTrue(f.getPetalLength() > 0 && f.getPetalLength() < 10);
            assertTrue(f.getPetalWidth() > 0 && f.getPetalWidth() < 10);
        }
    }

    // ---------- topología ----------

    @Test
    @DisplayName("initialize() crea exactamente las neuronas pedidas")
    void initializeCreaLasNeuronasPedidas() {
        SOM som = new SOM(1, 12, 0.5, 2);
        som.initialize();
        assertEquals(12, som.getVerticesList().size());
        assertTrue(som.isInit());
        assertFalse(som.isTrained());
    }

    @Test
    @DisplayName("El anillo conecta cada neurona con 4 vecinas (2 antes, 2 después)")
    void topologiaEsUnAnilloDeGrado4() {
        SOM som = new SOM(1, 10, 0.5, 2);
        som.initialize();

        for (Vertex v : som.getVerticesList()) {
            SOMNeuron n = (SOMNeuron) v;
            assertEquals(4, n.getAdjacents().size(),
                    "la neurona " + n.getId() + " debería tener 4 vecinas en el anillo");
        }
    }

    // ---------- BMU ----------

    @Test
    @DisplayName("findBMU devuelve la neurona de menor distancia euclidiana")
    void findBmuDevuelveLaMasCercana() {
        SOM som = new SOM(1, 8, 0.5, 1);
        som.initialize();

        Flower objetivo = new Flower(5.1, 3.5, 1.4, 0.2, "Iris-setosa");
        SOMNeuron bmu = som.findBMU(objetivo);
        assertNotNull(bmu);

        double distanciaBmu = bmu.euclidianDistance(objetivo);
        for (Vertex v : som.getVerticesList()) {
            SOMNeuron n = (SOMNeuron) v;
            assertTrue(n.euclidianDistance(objetivo) >= distanciaBmu - 1e-9,
                    "ninguna neurona puede estar más cerca que la BMU");
        }
    }

    // ---------- entrenamiento ----------

    @Test
    @DisplayName("train() deja el mapa entrenado y acerca las neuronas a los datos")
    void entrenarAcercaLasNeuronasALosDatos() {
        SOM som = new SOM(5, 20, 0.5, 2);
        som.initialize();

        List<Flower> db = GestorTxt.getDataBase();
        double errorAntes = errorMedio(som, db);

        som.train();

        assertTrue(som.isTrained());
        double errorDespues = errorMedio(som, db);
        assertTrue(errorDespues < errorAntes,
                "entrenar debería reducir la distancia media a los datos (antes=" + errorAntes
                        + ", después=" + errorDespues + ")");
    }

    private double errorMedio(SOM som, List<Flower> db) {
        double total = 0;
        for (Flower f : db) {
            total += som.findBMU(f).euclidianDistance(f);
        }
        return total / db.size();
    }

    // ---------- agrupamiento y clasificación ----------

    @Test
    @DisplayName("groupBmus() reparte las 150 muestras en los tres grupos")
    void groupBmusRepteLasMuestras() {
        SOM som = new SOM(3, 20, 0.5, 2);
        som.initialize();
        som.train();

        int total = BMUStock.getSetosa().size()
                + BMUStock.getVersicolor().size()
                + BMUStock.getVirginica().size();
        assertEquals(150, total,
                "tras entrenar debe haber exactamente una BMU registrada por muestra");
    }

    @Test
    @DisplayName("Reentrenar no debe duplicar las BMUs registradas")
    void reentrenarNoDuplicaLasBmus() {
        SOM som = new SOM(2, 20, 0.5, 2);
        som.initialize();

        som.train();
        int trasPrimera = BMUStock.getSetosa().size()
                + BMUStock.getVersicolor().size()
                + BMUStock.getVirginica().size();

        som.train();
        int trasSegunda = BMUStock.getSetosa().size()
                + BMUStock.getVersicolor().size()
                + BMUStock.getVirginica().size();

        assertEquals(trasPrimera, trasSegunda,
                "cada entrenamiento debe reemplazar el agrupamiento, no acumularlo");
        assertEquals(150, trasSegunda);
    }

    @Test
    @DisplayName("classify() termina y devuelve una de las tres especies")
    void classifyDevuelveUnaEspecieYTermina() {
        SOM som = new SOM(3, 20, 0.5, 2);
        som.initialize();
        som.train();

        Flower setosaClara = new Flower(5.1, 3.5, 1.4, 0.2, "Iris-setosa");
        String especie = som.classify(som.findBMU(setosaClara));

        assertTrue(List.of("setosa", "versicolor", "virginica").contains(especie),
                "classify devolvió: " + especie);
    }
}
