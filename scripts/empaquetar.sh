#!/usr/bin/env bash
#
# Construye una aplicación nativa de NeuronaIris.
#
# Necesita un JDK COMPLETO (con carpeta jmods y con jpackage). El runtime que
# trae Android Studio no sirve: es una imagen tipo JRE sin jmods, y jlink falla
# con "Module java.desktop not found".
#
#   Comprobar:  ls "$JAVA_HOME/jmods" && "$JAVA_HOME/bin/jpackage" --version
#
# Uso:
#   JAVA_HOME=/ruta/al/jdk ./scripts/empaquetar.sh
#
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ -z "${JAVA_HOME:-}" ]]; then
    echo "Define JAVA_HOME apuntando a un JDK completo." >&2
    exit 1
fi

if [[ ! -d "$JAVA_HOME/jmods" ]]; then
    echo "ERROR: $JAVA_HOME no tiene carpeta jmods, así que jlink no puede construir la imagen." >&2
    echo "       Instala un JDK completo (por ejemplo Temurin 21) y apunta JAVA_HOME ahí." >&2
    exit 1
fi

if [[ ! -x "$JAVA_HOME/bin/jpackage" ]]; then
    echo "ERROR: no hay jpackage en $JAVA_HOME/bin. Hace falta un JDK 14 o superior." >&2
    exit 1
fi

VERSION="1.0.0"
NOMBRE="NeuronaIris"

echo "==> Limpiando"
rm -rf target/app target/app.zip target/instalador
./mvnw -B clean package -DskipTests

echo "==> Imagen de runtime con jlink"
./mvnw -B javafx:jlink

echo "==> Aplicación nativa con jpackage"
mkdir -p target/instalador
"$JAVA_HOME/bin/jpackage" \
    --type app-image \
    --name "$NOMBRE" \
    --app-version "$VERSION" \
    --runtime-image target/app \
    --module com.example.edfinal/com.example.edfinal.HelloApplication \
    --dest target/instalador \
    --vendor "CUJAE" \
    --description "Clasificador del dataset Iris con un mapa autoorganizado"

echo
echo "Listo: target/instalador/$NOMBRE"
echo "Para generar además un instalador, cambia --type app-image por dmg (macOS),"
echo "msi (Windows) o deb/rpm (Linux)."
