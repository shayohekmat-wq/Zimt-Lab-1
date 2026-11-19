package de.naehe.gartenkueche;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Arrays;

public class EditorActivity extends Activity {
    private PixelCanvas pixelCanvas;
    private Button validate;
    private Button fill;
    private GridLayout palette;
    private TextView hint;
    private boolean colorMode = false;
    private static final int PURPLE = Color.rgb(77, 30, 113);
    private static final int[] COLORS = {
        0xFFFFFFFF,0xFFDAD5C9,0xFFA6A6A6,0xFF555555,0xFF1D1D1D,
        0xFFFAE6A0,0xFFF7BC45,0xFFF28433,0xFFD83B37,0xFF9F1839,
        0xFFFFB6A4,0xFFF5799A,0xFFB852A5,0xFF8052BD,0xFF472476,
        0xFFB3A3DB,0xFF88BDEA,0xFF398CD1,0xFF2252A4,0xFF12365B,
        0xFFA8E2D5,0xFF54BCB1,0xFF218A8C,0xFF145D67,0xFFD9DF71,
        0xFFA8CB4B,0xFF6FA548,0xFF38733C,0xFF1E4A34,0xFFA0BD8C,
        0xFFE8C4A3,0xFFC89062,0xFFA36540,0xFF753B25,0xFF452A25,
        0xFFFAE7D7,0xFFDDAA8F,0xFFAD7471,0xFF75474E,0xFF322932
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int stageCount = Math.max(1, Math.min(10, getIntent().getIntExtra("stage_count", 1)));
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout column = Ui.column(this);
        column.setPadding(Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20));
        scroll.addView(column);

        Ui.add(column, Ui.text(this, "Pflanzen-Kontur · Stufe 1 / " + stageCount, 22, true), 8);
        hint = Ui.text(this,
                "300 × 300 Raster. Zeichne mit einem Zug eine geschlossene dunkelviolette Kontur. Zwei Finger verschieben die vergrößerte Fläche.",
                13, false);
        Ui.add(column, hint, 12);

        pixelCanvas = new PixelCanvas();
        column.addView(pixelCanvas, Ui.spacing(this, -1, -2, 9));

