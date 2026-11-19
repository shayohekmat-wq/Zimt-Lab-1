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
     * Validates a visibly closed contour: endpoints touch, no canvas-edge leaks,
     * all stroke pixels connected, exactly one enclosed 4-neighbour region.
     * Mask calculation deliberately avoids allocations per pixel.
     */
    public boolean validate() {
        inside = null;
        if (points < 12 || startX < 0 ||
                Math.abs(endX - startX) > 1 || Math.abs(endY - startY) > 1) return false;
        for (int x = 0; x < SIZE; x++)
            if (outline[at(x, 0)] || outline[at(x, SIZE - 1)]) return false;
        for (int y = 0; y < SIZE; y++)
            if (outline[at(0, y)] || outline[at(SIZE - 1, y)]) return false;

        // Every outline pixel must be connected (eight-neighbour drawing).
        boolean[] connected = new boolean[TOTAL];
        int[] queue = new int[TOTAL];
        int head = 0, tail = 0;
        int first = at(startX, startY);
        connected[first] = true; queue[tail++] = first;
        while (head < tail) {
            int p = queue[head++], x = p % SIZE, y = p / SIZE;
            for (int yy = Math.max(0, y - 1); yy <= Math.min(SIZE - 1, y + 1); yy++) {
                for (int xx = Math.max(0, x - 1); xx <= Math.min(SIZE - 1, x + 1); xx++) {
                    int j = at(xx, yy);
                    if (outline[j] && !connected[j]) { connected[j] = true; queue[tail++] = j; }
                }
            }
        }
        if (tail != points) return false;

        // Flood from all 4 edges (4-neighbour) to find the exterior.
        boolean[] outside = new boolean[TOTAL];
        head = 0; tail = 0;
        for (int x = 0; x < SIZE; x++) {
            tail = floodOffer(at(x, 0), outside, queue, tail);
            tail = floodOffer(at(x, SIZE - 1), outside, queue, tail);
        }
        for (int y = 0; y < SIZE; y++) {
            tail = floodOffer(at(0, y), outside, queue, tail);
            tail = floodOffer(at(SIZE - 1, y), outside, queue, tail);
        }
        while (head < tail) {
            int p = queue[head++], x = p % SIZE, y = p / SIZE;
            if (x > 0) tail = floodOffer(p - 1, outside, queue, tail);
            if (x < SIZE - 1) tail = floodOffer(p + 1, outside, queue, tail);
            if (y > 0) tail = floodOffer(p - SIZE, outside, queue, tail);
            if (y < SIZE - 1) tail = floodOffer(p + SIZE, outside, queue, tail);
        }
        boolean[] candidate = new boolean[TOTAL];
        int n = 0, seed = -1;
        for (int i = 0; i < TOTAL; i++) {
            candidate[i] = !outline[i] && !outside[i];
            if (candidate[i]) { n++; if (seed < 0) seed = i; }
        }
        if (n < 4) return false;

        // Exactly one interior region; disallow figures-of-eight / split shapes.
        boolean[] visited = new boolean[TOTAL];
        head = 0; tail = 0;
        visited[seed] = true; queue[tail++] = seed;
        while (head < tail) {
            int p = queue[head++], x = p % SIZE, y = p / SIZE;
            if (x > 0) tail = insideOffer(p - 1, candidate, visited, queue, tail);
            if (x < SIZE - 1) tail = insideOffer(p + 1, candidate, visited, queue, tail);
            if (y > 0) tail = insideOffer(p - SIZE, candidate, visited, queue, tail);
            if (y < SIZE - 1) tail = insideOffer(p + SIZE, candidate, visited, queue, tail);
        }
        if (tail != n) return false;
        inside = candidate;
        return true;
    }
    private int floodOffer(int i, boolean[] visited, int[] queue, int tail) {
        if (!outline[i] && !visited[i]) { visited[i] = true; queue[tail++] = i; }
        return tail;
    }
    private static int insideOffer(int i, boolean[] region, boolean[] seen, int[] queue, int tail) {
        if (region[i] && !seen[i]) { seen[i] = true; queue[tail++] = i; }
        return tail;
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
    public Bitmap bitmap() {
        int[] rgba = new int[TOTAL];
        for (int i = 0; i < TOTAL; i++) rgba[i] = outline[i] ? (grayOutline ? Color.rgb(86, 86, 86) : PURPLE) : pigments[i];
        Bitmap bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, SIZE, 0, 0, SIZE, SIZE);
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
        if (a.points > 0 && a.startX >= 0 && a.endX >= 0) a.validate();
        return a;
    }
}
