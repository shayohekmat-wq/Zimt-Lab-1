package de.naehe.gartenkueche;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 800x600 logical-pixel bed. A watered plant is a saved instance, not just artwork. */
public class BeetActivity extends Activity {
    private final List<PlantStore.Plant> plants = new ArrayList<>();
    private final Map<String, Bitmap> sprites = new HashMap<>();
    private PlantStore store;
    private BeetCanvas bed;
    private TextView status;
    private TextView hint;
    private Button wateringToggle;
    private boolean wateringMode;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new PlantStore(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout page = Ui.column(this);
        page.setPadding(Ui.dp(this, 17), Ui.dp(this, 27), Ui.dp(this, 17), Ui.dp(this, 26));
        scroll.addView(page);
        Ui.add(page, Ui.text(this, "Beet", 29, true), 5);
        Ui.add(page, Ui.text(this, "800 × 600 Pixel – deine selbst gezeichneten Pflanzen", 13, false), 16);

        bed = new BeetCanvas();
        Ui.add(page, bed, 12);
        status = Ui.text(this, "Deine Pflanzen werden geladen …", 14, false);
        Ui.add(page, status, 12);
        wateringToggle = Ui.button(this, "Gießkanne aktivieren");
        wateringToggle.setOnClickListener(v -> {
            wateringMode = !wateringMode;
            wateringToggle.setText(wateringMode ? "Gießkanne ablegen" : "Gießkanne aktivieren");
            hint.setText(wateringMode
                ? "Finger über einer Pflanze fünf Sekunden halten. Ein langer Halt zählt genau einmal."
                : "Pflanze lange berühren und dann verschieben. Im Gießmodus wird nicht verschoben.");
            bed.cancelAll();
            bed.invalidate();
        });
        Ui.add(page, wateringToggle, 8);
        Button create = Ui.button(this, "Neue Pflanze erstellen");
        create.setOnClickListener(v -> startActivity(new Intent(this, StageSetupActivity.class)));
        Ui.add(page, create, 9);
        hint = Ui.text(this, "Pflanze lange berühren und dann verschieben.", 13, false);
        Ui.add(page, hint, 9);
        Ui.add(page, Ui.text(this,
            "Entwicklungsstand 0.0.2: Gießvorgänge werden gespeichert; die zufälligen Pflegefristen und echte Evolution folgen im nächsten Spielmechanik-Build.",
            12, false), 0);
        setContentView(scroll);
    }
    @Override protected void onResume() {
        super.onResume();
        if (store != null && bed != null) reload();
    }
    private void reload() {
        bed.cancelAll();
        for (Bitmap b : sprites.values()) if (b != null && !b.isRecycled()) b.recycle();
        sprites.clear();
        plants.clear();
        plants.addAll(store.loadPlants());
        int n = 0;
        for (PlantStore.Plant p : plants) n += p.wateringCount;
        status.setText(plants.isEmpty()
            ? "Noch keine Pflanze vorhanden. Erstelle deine erste Evolutionspflanze."
            : plants.size() + (plants.size() == 1 ? " Pflanze" : " Pflanzen") +
            " im Beet · " + n + " gespeicherte Gießvorgänge");
        bed.invalidate();
    }
    @Override protected void onDestroy() {
        if (bed != null) bed.cancelAll();
        for (Bitmap b : sprites.values()) if (b != null && !b.isRecycled()) b.recycle();
        sprites.clear();
        super.onDestroy();
    }
    private void save(PlantStore.Plant p) {
        try { store.savePlant(p); }
        catch (IOException e) {
            Toast.makeText(this, "Pflanze konnte nicht gespeichert werden: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private Bitmap sprite(PlantStore.Plant p) {
        String key = p.id + ":" + p.stage + ":" + p.dead;
        Bitmap bitmap = sprites.get(key);
        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = p.dead ? p.death.bitmap() : p.stages.get(p.stage - 1).bitmap();
            sprites.put(key, bitmap);
        }
        return bitmap;
    }

    private final class BeetCanvas extends View {
        private final Paint p = new Paint();
        private final Paint spritePaint = new Paint();
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final int longPressMs = ViewConfiguration.getLongPressTimeout();
        private PlantStore.Plant pressed, held, moving;
        private long holdStart;
        private boolean counted, pointerDown, dragging;
        private float initialX, initialY, pointerX, pointerY, offsetX, offsetY;
        private Runnable pendingLongPress, pendingWater;
        private final Rect bitmapRect = new Rect(0, 0, PlantArt.SIZE, PlantArt.SIZE);
        private final RectF target = new RectF();
        private final Path spout = new Path();

        BeetCanvas() { super(BeetActivity.this); spritePaint.setFilterBitmap(false); }
        @Override protected void onMeasure(int wSpec, int hSpec) {
            int width = View.MeasureSpec.getSize(wSpec);
            setMeasuredDimension(width, Math.round(width * 600f / 800f));
        }
        @Override protected void onDraw(Canvas original) {
            super.onDraw(original);
            Canvas c = original;
            c.save();
            c.scale(getWidth() / 800f, getHeight() / 600f);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(225, 212, 194));
            c.drawRect(0, 0, 800, 600, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1f);
            p.setColor(0x606B5B4C);
            for (int x=0; x<=800; x+=20) c.drawLine(x,0,x,600,p);
            for (int y=0; y<=600; y+=20) c.drawLine(0,y,800,y,p);
            for (PlantStore.Plant plant : plants) {
                Bitmap img = sprite(plant);
                target.set(plant.x-150, plant.y-300, plant.x+150, plant.y);
                c.drawBitmap(img, bitmapRect, target, spritePaint);
                if (plant == moving) {
                    p.setColor(0xff693e91);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(3f);
                    c.drawRect(target,p);
                }
            }
            if (wateringMode && pointerDown) {
                drawWateringCan(c, pointerX, pointerY);
                if (held != null && !counted) {
                    float progress = Math.min(1f, (SystemClock.elapsedRealtime() - holdStart) / 5000f);
                    float x = Math.max(8f, Math.min(632f, held.x - 80f));
                    float y = Math.max(10f, held.y - 315f);
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(0xff5d5148);
                    c.drawRect(x,y,x+160f,y+13f,p);
                    p.setColor(0xff4e9e7b);
                    c.drawRect(x,y,x+160f*progress,y+13f,p);
                    postInvalidateDelayed(80);
                }
            }
            p.setColor(0xff65503e);
            p.setStrokeWidth(3f);
            p.setStyle(Paint.Style.STROKE);
            c.drawRect(1.5f,1.5f,798.5f,598.5f,p);
            c.restore();
        }
        private void drawWateringCan(Canvas c, float x, float y) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xff687d85);
            c.drawRoundRect(x-30,y-20,x+12,y+20,4,4,p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5);
            c.drawArc(x-49,y-15,x-22,y+10,90,180,false,p);
            spout.reset();
            spout.moveTo(x+10,y-13);spout.lineTo(x+33,y-20);
            c.drawPath(spout,p);
            c.drawLine(x+28,y-23,x+40,y-23,p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xff488eb3);
            c.drawCircle(x+40,y-8,3,p);
        }
        private float worldX(float x) { return x * 800f / Math.max(1,getWidth()); }
        private float worldY(float y) { return y * 600f / Math.max(1,getHeight()); }
        private PlantStore.Plant plantAt(float x,float y) {
            for(int i=plants.size()-1;i>=0;i--) {
                PlantStore.Plant plant=plants.get(i);
                if (x>=plant.x-150 && x<=plant.x+150 && y>=plant.y-300 && y<=plant.y)
                    return plant;
            }
            return null;
        }
        private void clearTimers() {
            if (pendingLongPress != null) handler.removeCallbacks(pendingLongPress);
            if (pendingWater != null) handler.removeCallbacks(pendingWater);
            pendingLongPress = null; pendingWater = null;
        }
        void cancelAll() {
            clearTimers();
            pointerDown = false; counted = false; held = null; pressed = null;
            moving = null; dragging = false; invalidate();
        }
        private void startWater(PlantStore.Plant candidate) {
            if (candidate == held && pointerDown) return;
            if (pendingWater != null) handler.removeCallbacks(pendingWater);
            held = candidate;
            counted = false;
            holdStart = SystemClock.elapsedRealtime();
            if (candidate == null) { invalidate(); return; }
            final PlantStore.Plant expected = candidate;
            pendingWater = () -> {
                if (!pointerDown || !wateringMode || held != expected || counted) return;
                if (SystemClock.elapsedRealtime() - holdStart < 5000L) return;
                counted = true;
                expected.wateringCount++;
                expected.lastWaterAtUtc = System.currentTimeMillis();
                save(expected);
                status.setText(plants.size() + " Pflanzen im Beet · " +
                    totalWaterings() + " gespeicherte Gießvorgänge");
                Toast.makeText(BeetActivity.this, "Ein Gießvorgang registriert!", Toast.LENGTH_SHORT).show();
                invalidate();
            };
            handler.postDelayed(pendingWater, 5000L);
            invalidate();
        }
        private int totalWaterings() {
            int n=0; for(PlantStore.Plant plant:plants) n+=plant.wateringCount; return n;
        }
        @Override public boolean onTouchEvent(MotionEvent ev) {
            int action=ev.getActionMasked();
            if (ev.getPointerCount()>1) { cancelAll(); return true; }
            float x=worldX(ev.getX()), y=worldY(ev.getY());
            if (action==MotionEvent.ACTION_DOWN) {
                pointerDown=true; initialX=x;initialY=y;pointerX=x;pointerY=y;
                if (wateringMode) {
                    startWater(plantAt(x,y));
                } else {
                    pressed=plantAt(x,y);
                    if (pressed != null) {
                        final PlantStore.Plant candidate = pressed;
                        pendingLongPress = () -> {
                            if(!pointerDown || pressed != candidate || wateringMode) return;
                            moving = candidate; dragging = true;
                            offsetX=candidate.x-pointerX;offsetY=candidate.y-pointerY;
                            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                            invalidate();
                        };
                        handler.postDelayed(pendingLongPress,longPressMs);
                    }
                }
                return true;
            }
            if (action==MotionEvent.ACTION_MOVE && pointerDown) {
                pointerX=x;pointerY=y;
                if (wateringMode) {
                    PlantStore.Plant candidate=plantAt(x,y);
                    if(candidate != held) startWater(candidate);
                } else if (dragging && moving != null) {
                    moving.x=Math.max(20,Math.min(780,Math.round(x+offsetX)));
                    moving.y=Math.max(40,Math.min(590,Math.round(y+offsetY)));
                } else if (Math.hypot(x-initialX,y-initialY)>25) {
                    if(pendingLongPress != null) handler.removeCallbacks(pendingLongPress);
                    pressed=null;
                }
                invalidate();return true;
            }
            if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
                if (action==MotionEvent.ACTION_UP && dragging && moving != null) save(moving);
                cancelAll();return true;
            }
            return true;
        }
        @Override protected void onDetachedFromWindow() { cancelAll();super.onDetachedFromWindow(); }
    }
}
