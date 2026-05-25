#!/usr/bin/env bash
#
# update-icon.sh — Regenerate Android app icons from logo.svg
#
# The SVG contains embedded raster PNGs layered at specific positions.
# Python extracts each layer, then Java composites and resizes them.
#
# Requirements: python3, java (JDK)
# Usage: ./update-icon.sh [path/to/logo.svg]

set -euo pipefail

SVG="${1:-logo.svg}"
RES_DIR="app/src/main/res"
WORK="${TMPDIR:-/tmp}/icon-gen-$$"

mkdir -p "$WORK"
trap 'rm -rf "$WORK"' EXIT

if [ ! -f "$SVG" ]; then
    echo "Error: $SVG not found. Run from KompaktWind/ root." >&2
    exit 1
fi

echo "==> Extracting layers from $SVG..."

# Step 1: Extract all embedded PNGs and their positions from the SVG
python3 - "$SVG" "$WORK" << 'PYEOF'
import sys, re, base64, json

svg_path = sys.argv[1]
work_dir = sys.argv[2]

with open(svg_path, "r") as f:
    svg = f.read()

vb = re.search(r'viewBox="\s*(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s*"', svg)
if not vb:
    print("Error: no viewBox found"); sys.exit(1)
canvas_w, canvas_h = int(vb.group(3)), int(vb.group(4))

# Match each <image> block: grab x, y, width, height, href data
# The attributes may appear in any order
image_blocks = re.findall(r'<image\b(.*?)/?>', svg, re.DOTALL)
layers = []
for i, block in enumerate(image_blocks):
    x = int(re.search(r'x="(\d+)"', block).group(1)) if re.search(r'x="(\d+)"', block) else 0
    y = int(re.search(r'y="(\d+)"', block).group(1)) if re.search(r'y="(\d+)"', block) else 0
    w = int(re.search(r'width="(\d+)"', block).group(1)) if re.search(r'width="(\d+)"', block) else canvas_w
    h = int(re.search(r'height="(\d+)"', block).group(1)) if re.search(r'height="(\d+)"', block) else canvas_h
    b64_match = re.search(r'href="data:image/png;base64,([^"]*)"', block, re.DOTALL)
    if not b64_match:
        continue
    png_data = base64.b64decode(b64_match.group(1))
    png_path = f"{work_dir}/layer_{i}.png"
    with open(png_path, "wb") as f:
        f.write(png_data)
    layers.append({"path": png_path, "x": x, "y": y, "w": w, "h": h})
    print(f"  Layer {i}: {w}x{h} at ({x},{y}), {len(png_data)} bytes")

if not layers:
    print("Error: no embedded PNG found in SVG"); sys.exit(1)

manifest = {"canvas_w": canvas_w, "canvas_h": canvas_h, "layers": layers}
with open(f"{work_dir}/layers.json", "w") as f:
    json.dump(manifest, f)

print(f"  Canvas: {canvas_w}x{canvas_h}, {len(layers)} layer(s)")
PYEOF

echo "==> Compositing and resizing..."

# Step 2: Java composites layers onto canvas, then resizes for all densities
cat > "$WORK/IconGen.java" << 'JAVAEOF'
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

public class IconGen {
    // Legacy launcher icon sizes: 48dp * density multiplier
    static int[] ICON_SIZES = {72, 48, 96, 144, 192};
    static String[] DENSITIES = {"hdpi", "mdpi", "xhdpi", "xxhdpi", "xxxhdpi"};

    public static void main(String[] args) throws Exception {
        String workDir = args[0];
        String resDir = args[1];

        // Parse layers.json (minimal JSON parsing without dependencies)
        String json = readFile(workDir + "/layers.json");
        int canvasW = jsonInt(json, "canvas_w");
        int canvasH = jsonInt(json, "canvas_h");

        // Composite all layers onto a single canvas
        BufferedImage canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gc = canvas.createGraphics();
        gc.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Find layer entries
        int layersStart = json.indexOf("\"layers\"");
        String layersStr = json.substring(json.indexOf('[', layersStart));
        String[] layerChunks = layersStr.split("\\},\\s*\\{");
        for (String chunk : layerChunks) {
            String path = jsonString(chunk, "path");
            int x = jsonInt(chunk, "\"x\"");
            int y = jsonInt(chunk, "\"y\"");
            int w = jsonInt(chunk, "\"w\"");
            int h = jsonInt(chunk, "\"h\"");
            BufferedImage layer = ImageIO.read(new File(path));
            gc.drawImage(layer, x, y, w, h, null);
            System.out.println("  Drew layer " + layer.getWidth() + "x" + layer.getHeight() + " at (" + x + "," + y + ") scaled to " + w + "x" + h);
        }
        gc.dispose();

        // Save composite for debugging
        ImageIO.write(canvas, "png", new File(workDir + "/composite.png"));
        System.out.println("  Composite: " + canvasW + "x" + canvasH);

        // Resize for each density (legacy ic_launcher.png only, no adaptive icon)
        for (int i = 0; i < DENSITIES.length; i++) {
            File dir = new File(resDir, "mipmap-" + DENSITIES[i]);
            dir.mkdirs();
            resize(canvas, new File(dir, "ic_launcher.png"), ICON_SIZES[i]);
            System.out.println("  " + DENSITIES[i] + ": " + ICON_SIZES[i] + "px");
        }
    }

    static void resize(BufferedImage src, File out, int size) throws Exception {
        BufferedImage dst = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        ImageIO.write(dst, "png", out);
    }

    static String readFile(String path) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    static int jsonInt(String json, String key) {
        String k = key.startsWith("\"") ? key : "\"" + key + "\"";
        int idx = json.indexOf(k);
        if (idx < 0) return 0;
        int colon = json.indexOf(':', idx + k.length());
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        return Integer.parseInt(json.substring(start, end));
    }

    static String jsonString(String json, String key) {
        String k = "\"" + key + "\"";
        int idx = json.indexOf(k);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + k.length());
        int qStart = json.indexOf('"', colon + 1);
        int qEnd = json.indexOf('"', qStart + 1);
        return json.substring(qStart + 1, qEnd);
    }
}
JAVAEOF

javac "$WORK/IconGen.java" -d "$WORK"
java -cp "$WORK" IconGen "$WORK" "$RES_DIR"

echo ""
echo "Done! Icons updated in $RES_DIR/mipmap-*/"
echo "Rebuild with: gradle assembleDebug"
