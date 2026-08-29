# NeuronaIris

Herramienta de **mapas autoorganizados (SOM)** implementada desde cero en Java, con interfaz JavaFX. Viene con el dataset **Iris** cargado, y acepta cualquier CSV numérico.

Un SOM es una red neuronal no supervisada que proyecta datos de muchas variables sobre una rejilla de neuronas, colocando cerca lo que se parece. Con el Iris se entrena sobre las medidas de 150 flores y el mapa acaba separando solo las tres especies, sin que nadie le diga cuáles son.

![U-matrix, especie dominante y planos de componentes](docs/mapa-som.png)

## Qué se ve en la imagen

- **U-matrix** — distancia media de cada neurona a sus vecinas. Las zonas claras son fronteras entre grupos. La banda clara cae justo donde *setosa* se separa de las otras dos.
- **Especie dominante** — el mapa se organiza en tres bandas limpias sin haber visto las etiquetas durante el entrenamiento.
- **Planos de componentes** — el valor que aprendió cada neurona para cada medida. Largo y ancho de pétalo salen casi idénticos (están correlacionados) y alineados con el gradiente de especie; el ancho de sépalo sale sin estructura.

## Resultados

Media de **20 particiones estratificadas 70/30**, rejilla 8×6, 40 épocas:

| | |
|---|---|
| Acierto en entrenamiento | 97.6 % |
| **Acierto sobre datos no vistos** | **95.9 %** |
| Error de cuantización | 0.28 |
| Error topográfico | 2.6 – 3.3 % |

Matriz de confusión sobre 900 muestras de prueba:

| real \ predicha | setosa | versicolor | virginica |
|---|---|---|---|
| **setosa** | **300** | 0 | 0 |
| **versicolor** | 0 | 277 | 23 |
| **virginica** | 0 | 14 | 286 |

*Setosa* se separa perfectamente. Los 37 fallos son todos entre *versicolor* y *virginica*, que es donde el dataset se solapa de verdad — lo mismo que ya insinuaba la U-matrix.

## Cómo ejecutarlo

Requiere **JDK 19 o superior**. Maven va incluido con el wrapper.

```bash
./mvnw javafx:run
```

Los tests:

```bash
./mvnw test
```

## Cómo empaquetarlo

```bash
JAVA_HOME=/ruta/al/jdk ./scripts/empaquetar.sh
```

Genera `target/instalador/NeuronaIris.app` (~88 MB): una aplicación autocontenida con su propio runtime, que no necesita Java instalado ni el proyecto al lado.

Hace falta un **JDK completo**, con carpeta `jmods` y con `jpackage`. Los runtime que traen algunos IDE (por ejemplo el de Android Studio) son imágenes tipo JRE sin `jmods`, y `jlink` falla con *"Module java.desktop not found"*; el script lo detecta y lo dice. En macOS con Homebrew:

```bash
brew install openjdk@21
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./scripts/empaquetar.sh
```

Para un instalador en vez de una carpeta de aplicación, cambia `--type app-image` por `dmg`, `msi` o `deb` en el script.

## Cómo se usa la aplicación

1. **START** crea el mapa con los parámetros del formulario: épocas, neuronas, tasa de aprendizaje, radio y **topología** (anillo 1-D o rejilla 2-D).
2. **TRAIN** lo entrena. Se puede reentrenar para seguir afinando.
3. **2-D MAP** abre la U-matrix, los planos de componentes y las etiquetas del mapa actual. Necesita que la topología sea rejilla.
4. **Load Dataset** carga cualquier CSV numérico: la pantalla rehace los campos de entrada, los ejes de las gráficas y la leyenda según sus variables y etiquetas. En `docs/ejemplo-3variables.csv` hay uno de prueba con tres variables. Cada gráfica tiene sus propios desplegables **X** e **Y** para elegir qué par de variables muestra.
5. **Classify** clasifica una muestra introducida a mano; **Load File** clasifica un fichero entero.
6. **Save Map** / **Load Map** guardan y recuperan el mapa entrenado en `~/.neuronairis/mapa.som`, un fichero de texto con la topología, los parámetros y los pesos de cada neurona.