        LinearLayout tools = Ui.row(this);
        Button zoomOut = Ui.button(this, "−");
        Button zoomIn = Ui.button(this, "+");
        Button reset = Ui.button(this, "Leeren");
        tools.addView(zoomOut, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1);
        plusParams.leftMargin = Ui.dp(this, 6);
        tools.addView(zoomIn, plusParams);
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 2);
        resetParams.leftMargin = Ui.dp(this, 6);
        tools.addView(reset, resetParams);
        Ui.add(column, tools, 10);
        zoomOut.setOnClickListener(v -> pixelCanvas.changeZoom(false));
        zoomIn.setOnClickListener(v -> pixelCanvas.changeZoom(true));
        reset.setOnClickListener(v -> {
            pixelCanvas.clear();
            colorMode = false;
            palette.setVisibility(View.GONE);
            fill.setVisibility(View.GONE);
            validate.setText("Kontur prüfen");
            hint.setText("Zeichne eine geschlossene Kontur. Prüfen schaltet die Innenpixel zum Ausmalen frei.");
        });

        palette = new GridLayout(this);
        palette.setColumnCount(10);
        palette.setVisibility(View.GONE);
        for (int color : COLORS) {
            View swatch = new View(this);
            swatch.setBackground(Ui.shape(color, Ui.dp(this, 3)));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = Ui.dp(this, 28);
            lp.height = Ui.dp(this, 28);
            lp.setMargins(Ui.dp(this, 1), Ui.dp(this, 1), Ui.dp(this, 1), Ui.dp(this, 1));
            palette.addView(swatch, lp);
            swatch.setContentDescription("Farbe " + String.format("#%06X", 0xFFFFFF & color));
            swatch.setOnClickListener(v -> pixelCanvas.setBrush(color));
        }
        Ui.add(column, palette, 10);

        fill = Ui.button(this, "Innenfläche mit gewählter Farbe füllen");
        fill.setVisibility(View.GONE);
        fill.setOnClickListener(v -> pixelCanvas.fillInterior());
        Ui.add(column, fill, 8);

        validate = Ui.button(this, "Kontur prüfen");
        validate.setOnClickListener(v -> {
            if (!colorMode) {
                if (!pixelCanvas.validateClosed()) {
                    Toast.makeText(this, "Kontur offen oder Innenfläche nicht eindeutig. Anfang und Ende müssen sich berühren.", Toast.LENGTH_LONG).show();
                    return;
                }
                colorMode = true;
                pixelCanvas.colorMode = true;
                palette.setVisibility(View.VISIBLE);
                fill.setVisibility(View.VISIBLE);
                validate.setText("Ausmalen testen");
                hint.setText("Kontur geprüft: Male ausschließlich innerhalb der Pflanze. Dieses erste Editor-Demo speichert noch keine Evolutionen.");
            } else {
                Toast.makeText(this, "Editor-Prototyp funktioniert. Der dauerhafte Mehrstufen-Speicher folgt im nächsten Entwicklungsmeilenstein.", Toast.LENGTH_LONG).show();
            }
        });
        Ui.add(column, validate, 10);
        Ui.add(column, Ui.text(this,
                "Früher Prototyp 0.0.1: Noch kein Speichern, Farbeditor ist auf Stufe 1 begrenzt.", 12, false), 0);
        setContentView(scroll);
    }

    private final class PixelCanvas extends View {
        private static final int SIZE = 300;
        private static final int CELL_COUNT = SIZE * SIZE;
        private final boolean[] outline = new boolean[CELL_COUNT];
        private final boolean[] interior = new boolean[CELL_COUNT];
        private final int[] artwork = new int[CELL_COUNT];
        private final Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        private final Paint bitmapPaint = new Paint();
        private final Paint gridPaint = new Paint();
        private final Paint borderPaint = new Paint();
        private int zoom = 1;
        private int offsetX = 0, offsetY = 0;
        private int brush = COLORS[26];
        private int firstX = -1, firstY = -1, lastX = -1, lastY = -1;
        private int numberOfPoints = 0;
        private float lastPanX, lastPanY;
        private boolean panning;
        private boolean colorMode;
        private final Rect src = new Rect();
        private final RectF dst = new RectF();

        PixelCanvas() {
            super(EditorActivity.this);
            bitmapPaint.setFilterBitmap(false);
            gridPaint.setColor(0x55928A82);
            gridPaint.setStrokeWidth(1f);
            borderPaint.setColor(PURPLE);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(Ui.dp(EditorActivity.this, 2));
        }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int w = MeasureSpec.getSize(widthSpec);
            setMeasuredDimension(w, w);
        }
        void setBrush(int color) { brush = color; }
        void clear() {
            Arrays.fill(outline, false);
            Arrays.fill(interior, false);
            Arrays.fill(artwork, Color.TRANSPARENT);
            bitmap.eraseColor(Color.TRANSPARENT);
            firstX = -1; firstY = -1; lastX = -1; lastY = -1; numberOfPoints = 0;
            colorMode = false;
            invalidate();
        }
        void changeZoom(boolean increase) {
            int previous = zoom;
            if (increase) zoom = zoom == 1 ? 2 : zoom == 2 ? 5 : 10;
            else zoom = zoom == 10 ? 5 : zoom == 5 ? 2 : 1;
            if (zoom == previous) return;
            int oldVisible = SIZE / previous;
            float centerX = offsetX + oldVisible / 2f;
            float centerY = offsetY + oldVisible / 2f;
            int visible = SIZE / zoom;
            offsetX = Math.max(0, Math.min(SIZE - visible, Math.round(centerX - visible / 2f)));
            offsetY = Math.max(0, Math.min(SIZE - visible, Math.round(centerY - visible / 2f)));
            invalidate();
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float side = Math.min(getWidth(), getHeight());
            float left = (getWidth() - side) / 2f;
            float top = (getHeight() - side) / 2f;
            dst.set(left, top, left + side, top + side);
            Paint background = new Paint();
            background.setColor(Color.WHITE);
            canvas.drawRect(dst, background);
            int visible = SIZE / zoom;
            src.set(offsetX, offsetY, offsetX + visible, offsetY + visible);
            canvas.drawBitmap(bitmap, src, dst, bitmapPaint);
            int step = zoom >= 5 ? 1 : zoom >= 2 ? 5 : 10;
            for (int x = offsetX; x <= offsetX + visible; x++) {
                if (x % step != 0) continue;
                float screenX = left + (x - offsetX) * side / visible;
                canvas.drawLine(screenX, top, screenX, top + side, gridPaint);
            }
            for (int y = offsetY; y <= offsetY + visible; y++) {
                if (y % step != 0) continue;
                float screenY = top + (y - offsetY) * side / visible;
                canvas.drawLine(left, screenY, left + side, screenY, gridPaint);
            }
            canvas.drawRect(dst, borderPaint);
        }
        private int logicalX(float px) {
            float side = Math.min(getWidth(), getHeight());
            return Math.max(0, Math.min(299, offsetX + (int) (px * (SIZE / zoom) / side)));
        }
        private int logicalY(float py) {
            float side = Math.min(getWidth(), getHeight());
            return Math.max(0, Math.min(299, offsetY + (int) (py * (SIZE / zoom) / side)));
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
                panning = true;
                lastPanX = (event.getX(0) + event.getX(1)) / 2f;
                lastPanY = (event.getY(0) + event.getY(1)) / 2f;
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() >= 2) {
                float cx = (event.getX(0) + event.getX(1)) / 2f;
                float cy = (event.getY(0) + event.getY(1)) / 2f;
                int visible = SIZE / zoom;
                float scale = visible / (float) Math.max(1, getWidth());
                offsetX = Math.max(0, Math.min(SIZE - visible, Math.round(offsetX - (cx - lastPanX) * scale)));
                offsetY = Math.max(0, Math.min(SIZE - visible, Math.round(offsetY - (cy - lastPanY) * scale)));
                lastPanX = cx; lastPanY = cy;
                invalidate(); return true;
            }
            if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                panning = true; lastX = -1; lastY = -1; return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                lastX = -1; lastY = -1; panning = false; return true;
            }
            if (panning && action != MotionEvent.ACTION_DOWN) return true;
            if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE) return true;
            if (action == MotionEvent.ACTION_DOWN) panning = false;
            int x = logicalX(event.getX());
            int y = logicalY(event.getY());
            if (action == MotionEvent.ACTION_DOWN) {
                if (!colorMode && firstX < 0) { firstX = x; firstY = y; }
                lastX = x; lastY = y;
                putPixel(x, y);
            } else {
                if (lastX >= 0) line(lastX, lastY, x, y);
                else putPixel(x, y);
                lastX = x; lastY = y;
            }
            invalidate();
            return true;
        }
        private void line(int x0, int y0, int x1, int y1) {
            int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
            int err = dx - dy;
            for (;;) {
                putPixel(x0, y0);
                if (x0 == x1 && y0 == y1) return;
                int e2 = 2 * err;
                if (e2 > -dy) { err -= dy; x0 += sx; }
                if (e2 < dx) { err += dx; y0 += sy; }
            }
        }
        private void putPixel(int x, int y) {
            int idx = y * SIZE + x;
            if (!colorMode) {
                if (!outline[idx]) {
                    outline[idx] = true;
                    numberOfPoints++;
                }
                bitmap.setPixel(x, y, PURPLE);
                lastX = x; lastY = y;
            } else if (interior[idx] && !outline[idx]) {
                artwork[idx] = brush;
                bitmap.setPixel(x, y, brush);
            }
        }
        boolean validateClosed() {
            if (numberOfPoints < 12 || firstX < 0 || lastX < 0) {
                // ACTION_UP resets last pointer; find closure by connectivity of drawn pixels instead.
                return detectInterior();
            }
            return detectInterior();
        }
        private boolean detectInterior() {
            if (numberOfPoints < 12) return false;
            for (int x = 0; x < SIZE; x++)
                if (outline[x] || outline[(SIZE - 1) * SIZE + x]) return false;
            for (int y = 0; y < SIZE; y++)
                if (outline[y * SIZE] || outline[y * SIZE + SIZE - 1]) return false;
            return computeInteriorByFlood();
        }
        private boolean computeInteriorByFlood() {
            boolean[] outside = new boolean[CELL_COUNT];
            int[] queue = new int[CELL_COUNT];
            int head = 0, tail = 0;
            for (int x = 0; x < SIZE; x++) {
                int a = x, b = (SIZE - 1) * SIZE + x;
                if (!outline[a] && !outside[a]) { outside[a] = true; queue[tail++] = a; }
                if (!outline[b] && !outside[b]) { outside[b] = true; queue[tail++] = b; }
            }
            for (int y = 0; y < SIZE; y++) {
                int a = y * SIZE, b = a + SIZE - 1;
                if (!outline[a] && !outside[a]) { outside[a] = true; queue[tail++] = a; }
                if (!outline[b] && !outside[b]) { outside[b] = true; queue[tail++] = b; }
            }
            while (head < tail) {
                int i = queue[head++];
                int x = i % SIZE, y = i / SIZE;
                if (x > 0) tail = enqueue(i - 1, outside, queue, tail);
                if (x + 1 < SIZE) tail = enqueue(i + 1, outside, queue, tail);
                if (y > 0) tail = enqueue(i - SIZE, outside, queue, tail);
                if (y + 1 < SIZE) tail = enqueue(i + SIZE, outside, queue, tail);
            }
            int inside = 0;
            for (int i = 0; i < CELL_COUNT; i++) {
                interior[i] = !outline[i] && !outside[i];
                if (interior[i]) inside++;
            }
            return inside > 3;
        }
        private int enqueue(int i, boolean[] visited, int[] queue, int tail) {
            if (!outline[i] && !visited[i]) { visited[i] = true; queue[tail++] = i; }
            return tail;
        }
        void fillInterior() {
            if (!colorMode) return;
            for (int i = 0; i < CELL_COUNT; i++) {
                if (interior[i]) {
                    artwork[i] = brush;
                    bitmap.setPixel(i % SIZE, i / SIZE, brush);
                }
            }
            invalidate();
        }
    }
}
