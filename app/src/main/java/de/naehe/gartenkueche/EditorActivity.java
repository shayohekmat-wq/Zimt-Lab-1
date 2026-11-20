package de.naehe.gartenkueche;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayDeque;

/** A/B wizard for all stages and custom death artwork, using real logical pixels. */
public class EditorActivity extends Activity {
    private static final int[] COLORS = {
        0xFFFFFFFF,0xFFDAD5C9,0xFFA6A6A6,0xFF555555,0xFF1D1D1D,
        0xFFFAE6A0,0xFFF7BC45,0xFFF28433,0xFFD83B37,0xFF9F1839,
        0xFFFFB6A4,0xFFF5799A,0xFFB852A5,0xFF8052BD,0xFF472476,
        0xFFA8E2D5,0xFF54BCB1,0xFF218A8C,0xFF145D67,0xFFD9DF71,
        0xFFA8CB4B,0xFF6FA548,0xFF38733C,0xFF1E4A34,0xFFA0BD8C,
        0xFFE8C4A3,0xFFC89062,0xFFA36540,0xFF753B25,0xFF452A25,
        0xFFFAE7D7,0xFFDDAA8F,0xFFAD7471,0xFF75474E,0xFF322932
    };
    private final ArrayDeque<PlantArt> undo = new ArrayDeque<>();
    private final ArrayDeque<PlantArt> redo = new ArrayDeque<>();
    private final Handler saveHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveTask = this::persistDraft;
    private PlantStore store;
    private PlantStore.Draft draft;
    private PixelCanvas paintingSurface;
    private LinearLayout page;
    private TextView tip;
    private int brush = COLORS[26];
    private boolean erase, bucket, finished, committing;
    private int screenGeneration;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        store = new PlantStore(this);
        if (getIntent().getBooleanExtra("resume_draft", false)) {
            setContentView(Ui.text(this,"Gespeicherter Entwurf wird geladen …",20,true));
            AppIo.execute(()->{
                PlantStore.Draft loaded=store.loadDraft();
                AppIo.onMain(()->{
                    if(isFinishing() || isDestroyed())return;
                    draft=loaded!=null ? loaded : PlantStore.Draft.empty(1);
                    buildScreen();
                });
            });
        } else {
            draft=PlantStore.Draft.empty(getIntent().getIntExtra("stage_count", 1));
            buildScreen();
        }
    }

    @Override protected void onPause() {
        saveHandler.removeCallbacks(saveTask);
        if (!finished) persistDraft();
        super.onPause();
    }
    private PlantStore.Draft draftSnapshot() {
        PlantStore.Draft snapshot=PlantStore.Draft.empty(draft.requestedCount);
        snapshot.stages.addAll(draft.stages); // completed stages are immutable
        snapshot.difficulties.addAll(draft.difficulties);
        snapshot.active=draft.active.copy();
        snapshot.colorMode=draft.colorMode;
        snapshot.drawingDeath=draft.drawingDeath;
        return snapshot;
    }
    private void persistDraft() {
        if (finished || committing || draft == null) return;
        final PlantStore.Draft snapshot=draftSnapshot();
        AppIo.execute(()->{
            try { store.saveDraft(snapshot); }
            catch(IOException e){
                android.util.Log.e("GartenKueche","Autosave failed",e);
                AppIo.onMain(()->{if(!isFinishing() && !isDestroyed())
                    Toast.makeText(this,"Entwurf konnte nicht gespeichert werden.",Toast.LENGTH_LONG).show();});
            }
        });
    }
    private void scheduleSave() {
        saveHandler.removeCallbacks(saveTask);
        saveHandler.postDelayed(saveTask, 750);
    }
    private void freshUndo() { undo.clear(); redo.clear(); }
    private void history() {
        undo.addLast(draft.active.copy());
        while (undo.size() > 8) undo.removeFirst();
        redo.clear();
    }
    private void undoAction() {
        if (undo.isEmpty()) { toast("Nichts rückgängig zu machen"); return; }
        redo.addLast(draft.active.copy());
        draft.active = undo.removeLast();
        paintingSurface.dirty();
        scheduleSave();
    }
    private void redoAction() {
        if (redo.isEmpty()) { toast("Nichts zu wiederholen"); return; }
        undo.addLast(draft.active.copy());
        draft.active = redo.removeLast();
        paintingSurface.dirty();
        scheduleSave();
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private void buildScreen() {
        if (draft == null) return;
        screenGeneration++;
        paintingSurface = null;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        page = Ui.column(this);
        page.setPadding(Ui.dp(this, 13), Ui.dp(this, 26), Ui.dp(this, 13), Ui.dp(this, 22));
        scroll.addView(page);
        if (draft.stages.size() >= draft.requestedCount && !draft.drawingDeath) {
            buildDeathChoice();
        } else {
            buildEditor();
        }
        setContentView(scroll);
    }

    private void buildDeathChoice() {
        Ui.add(page, Ui.text(this, "Todesform festlegen", 25, true), 10);
        Ui.add(page, Ui.text(this,
            "Hier siehst du die erste Evolutionsstufe in Grauabstufungen. Du kannst sie übernehmen oder eine eigene Todespflanze zeichnen.",
            15, false), 14);
        ImageView image = new ImageView(this);
        image.setImageBitmap(draft.stages.get(0).grayCopy().bitmap());
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundColor(Color.rgb(231, 225, 216));
        page.addView(image, Ui.spacing(this, -1, Ui.dp(this, 280), 14));
        Button accept = Ui.button(this, "Passt – Pflanze ins Beet");
        accept.setOnClickListener(v -> completePlant(draft.stages.get(0).grayCopy()));
        Ui.add(page, accept, 10);
        Button custom = Ui.button(this, "Selber zeichnen");
        custom.setOnClickListener(v -> {
            draft.drawingDeath = true;
            draft.colorMode = false;
            draft.active = new PlantArt();
            freshUndo();
            persistDraft();
            buildScreen();
        });
        Ui.add(page, custom, 10);
        Ui.add(page, Ui.text(this, "Die Todesform ist ein zusätzlicher Zustand und zählt nicht zu den Evolutionsstufen.", 12, false), 0);
    }
    private void buildEditor() {
        String step = draft.drawingDeath ? "Eigene Todesform" :
                "Evolution " + (draft.stages.size() + 1) + "/" + draft.requestedCount;
        Ui.add(page, Ui.text(this, step, 24, true), 5);
        tip = Ui.text(this,
                draft.colorMode
                    ? "Schritt B: "+draft.active.detectedRegions()+" geschlossene Bereiche erkannt. Alle Innenflächen lassen sich mit 40 Farben bemalen."
                    : "Schritt A: Zeichne mit beliebig vielen Strichen einen Umriss. Sobald eine erkennbare Innenfläche entsteht, kannst du mit Ausmalen weitermachen. Kleine Lücken werden automatisch geschlossen.",
                14, false);
        Ui.add(page, tip, 12);
        paintingSurface = new PixelCanvas();
        Ui.add(page, paintingSurface, 10);
        LinearLayout zoomRow = Ui.row(this);
        Button less = Ui.button(this, "− Zoom");
        Button more = Ui.button(this, "+ Zoom");
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, Ui.dp(this, 43), 1f);
        half.rightMargin = Ui.dp(this, 6);
        zoomRow.addView(less, half);
        zoomRow.addView(more, new LinearLayout.LayoutParams(0, Ui.dp(this, 43), 1f));
        less.setOnClickListener(v -> paintingSurface.zoom(false));
        more.setOnClickListener(v -> paintingSurface.zoom(true));
        Ui.add(page, zoomRow, 7);
        LinearLayout historyRow = Ui.row(this);
        Button undoButton = Ui.button(this, "↶ Zurück");
        Button redoButton = Ui.button(this, "↷ Wiederholen");
        LinearLayout.LayoutParams historyLeft = new LinearLayout.LayoutParams(0, Ui.dp(this, 43), 1f);
        historyLeft.rightMargin = Ui.dp(this, 6);
        historyRow.addView(undoButton, historyLeft);
        historyRow.addView(redoButton, new LinearLayout.LayoutParams(0, Ui.dp(this, 43), 1f));
        undoButton.setOnClickListener(v -> undoAction());
        redoButton.setOnClickListener(v -> redoAction());
        Ui.add(page, historyRow, 10);

        if (draft.colorMode) {
            GridLayout palette = new GridLayout(this);
            palette.setColumnCount(8);
            for (int color : COLORS) {
                View swatch = new View(this);
                android.graphics.drawable.GradientDrawable bg = Ui.shape(color, Ui.dp(this, 3));
                bg.setStroke(Ui.dp(this, 1), Color.rgb(115, 106, 99));
                swatch.setBackground(bg);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = Ui.dp(this, 32); lp.height = Ui.dp(this, 32);
                lp.setMargins(Ui.dp(this, 2), Ui.dp(this, 2), Ui.dp(this, 2), Ui.dp(this, 2));
                palette.addView(swatch, lp);
                swatch.setContentDescription("Malfarbe " + String.format("#%06X", color & 0xffffff));
                swatch.setOnClickListener(v -> {
                    brush = color; erase = false; bucket = false;
                    tip.setText("Stift aktiviert: " + String.format("#%06X", color & 0xffffff));
                });
            }
            Ui.add(page, palette, 10);
            LinearLayout actions = Ui.row(this);
            Button fill = Ui.button(this, "Eimer");
            Button all = Ui.button(this, "Alle füllen");
            Button eraser = Ui.button(this, "Radierer");
            LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, Ui.dp(this, 43), 1f);
            a.rightMargin = Ui.dp(this, 4);
            actions.addView(fill, a);
            LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, Ui.dp(this, 43), 1f);
            b.rightMargin = Ui.dp(this, 4);
            actions.addView(all, b);
            actions.addView(eraser, new LinearLayout.LayoutParams(0, Ui.dp(this, 43), 1f));
            fill.setOnClickListener(v -> { bucket = true; erase = false; tip.setText("Fülleimer: Tippe auf eine zusammenhängende Innenfläche."); });
            all.setOnClickListener(v -> { history(); draft.active.fillAll(brush); paintingSurface.dirty(); scheduleSave(); });
            eraser.setOnClickListener(v -> { erase = true; bucket = false; tip.setText("Radierer: Tippe oder ziehe innerhalb der Kontur."); });
            Ui.add(page, actions, 10);
        }
        Button next = Ui.button(this, draft.colorMode
                ? (draft.drawingDeath ? "Todesform fertigstellen" : "Evolution fertigstellen")
                : "Kontur prüfen und ausmalen");
        next.setOnClickListener(v -> {
            if (!draft.colorMode) {
                // Validate off-thread: complex multi-stroke art must NEVER stall touch/UI.
                next.setEnabled(false);
                paintingSurface.setEnabled(false);
                tip.setText("Kontur und Innenfläche werden im Hintergrund geprüft …");
                final int generation=screenGeneration;
                final PlantArt candidate=draft.active.copy();
                AppIo.execute(()->{
                    boolean success=candidate.validate();
                    AppIo.onMain(()->{
                        if(isFinishing() || isDestroyed() || generation!=screenGeneration)return;
                        next.setEnabled(true);
                        paintingSurface.setEnabled(true);
                        if(!success){
                            tip.setText("Noch keine abgeschlossene Innenfläche erkannt. Zeichne einfach weiter.");
                            new AlertDialog.Builder(this).setTitle("Noch keine Innenfläche erkannt")
                                .setMessage("Mehrere Striche und überstehende Linien sind erlaubt. Es muss nur eine klar eingeschlossene Fläche geben; kleine Lücken werden automatisch geschlossen.")
                                .setPositiveButton("Verstanden",null).show();
                            return;
                        }
                        draft.active=candidate;
                        draft.colorMode=true;
                        freshUndo();
                        persistDraft();
                        buildScreen();
                    });
                });
            } else if (draft.drawingDeath) {
                completePlant(draft.active.copy());
            } else {
                selectDifficulty();
            }
        });
        Ui.add(page, next, 10);
        Ui.add(page, Ui.text(this,
            "Entwurf wird lokal gesichert. Vergrößere mit den Zoom-Tasten; verschiebe den Ausschnitt mit zwei Fingern. Alte Android-Versionen werden unterstützt.",
            12, false), 0);
    }
    private void selectDifficulty() {
        final int[] chosen = {0};
        new AlertDialog.Builder(this)
            .setTitle("Schwierigkeit für Evolutionsstufe " + (draft.stages.size()+1))
            .setSingleChoiceItems(PlantStore.DIFFICULTIES, 0, (dlg, which) -> chosen[0] = which)
            .setNegativeButton("Zurück", null)
            .setPositiveButton("Speichern und weiter", (dlg, which) -> {
                draft.stages.add(draft.active.copy());
                draft.difficulties.add(chosen[0]);
                draft.active = new PlantArt();
                draft.colorMode = false;
                freshUndo();
                persistDraft();
                buildScreen();
            })
            .show();
    }
    private void completePlant(PlantArt death) {
        if(committing || finished)return;
        committing=true;
        PlantStore.Plant plant=new PlantStore.Plant();
        plant.stages.addAll(draft.stages); // immutable once stage is complete
        plant.difficulties.addAll(draft.difficulties);
        plant.death=death;
        Toast.makeText(this,"Pflanze wird sicher gespeichert …",Toast.LENGTH_SHORT).show();
        AppIo.execute(()->{
            IOException failure=null;
            try{
                store.savePlant(plant);
                store.clearDraft();
            }catch(IOException e){failure=e;}
            final IOException error=failure;
            AppIo.onMain(()->{
                if(isFinishing() || isDestroyed())return;
                committing=false;
                if(error!=null){
                    new AlertDialog.Builder(this).setTitle("Speicherung fehlgeschlagen")
                        .setMessage(error.toString()).setPositiveButton("OK",null).show();
                    return;
                }
                finished=true;
                saveHandler.removeCallbacks(saveTask);
                Toast.makeText(this,"Pflanze gespeichert!",Toast.LENGTH_SHORT).show();
                Intent beet=new Intent(this,BeetActivity.class);
                beet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(beet);
                finish();
            });
        });
    }

    private final class PixelCanvas extends View {
        private final Paint bmpPaint = new Paint();
        private final Paint overlayPaint = new Paint();
        private final Paint gridPaint = new Paint();
        private final Paint borderPaint = new Paint();
        private final Paint backgroundPaint = new Paint();
        private final int[] renderScratch = new int[PlantArt.TOTAL];
        private final Rect src = new Rect();
        private final RectF dst = new RectF();
        private Bitmap cached, previous;
        private boolean dirty = false, panning;
        private boolean gestureBlocked;
        private int zoom = 1, ox, oy, previousX=-1, previousY=-1;
        private float panX, panY;

        PixelCanvas() {
            super(EditorActivity.this);
            bmpPaint.setFilterBitmap(false);
            backgroundPaint.setColor(0xfffdfaf4);
            cached=draft.active.bitmap();
            overlayPaint.setFilterBitmap(false);
            overlayPaint.setAlpha(51); // 20% opacity
            gridPaint.setColor(0x55847A70);
            gridPaint.setStrokeWidth(1f);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setColor(PlantArt.PURPLE);
            borderPaint.setStrokeWidth(Ui.dp(EditorActivity.this, 2));
            if (!draft.drawingDeath && !draft.stages.isEmpty())
                previous = draft.stages.get(draft.stages.size() - 1).bitmap();
        }
        @Override protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec);
            setMeasuredDimension(w, w);
        }
        // Resynchronize the same bitmap only after Undo, Redo or a bulk fill.
        void dirty() { dirty = true; postInvalidateOnAnimation(); }
        void zoom(boolean bigger) {
            int before = zoom;
            zoom = bigger ? zoom == 1 ? 2 : zoom == 2 ? 5 : 10 :
                   zoom == 10 ? 5 : zoom == 5 ? 2 : 1;
            if (zoom == before) return;
            int oldVisible = PlantArt.SIZE / before, newVisible = PlantArt.SIZE / zoom;
            ox = Math.max(0, Math.min(PlantArt.SIZE-newVisible,
                    Math.round(ox + oldVisible/2f - newVisible/2f)));
            oy = Math.max(0, Math.min(PlantArt.SIZE-newVisible,
                    Math.round(oy + oldVisible/2f - newVisible/2f)));
            invalidate();
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float side = Math.min(getWidth(), getHeight());
            int visible = PlantArt.SIZE / zoom;
            src.set(ox, oy, ox + visible, oy + visible);
            dst.set(0, 0, side, side);
            c.drawRect(dst, backgroundPaint);
            if (previous != null) c.drawBitmap(previous, src, dst, overlayPaint);
            if (dirty) {
                draft.active.renderInto(cached,renderScratch);
                dirty=false;
            }
            c.drawBitmap(cached, src, dst, bmpPaint);
            int step = zoom >= 5 ? 1 : zoom >= 2 ? 5 : 10;
            for (int x=ox; x<=ox+visible; x++)
                if (x % step == 0) {
                    float px=(x-ox)*side/visible;
                    c.drawLine(px, 0, px, side, gridPaint);
                }
            for (int y=oy; y<=oy+visible; y++)
                if (y % step == 0) {
                    float py=(y-oy)*side/visible;
                    c.drawLine(0, py, side, py, gridPaint);
                }
            c.drawRect(dst, borderPaint);
        }
        private int lx(float px) {
            return Math.max(0, Math.min(PlantArt.SIZE-1, ox + (int)(px * (PlantArt.SIZE / zoom) / getWidth())));
        }
        private int ly(float py) {
            return Math.max(0, Math.min(PlantArt.SIZE-1, oy + (int)(py * (PlantArt.SIZE / zoom) / getHeight())));
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if(!isEnabled())return true;
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN && getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            if (action == MotionEvent.ACTION_POINTER_DOWN && e.getPointerCount() >= 2) {
                panning=true; gestureBlocked=true;
                panX=(e.getX(0)+e.getX(1))/2; panY=(e.getY(0)+e.getY(1))/2;
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE && panning && e.getPointerCount() >= 2) {
                float nx=(e.getX(0)+e.getX(1))/2, ny=(e.getY(0)+e.getY(1))/2;
                int visible=PlantArt.SIZE/zoom;
                ox=Math.max(0,Math.min(PlantArt.SIZE-visible,Math.round(ox-(nx-panX)*visible/getWidth())));
                oy=Math.max(0,Math.min(PlantArt.SIZE-visible,Math.round(oy-(ny-panY)*visible/getHeight())));
                panX=nx;panY=ny;invalidate();return true;
            }
            if (action == MotionEvent.ACTION_POINTER_UP) {
                previousX=-1; previousY=-1; gestureBlocked=true; return true;
            }
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                if (previousX >= 0) scheduleSave();
                previousX=-1; previousY=-1; panning=false; gestureBlocked=false;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
            if (gestureBlocked || panning || (action!=MotionEvent.ACTION_DOWN && action!=MotionEvent.ACTION_MOVE))
                return true;
            int x=lx(e.getX()), y=ly(e.getY());
            if (action == MotionEvent.ACTION_DOWN) {
                history();
                previousX=x; previousY=y;
                apply(x,y);
            } else if (previousX >= 0) {
                if (draft.colorMode && bucket) return true;
                drawLine(previousX,previousY,x,y);
                previousX=x;previousY=y;
            }
            postInvalidateOnAnimation();return true;
        }
        private void drawLine(int x0,int y0,int x1,int y1) {
            int dx=Math.abs(x1-x0),dy=Math.abs(y1-y0),sx=x0<x1?1:-1,sy=y0<y1?1:-1;
            int error=dx-dy;
            while(true) {
                apply(x0,y0);
                if(x0==x1 && y0==y1) break;
                int twice=2*error;
                if(twice > -dy) {error-=dy;x0+=sx;}
                if(twice < dx) {error+=dx;y0+=sy;}
            }
        }
        private void apply(int x,int y) {
            if(!draft.colorMode) draft.active.drawContour(x,y);
            else if(bucket){
                draft.active.fillRegion(x,y,brush);
                dirty=true; // bulk fill is rare; rerender pixels into the SAME bitmap
                return;
            }else draft.active.colorPixel(x,y,erase ? Color.TRANSPARENT : brush);
            // Touch strokes update only the changed pixel, not the whole 300x300 bitmap.
            cached.setPixel(x,y,draft.active.renderedPixel(x,y));
        }
        @Override protected void onDetachedFromWindow() {
            if (cached != null) { cached.recycle(); cached=null; }
            if (previous != null) { previous.recycle(); previous=null; }
            super.onDetachedFromWindow();
        }
    }
}