## Cómo está hecho

- **`data.Sample`** — una muestra de N variables con etiqueta. Es la unidad del mapa: tanto los datos como los pesos de cada neurona.
- **`data.Dataset`** — carga cualquier CSV numérico (toma como variables las columnas que parsean a número y como etiqueta la que no), calcula rangos, normaliza y parte en entrenamiento/prueba de forma estratificada.
- **`SOM`** — el mapa. Dos topologías: anillo 1-D y **rejilla 2-D**. La vecindad se calcula con un recorrido en anchura sobre el grafo, así que la distancia es el número de saltos y funciona igual en ambas.
- **`SOMNeuron`** — una neurona; sus pesos viven en el mismo espacio que los datos.
- **`data.SOMAnalysis`** — U-matrix, planos de componentes, error topográfico, matriz de confusión.
- **`ui.MapaView`** — la ventana del mapa, construida con contenedores y un `Canvas` que se redibuja al redimensionar.

La pantalla principal usa `BorderPane` + `GridPane` + `FlowPane`: las gráficas se reparten el espacio, el panel lateral mantiene su ancho y los controles bajan de línea si la ventana se estrecha. Los campos de entrada, los desplegables de ejes y la leyenda se construyen en tiempo de ejecución a partir del dataset cargado. Lo dibujado se guarda en capas, de modo que cambiar un eje repinta la gráfica sin tener que reentrenar. El panel inferior derecho muestra el carrusel de fotos con el Iris y la distribución de muestras por etiqueta con cualquier otro dataset. El Iris viaja dentro del jar y el estado de la aplicación (mapas guardados) se escribe en `~/.neuronairis/`.

El grafo sobre el que se apoya el mapa es la librería `cu.edu.cujae.ceis.graph` de la CUJAE, incluida en el árbol de fuentes.

`Flower` sobrevive como vista con nombres (`getPetalLength()`…) sobre `Sample`, porque la interfaz lee las medidas del Iris por nombre.

## Estado y limitaciones

- El radio de vecindad puede encogerse con las épocas (`setShrinkRadius`), pero viene **desactivado**: medido sobre 20 semillas mejora siempre el error de cuantización y da resultados mixtos en el topográfico.
- La normalización min-max existe pero no se aplica por defecto. En Iris no mejora el acierto porque el largo del pétalo —que domina la distancia con un 70 %— es justo la variable discriminante. En otro dataset conviene activarla.
- El SOM es de rejilla rectangular con vecindad de 4. Las implementaciones de referencia suelen usar rejilla hexagonal, que reparte la vecindad de forma más uniforme.
- No hay validación cruzada: la evaluación usa particiones 70/30 repetidas, que para este tamaño de dataset es suficiente pero no es lo mismo.

## Autoría

Trabajo de la asignatura de Estructuras de Datos de la **CUJAE** (2024), hecho en equipo por **Ruben Frias**, **Fabián Fernández**, **Clari21** y **MrKettleburn**. La mayor parte del código original —incluido el núcleo del SOM y la interfaz— es de Ruben Frias.

En 2026 Fabián retomó el proyecto para terminarlo, porque la última versión del equipo nunca llegó a subirse. De ese trabajo posterior salen: el desacoplamiento del dataset, la topología en rejilla, las lecturas del mapa, la evaluación con datos no vistos y la batería de 65 tests, además de arreglar varios fallos del código original (las imágenes se cargaban desde rutas absolutas de una máquina concreta, reentrenar corrompía el agrupamiento y la clasificación podía entrar en un ciclo infinito).

## Dataset

[Iris](https://archive.ics.uci.edu/dataset/53/iris) — R. A. Fisher, 1936. 150 muestras, 4 medidas, 3 especies.
