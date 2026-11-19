package de.naehe.gartenkueche;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/** Logical 300x300 canvas. Outline and pigment remain separate for transfer to ZIMT. */
public final class PlantArt {
    public static final int SIZE = 300;
    public static final int TOTAL = SIZE * SIZE;
    public static final int PURPLE = Color.rgb(77, 30, 113);
    final boolean[] outline = new boolean[TOTAL];
    final int[] pigments = new int[TOTAL]; // transparent=unset, any solid ARGB is painted
    int startX = -1, startY = -1, endX = -1, endY = -1, points = 0;
    private boolean[] inside = null;
    boolean grayOutline = false;

    public PlantArt copy() {
        PlantArt c = new PlantArt();
        System.arraycopy(outline, 0, c.outline, 0, TOTAL);
        System.arraycopy(pigments, 0, c.pigments, 0, TOTAL);
        c.startX = startX; c.startY = startY;
        c.endX = endX; c.endY = endY; c.points = points;
        if (inside != null) c.inside = inside.clone();
        c.grayOutline = grayOutline;
        return c;
    }
    public void clear() {
        Arrays.fill(outline, false);
        Arrays.fill(pigments, 0);
        startX = -1; startY = -1; endX = -1; endY = -1; points = 0;
        inside = null; grayOutline = false;
    }
    private static int at(int x, int y) { return y * SIZE + x; }
    public void drawContour(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return;
        if (startX == -1) { startX = x; startY = y; }
        int i = at(x, y);
        if (!outline[i]) { outline[i] = true; points++; }
        endX = x; endY = y; inside = null;
    }
    public void drawContourLine(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, error = dx - dy;
        while (true) {
            drawContour(x0, y0);
            if (x0 == x1 && y0 == y1) return;
            int e = 2 * error;
            if (e > -dy) { error -= dy; x0 += sx; }
            if (e < dx) { error += dx; y0 += sy; }
        }
    }

    /**
     * A valid contour is identified by its enclosed pixels, not by gesture order.
     * Multiple separately drawn lines, branches and crossings are accepted.
     * Short gaps up to four logical pixels may be closed automatically.
     */
    public boolean validate() {
        inside = null;
        ContourDetector.Result result = ContourDetector.detect(outline, SIZE);
        if (result == null) return false;
        System.arraycopy(result.outline, 0, outline, 0, TOTAL);
        inside = result.interior;
        points = 0;
        for (boolean pixel : outline) if (pixel) points++;
        // Preserve non-empty metadata for the older versioned storage reader.
        for (int i = 0; i < TOTAL; i++) {
            if (outline[i]) {
                startX = i % SIZE;
                startY = i / SIZE;
                endX = startX;
                endY = startY;
                break;
            }
        }
        return true;
    }
    public boolean hasInterior() { return inside != null; }
    public void colorPixel(int x, int y, int color) {
        if (inside == null || x < 0 || x >= SIZE || y < 0 || y >= SIZE) return;
        int i = at(x, y);
        if (inside[i]) pigments[i] = color;
    }
    /** Bucket fills a connected area of equal previous color; the outline stays untouched. */
    public void fillRegion(int x, int y, int color) {
        if (inside == null || x < 0 || x >= SIZE || y < 0 || y >= SIZE) return;
        int seed = at(x, y);
        if (!inside[seed] || pigments[seed] == color) return;
        int original = pigments[seed];
        int[] queue = new int[TOTAL];
        boolean[] queued = new boolean[TOTAL];
        int head = 0, tail = 0;
        queued[seed] = true; queue[tail++] = seed;
        while (head < tail) {
            int i = queue[head++], px = i % SIZE, py = i / SIZE;
            pigments[i] = color;
            if (px > 0) tail = fillOffer(i-1, original, queued, queue, tail);
            if (px < SIZE-1) tail = fillOffer(i+1, original, queued, queue, tail);
            if (py > 0) tail = fillOffer(i-SIZE, original, queued, queue, tail);
            if (py < SIZE-1) tail = fillOffer(i+SIZE, original, queued, queue, tail);
        }
    }
    private int fillOffer(int i, int color, boolean[] seen, int[] queue, int tail) {
        if (inside[i] && !seen[i] && pigments[i] == color) {
            seen[i] = true; queue[tail++] = i;
        }
        return tail;
    }
    public void fillAll(int color) {
        if (inside == null) return;
        for (int i = 0; i < TOTAL; i++) if (inside[i]) pigments[i] = color;
    }
    public int renderedPixel(int x, int y) {
        int i=y*SIZE+x;
        return outline[i] ? (grayOutline ? 0xFF565656 : PURPLE) : pigments[i];
    }
    public void renderInto(Bitmap target, int[] scratch) {
        if (target.getWidth()!=SIZE || target.getHeight()!=SIZE || scratch.length<TOTAL)
            throw new IllegalArgumentException("Expected 300x300 ARGB bitmap");
        final int outlineColor=grayOutline ? 0xFF565656 : PURPLE;
        for (int i=0;i<TOTAL;i++)scratch[i]=outline[i] ? outlineColor : pigments[i];
        target.setPixels(scratch,0,SIZE,0,0,SIZE,SIZE);
    }
    public Bitmap bitmap() {
        Bitmap bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        renderInto(bmp,new int[TOTAL]);
        return bmp;
    }
    public PlantArt grayCopy() {
        PlantArt a = copy();
        a.grayOutline = true;
        for (int i = 0; i < TOTAL; i++) {
            int v = pigments[i];
            if (Color.alpha(v) == 0) continue;
            int g = Math.round(Color.red(v) * 0.2126f + Color.green(v) * 0.7152f + Color.blue(v) * 0.0722f);
            a.pigments[i] = Color.argb(Color.alpha(v), g, g, g);
        }
        return a;
    }
    public void write(DataOutputStream stream) throws IOException {
        stream.writeBoolean(grayOutline);
        stream.writeInt(startX); stream.writeInt(startY); stream.writeInt(endX); stream.writeInt(endY);
        stream.writeInt(points);
        for (boolean b : outline) stream.writeBoolean(b);
        for (int rgba : pigments) stream.writeInt(rgba);
    }
    public static PlantArt read(DataInputStream stream) throws IOException {
        PlantArt a = new PlantArt();
        a.grayOutline = stream.readBoolean();
        a.startX=stream.readInt(); a.startY=stream.readInt(); a.endX=stream.readInt();
        a.endY=stream.readInt(); a.points=stream.readInt();
        if (a.points < 0 || a.points > TOTAL) throw new IOException("Invalid outline length");
        for (int i=0;i<TOTAL;i++) a.outline[i] = stream.readBoolean();
        for (int i=0;i<TOTAL;i++) a.pigments[i] = stream.readInt();
        // Reading saved artwork must NEVER re-run expensive contour detection.
        // The active COLOR draft explicitly rebuilds its interior mask once on load.
        return a;
    }
}
