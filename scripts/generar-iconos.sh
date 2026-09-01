#!/usr/bin/env bash
#
# Genera todos los iconos a partir de docs/logo.svg, que es la única fuente.
#
# Se ejecuta a mano cuando cambia el logo; el resultado va versionado para que
# empaquetar no dependa de tener rsvg-convert instalado.
#
# Necesita rsvg-convert (brew install librsvg). El .icns además necesita
# iconutil, que trae macOS.
#
set -euo pipefail
cd "$(dirname "$0")/.."

command -v rsvg-convert >/dev/null || { echo "Falta rsvg-convert (brew install librsvg)" >&2; exit 1; }

SVG=docs/logo.svg
ICONOS=docs/iconos
rm -rf "$ICONOS" && mkdir -p "$ICONOS"

echo "==> PNG del icono de ventana"
rsvg-convert -w 512 -h 512 "$SVG" -o src/main/resources/panal/logo.png

echo "==> PNG para el README"
rsvg-convert -w 160 -h 160 "$SVG" -o docs/logo.png

echo "==> .icns para macOS"
SET="$ICONOS/panal.iconset"
mkdir -p "$SET"
for t in 16 32 128 256 512; do
    rsvg-convert -w $t          -h $t          "$SVG" -o "$SET/icon_${t}x${t}.png"
    rsvg-convert -w $((t*2))    -h $((t*2))    "$SVG" -o "$SET/icon_${t}x${t}@2x.png"
done
if command -v iconutil >/dev/null; then
    iconutil -c icns "$SET" -o "$ICONOS/panal.icns"
    echo "    $ICONOS/panal.icns"
else
    echo "    (sin iconutil: el .icns solo se genera en macOS)"
fi

echo "==> .ico para Windows"
python3 - "$SVG" "$ICONOS/panal.ico" <<'PY'
import subprocess, sys, io
from PIL import Image
svg, destino = sys.argv[1], sys.argv[2]
capas = []
for t in (16, 24, 32, 48, 64, 128, 256):
    png = subprocess.run(["rsvg-convert", "-w", str(t), "-h", str(t), svg],
                         capture_output=True, check=True).stdout
    capas.append(Image.open(io.BytesIO(png)).convert("RGBA"))
capas[0].save(destino, format="ICO", sizes=[(c.width, c.height) for c in capas],
              append_images=capas[1:])
PY
echo "    $ICONOS/panal.ico"

rm -rf "$SET"
echo
echo "Listo."
