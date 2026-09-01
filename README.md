<img src="docs/logo.png" alt="" width="120" align="right">

# NeuronaIris

[![tests](https://github.com/Fabian1820/NeuronaIris/actions/workflows/tests.yml/badge.svg)](https://github.com/Fabian1820/NeuronaIris/actions/workflows/tests.yml)
[![licencia MIT](https://img.shields.io/badge/licencia-MIT-blue.svg)](LICENSE)

Herramienta de **mapas autoorganizados (SOM)** implementada desde cero en Java, con interfaz JavaFX. Viene con el dataset **Iris** cargado, y acepta cualquier CSV numérico.

Un SOM es una red neuronal no supervisada que proyecta datos de muchas variables sobre una rejilla de neuronas, colocando cerca lo que se parece. Con el Iris se entrena sobre las medidas de 150 flores y el mapa acaba separando solo las tres especies, sin que nadie le diga cuáles son.

![U-matrix, especie dominante y planos de componentes](docs/mapa-som.png)

## Qué se ve en la imagen

- **U-matrix** — distancia media de cada neurona a sus vecinas. Las zonas claras son fronteras entre grupos. La banda clara cae justo donde *setosa* se separa de las otras dos.
- **Especie dominante** — el mapa se organiza en tres bandas limpias sin haber visto las etiquetas durante el entrenamiento.
- **Planos de componentes** — el valor que aprendió cada neurona para cada medida. Largo y ancho de pétalo salen casi idénticos (están correlacionados) y alineados con el gradiente de especie; el ancho de sépalo sale sin estructura.

## Resultados

**Validación cruzada estratificada de 5 pliegues, repetida 5 veces** (750 evaluaciones), rejilla hexagonal 8×6, 40 épocas:

| | |
|---|---|
| **Acierto sobre datos no vistos** | **95.3 % ± 2.7** |
| Peor pliegue / mejor pliegue | 90.0 % / 100.0 % |
| Error de cuantización | 0.30 |
| Error topográfico | 5.6 % |

Cada muestra se evalúa exactamente una vez por repetición, y cada pliegue conserva la proporción de las tres especies.

Matriz de confusión acumulada:

| real \ predicha | setosa | versicolor | virginica |
|---|---|---|---|
| **setosa** | **250** | 0 | 0 |
| **versicolor** | 1 | 231 | 18 |
| **virginica** | 0 | 16 | 234 |

*Setosa* se separa perfectamente. Casi todos los fallos son entre *versicolor* y *virginica*, que es donde el dataset se solapa de verdad — lo mismo que ya insinuaba la U-matrix.

Para comparar, la estimación por particiones 70/30 repetidas daba 95.9 %: la validación cruzada confirma el número en vez de darlo por bueno.

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

## Rectangular o hexagonal

La rejilla hexagonal es la que usan las implementaciones de referencia, porque su vecindad es uniforme: las seis vecinas equidistan, mientras que en la rectangular las cuatro de los lados están a distancia 1 y las cuatro diagonales a √2.

Medido sobre 20 semillas, rejilla 8×6, 40 épocas y radio 2:

| | rectangular | hexagonal |
|---|---|---|
| Error topográfico *(vecindad del grafo)* | 16.5 % | **5.6 %** |
| Error topográfico *(celdas que se tocan)* | **3.3 %** | 5.6 % |
| Acierto | 97.3 % | 97.2 % |
| Error de cuantización | **0.281** | 0.300 |
| Neuronas muertas | **5.9 / 48** | 7.5 / 48 |

La lectura importante está en las dos primeras filas. La rectangular solo sale bien parada cuando se le conceden las cuatro diagonales, que su función de vecindad **nunca usa al entrenar**: medida contra su propia topología se va al 16.5 %. En la hexagonal ambas medidas coinciden, porque sus seis vecinas son a la vez las de las aristas y las que se tocan.

A cambio, la hexagonal representa los datos algo peor y desperdicia más neuronas. Viene seleccionada por defecto por coherencia, pero las dos están disponibles.

![Mapa con rejilla hexagonal](docs/mapa-som-hex.png)

## Barrido de hiperparámetros

El botón **AUTO-TUNE** busca la mejor combinación de épocas, neuronas, radio, tasa de aprendizaje y topología, y la deja en el formulario. Corre en un hilo aparte para no congelar la interfaz.

Hay tres estrategias implementadas:

| | qué hace | dónde |
|---|---|---|
| Parrilla completa | recorre las 162 combinaciones de una lista fija | `BusquedaHiperparametros.buscar` |
| Búsqueda aleatoria | sortea configuraciones de rangos continuos | `BusquedaHiperparametros.buscarAleatorio` |
| **Búsqueda bayesiana (TPE)** | **cada sorteo aprende de los anteriores** | `BusquedaBayesiana.buscar` |

### Parrilla contra aleatoria

Con una cuarta parte de las evaluaciones la aleatoria llega prácticamente al mismo sitio, y además explora valores que la parrilla no contempla: la tasa de aprendizaje puede salir 0.37 y no solo 0.3 o 0.5.

| presupuesto | mejor acierto *(media de 5 semillas)* | veces que iguala a la parrilla |
|---|---|---|
| parrilla completa — 162 evaluaciones | 98.0 % | — |
| aleatoria — 10 | 97.3 % | 1 de 5 |
| aleatoria — 20 | 97.6 % | 2 de 5 |
| aleatoria — 40 | 97.9 % | 4 de 5 |

### La bayesiana y por qué al principio no servía de nada

TPE (*Tree-structured Parzen Estimator*, el algoritmo de Hyperopt y Optuna) parte lo ya probado en un montón bueno y otro malo, estima con qué frecuencia aparece cada valor en cada uno —`l(x)` y `g(x)`— y prueba a continuación la configuración que maximiza `l(x)/g(x)`. Frente a un proceso gaussiano no hay que invertir matrices ni ajustar el núcleo, y admite sin violencia una variable categórica como la topología.

Puesto tal cual sobre el Iris, **no le ganaba al azar**: 97.93 % contra 97.87 % con 40 evaluaciones, un empate. Antes de echarle la culpa al problema hay que descartar que el algoritmo esté mal, así que se le pasa una función de óptimo conocido con la misma forma de espacio de búsqueda:

| sobre una función sintética, óptimo 100 | 40 evaluaciones |
|---|---|
| aleatoria | 91.0 |
| **TPE** | **97.4** *(gana en 26 de 30 semillas)* |

El motor converge. El problema era otro, y se ve midiendo el ruido de la métrica:

- La **misma** configuración, evaluada con particiones distintas, se mueve entre **92.0 % y 97.3 %** — desviación de **1.57 puntos**.
- Toda la parrilla de 162 cabe entre 91.3 % y 98.0 %, y de la mediana al máximo hay **2.67 puntos**.
- El top-10 de una partición comparte **0 configuraciones** con el top-10 de otra.

Es decir: reevaluar la misma configuración se mueve casi tanto como separa a una configuración mediana de la mejor de las 162. El ranking del que TPE aprende es en buena parte azar de partición, y un modelo ajustado a ruido no puede batir a no ajustar ninguno.

Eso da una predicción comprobable: si se baja el ruido, TPE debería despegarse. Se comprueba **a igualdad de coste** —13 configuraciones con 3 particiones cada una, frente a 40 con una sola— y juzgando lo elegido con 10 particiones que ningún buscador vio:

| buscador *(mismo coste: ~40 pasadas de validación cruzada)* | acierto real de lo elegido |
|---|---|
| aleatoria, 40 × 1 partición | 95.72 % |
| TPE, 40 × 1 partición | 95.53 % |
| aleatoria, 13 × 3 particiones | 95.77 % |
| **TPE, 13 × 3 particiones** | **96.01 %** |

Sobre 50 semillas, la diferencia pareada a favor de TPE es de **+0.23 puntos, IC 95 % de +0.07 a +0.39** — gana en 30, pierde en 9 y empata en 11. Por eso **AUTO-TUNE usa TPE con 3 repeticiones**: no porque la búsqueda bayesiana suene mejor, sino porque en este reparto del presupuesto es la única que se separa del azar de forma medible.

Dos avisos que van con esto:

- El número que anuncia la búsqueda es **optimista**. Elegir al ganador de entre varias configuraciones ya gastó los datos: medido sobre el Iris, la elegida rinde **0.93 puntos menos** sobre particiones que la búsqueda nunca vio. La interfaz lo dice.
- TPE modela cada hiperparámetro por separado, así que no captura que un radio grande solo convenga con muchas neuronas. Las dependencias entre parámetros se le escapan.

Los parámetros importan bastante: sobre el Iris, entre la mejor y la peor combinación hay **casi 7 puntos** de acierto.

| | acierto | configuración |
|---|---|---|
| Mejor | **98.0 % ± 1.6** | hexagonal, 24 neuronas, 40 épocas, radio 2, lr 0.30 |
| Peor | 91.3 % | hexagonal, 48 neuronas, 20 épocas, radio 1, lr 0.50 |

**Ese 98.0 % es optimista y conviene no citarlo como resultado del modelo.** Elegir la ganadora entre 162 candidatas ya usó esos datos, así que parte de la ventaja es haber acertado con el ruido. Medido con evaluación anidada —buscar en una parte y evaluar en otra que la búsqueda no vio:

| | |
|---|---|
| Lo que promete el barrido | 97.7 % |
| Lo que rinde en datos no usados | **96.0 %** |
| Optimismo | **1.7 puntos** |

Por eso la cifra de la sección de resultados sale de validación cruzada con parámetros fijos, no del barrido.

## Cómo se usa la aplicación

1. **START** crea el mapa con los parámetros del formulario: épocas, neuronas, tasa de aprendizaje, radio y **topología** (anillo 1-D, rejilla rectangular o rejilla hexagonal).
2. **TRAIN** lo entrena. Se puede reentrenar para seguir afinando.
3. **2-D MAP** abre la U-matrix, los planos de componentes y las etiquetas del mapa actual. Necesita que la topología sea rejilla.
4. **AUTO-TUNE** busca los mejores parámetros por validación cruzada y rellena el formulario con ellos.
5. **Load Dataset** carga cualquier CSV numérico: la pantalla rehace los campos de entrada, los ejes de las gráficas y la leyenda según sus variables y etiquetas. En `docs/ejemplo-3variables.csv` hay uno de prueba con tres variables. Cada gráfica tiene sus propios desplegables **X** e **Y** para elegir qué par de variables muestra.
6. **Classify** clasifica una muestra introducida a mano; **Load File** clasifica un fichero entero.
7. **Save Map** / **Load Map** guardan y recuperan el mapa entrenado en `~/.neuronairis/mapa.som`, un fichero de texto con la topología, los parámetros y los pesos de cada neurona.

## Cómo está hecho

- **`data.Sample`** — una muestra de N variables con etiqueta. Es la unidad del mapa: tanto los datos como los pesos de cada neurona.
- **`data.Dataset`** — carga cualquier CSV numérico (toma como variables las columnas que parsean a número y como etiqueta la que no), calcula rangos, normaliza, y parte los datos de forma estratificada: en entrenamiento/prueba (`split`) o en k pliegues de validación cruzada (`kFold`).
- **`SOM`** — el mapa. Tres topologías: anillo 1-D, **rejilla rectangular** (4 vecinas) y **rejilla hexagonal** (6 vecinas, filas impares desplazadas media celda). La vecindad se calcula con un recorrido en anchura sobre el grafo, así que la distancia es el número de saltos y funciona igual en las tres.
- **`SOMNeuron`** — una neurona; sus pesos viven en el mismo espacio que los datos.
- **`data.SOMAnalysis`** — U-matrix, planos de componentes, error topográfico, matriz de confusión.
- **`data.BusquedaHiperparametros`** — parrilla de combinaciones y muestreo aleatorio de un espacio de rangos, con el barrido puntuado por validación cruzada.
- **`data.BusquedaBayesiana`** — TPE: modela lo ya probado y propone la siguiente configuración a partir de ello.
- **`ui.MapaView`** — la ventana del mapa, construida con contenedores y un `Canvas` que se redibuja al redimensionar.
- **`ui.PanelDispersion`** — las cuatro gráficas, sus selectores de ejes y la leyenda.
- **`ui.PanelInferior`** — el carrusel del Iris y el reparto de muestras por etiqueta.
- **`ui.FormularioEntrada`** — los campos para teclear una muestra a mano.

`HelloController` se quedó solo con el cableado: reparte los nodos entre esas tres piezas y coordina el ciclo del mapa (crear, entrenar, clasificar, guardar). Antes lo hacía todo él y pasaba de las 780 líneas.

Estas piezas reciben los nodos del controlador en vez de inyectarlos por FXML. Es a propósito: repartir la pantalla por `fx:include` obligaría a partir también el `.fxml` y a manejar controladores anidados, y no compensa para tres zonas de una sola ventana.

La pantalla principal usa `BorderPane` + `GridPane` + `FlowPane`: las gráficas se reparten el espacio, el panel lateral mantiene su ancho y los controles bajan de línea si la ventana se estrecha. Los campos de entrada, los desplegables de ejes y la leyenda se construyen en tiempo de ejecución a partir del dataset cargado. Lo dibujado se guarda en capas, de modo que cambiar un eje repinta la gráfica sin tener que reentrenar. El panel inferior derecho muestra el carrusel de fotos con el Iris y la distribución de muestras por etiqueta con cualquier otro dataset. El Iris viaja dentro del jar y el estado de la aplicación (mapas guardados) se escribe en `~/.neuronairis/`.

El logo es siete neuronas vecinas de un mapa hexagonal, cada una del color del peso que aprendió: el mismo racimo y la misma escala que salen en los planos de componentes. La fuente es [`docs/logo.svg`](docs/logo.svg) y de ahí salen todos los tamaños —icono de ventana, `.icns` y `.ico`— con `scripts/generar-iconos.sh`.

El grafo sobre el que se apoya el mapa es la librería `cu.edu.cujae.ceis.graph` de la CUJAE, incluida en el árbol de fuentes.

`Flower` sobrevive como vista con nombres (`getPetalLength()`…) sobre `Sample`, porque la interfaz lee las medidas del Iris por nombre.

## Estado y limitaciones

- El radio de vecindad puede encogerse con las épocas (`setShrinkRadius`), pero viene **desactivado**: medido sobre 20 semillas mejora siempre el error de cuantización y da resultados mixtos en el topográfico.
- La normalización min-max existe pero no se aplica por defecto. En Iris no mejora el acierto porque el largo del pétalo —que domina la distancia con un 70 %— es justo la variable discriminante. En otro dataset conviene activarla.
- TPE trata los hiperparámetros como independientes. Un TPE multivariante o un proceso gaussiano capturarían las dependencias entre ellos, aunque sobre el Iris el margen que queda es de décimas.
- El barrido usa validación cruzada repetida; la evaluación anidada, que es la que da la cifra honesta, está en los tests pero no en la interfaz.

## Autoría

Trabajo de la asignatura de Estructuras de Datos de la **CUJAE** (2024), hecho en equipo por **Ruben Frias**, **Fabián Fernández**, **Clari21** y **MrKettleburn**. La mayor parte del código original —incluido el núcleo del SOM y la interfaz— es de Ruben Frias.

En 2026 Fabián retomó el proyecto para terminarlo, porque la última versión del equipo nunca llegó a subirse. De ese trabajo posterior salen: el desacoplamiento del dataset, la topología en rejilla, las lecturas del mapa, la evaluación con validación cruzada y la batería de 100 tests, además de arreglar varios fallos del código original (las imágenes se cargaban desde rutas absolutas de una máquina concreta, reentrenar corrompía el agrupamiento y la clasificación podía entrar en un ciclo infinito).

## Dataset

[Iris](https://archive.ics.uci.edu/dataset/53/iris) — R. A. Fisher, 1936. 150 muestras, 4 medidas, 3 especies.

## Licencia

MIT — ver [LICENSE](LICENSE). El copyright es de los cuatro autores del proyecto original.

Aparte queda la copia de `cu.edu.cujae.ceis.graph` incluida en el árbol de fuentes, que es de la CUJAE y cuyos derechos son de sus autores.
