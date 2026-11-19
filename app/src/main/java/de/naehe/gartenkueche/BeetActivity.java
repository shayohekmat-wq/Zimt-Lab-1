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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimized 800x600 pixel bed: NO disk I/O or GZIP on the UI thread.
 * The background and active stage bitmaps are cached, care is evaluated on a worker.
 */
public class BeetActivity extends Activity {
    private final List<PlantStore.Plant> plants = new ArrayList<>();
    private final LinkedHashMap<String,Bitmap> sprites = new LinkedHashMap<>(20,0.75f,true);
    private final HashSet<String> pendingSprites = new HashSet<>();
    private static final int MAX_CACHED_SPRITES = 14;
    private PlantStore store;
    private BeetCanvas bed;
    private TextView status, rules, hint;
    private Button wateringToggle;
    private boolean wateringMode;
    private int loadGeneration;
    private boolean screenActive;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store=new PlantStore(getApplicationContext());
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout page=Ui.column(this);
        page.setPadding(Ui.dp(this,17),Ui.dp(this,27),Ui.dp(this,17),Ui.dp(this,26));
        scroll.addView(page);
        Ui.add(page,Ui.text(this,"Beet",29,true),5);
        Ui.add(page,Ui.text(this,"800 × 600 Pixel – deine selbst gezeichneten Pflanzen",13,false),16);
        bed=new BeetCanvas();
        Ui.add(page,bed,12);
        status=Ui.text(this,"Deine Pflanzen werden im Hintergrund geladen …",14,false);
        Ui.add(page,status,9);
        rules=Ui.text(this,"",13,false);
        Ui.add(page,rules,12);
        wateringToggle=Ui.button(this,"Gießkanne aktivieren");
        wateringToggle.setOnClickListener(v->{
            wateringMode=!wateringMode;
            wateringToggle.setText(wateringMode ? "Gießkanne ablegen" : "Gießkanne aktivieren");
            hint.setText(wateringMode
                ? "Finger fünf Sekunden über einer Pflanze halten. Längeres Halten zählt nur einmal."
                : "Pflanze lange berühren, dann ziehen. Verschieben und Gießen sind getrennte Modi.");
            bed.cancelAll();
        });
        Ui.add(page,wateringToggle,8);
        Button create=Ui.button(this,"Neue Pflanze erstellen");
        create.setOnClickListener(v->startActivity(new Intent(this,StageSetupActivity.class)));
        Ui.add(page,create,9);
        hint=Ui.text(this,"Pflanze lange berühren und dann verschieben.",13,false);
        Ui.add(page,hint,9);
        Ui.add(page,Ui.text(this,
            "0.0.5 Performance-Version: Die Pflegefristen laufen offline weiter. Speichern, Bildaufbau und Laden blockieren nicht mehr die Bedienoberfläche.",
            12,false),0);
        setContentView(scroll);
    }
    @Override protected void onResume(){
        super.onResume();
        screenActive=true;
        requestReload();
    }
    @Override protected void onPause(){
        screenActive=false;loadGeneration++;
        if(bed!=null)bed.cancelAll();
        super.onPause();
    }
    @Override protected void onDestroy(){
        screenActive=false;loadGeneration++;
        if(bed!=null)bed.cancelAll();
        clearSprites();
        super.onDestroy();
    }
    private void clearSprites(){
        for(Bitmap b:sprites.values()) if(b!=null && !b.isRecycled())b.recycle();
        sprites.clear();pendingSprites.clear();
    }
    private void requestReload(){
        final int generation=++loadGeneration;
        if(bed!=null)bed.cancelAll();
        if(status!=null)status.setText("Pflanzen werden geladen …");
        AppIo.execute(()->{
            long started=SystemClock.elapsedRealtime();
            List<PlantStore.Plant> loaded=store.loadPlants();
            long now=System.currentTimeMillis();
            for(PlantStore.Plant plant:loaded){
                CareEngine.Result result=CareEngine.evaluate(plant,now);
                if(result.changed){
                    try{store.saveState(PlantStore.StateSnapshot.capture(plant));}
                    catch(IOException ex){android.util.Log.e("GartenKueche","Care state save",ex);}
                }
            }
            long duration=SystemClock.elapsedRealtime()-started;
            android.util.Log.d("GartenKuechePerf","loadPlants+care worker "+duration+" ms; plants="+loaded.size());
            AppIo.onMain(()->{
                if(!screenActive || generation!=loadGeneration || isFinishing() || isDestroyed())return;
                clearSprites();
                plants.clear();plants.addAll(loaded);
                updateStatus(plants.isEmpty()?null:plants.get(0));
                bed.invalidate();
            });
        });
    }
    private void updateStatus(PlantStore.Plant selected){
        int n=0,dead=0;
        for(PlantStore.Plant p:plants){n+=p.wateringCount;if(p.dead)dead++;}
        status.setText(plants.isEmpty()
            ? "Noch keine Pflanze vorhanden. Erstelle deine erste Evolutionspflanze."
            : plants.size()+(plants.size()==1?" Pflanze":" Pflanzen")+
              " im Beet · "+n+" Gießvorgänge"+(dead>0?" · "+dead+" tot":""));
        rules.setText(selected==null?"":CareEngine.rulesFor(selected,System.currentTimeMillis()));
    }
    /** Capture tiny mutable state on UI, write it off-thread in strict event order. */
    private void enqueueStateSave(PlantStore.Plant plant){
        PlantStore.StateSnapshot snapshot=PlantStore.StateSnapshot.capture(plant);
        AppIo.execute(()->{
            try{store.saveState(snapshot);}
            catch(IOException ex){
                android.util.Log.e("GartenKueche","State save failed",ex);
                AppIo.onMain(()->{
                    if(screenActive && !isFinishing())Toast.makeText(this,
                        "Pflege-/Positionsdaten konnten nicht gespeichert werden!",Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private String key(PlantStore.Plant p){return p.id+":"+p.stage+":"+p.dead;}
    /** Off-thread artwork preparation; no allocations of 300x300 bitmaps in onDraw. */
    private Bitmap sprite(PlantStore.Plant plant) {
        String k=key(plant);
        Bitmap cached=sprites.get(k);
        if(cached!=null && !cached.isRecycled())return cached;
        if(pendingSprites.add(k)){
            final int generation=loadGeneration;
            final PlantArt artwork=plant.dead ? plant.death : plant.stages.get(plant.stage-1);
            AppIo.execute(()->{
                Bitmap bitmap=artwork.bitmap();
                AppIo.onMain(()->{
                    pendingSprites.remove(k);
                    if(!screenActive || generation!=loadGeneration || isFinishing() || isDestroyed()){
                        bitmap.recycle();return;
                    }
                    sprites.put(k,bitmap);
                    while(sprites.size()>MAX_CACHED_SPRITES){
                        Iterator<Map.Entry<String,Bitmap>> it=sprites.entrySet().iterator();
                        Map.Entry<String,Bitmap> eldest=it.next();
                        it.remove();
                        if(!eldest.getValue().isRecycled())eldest.getValue().recycle();
                    }
                    bed.invalidate();
                });
            });
        }
        return null;
    }

    private final class BeetCanvas extends View {
        private final Paint p=new Paint();
        private final Paint spritePaint=new Paint();
        private final Handler handler=new Handler(Looper.getMainLooper());
        private final int longPressMs=ViewConfiguration.getLongPressTimeout();
        private final Rect imageSrc=new Rect(0,0,300,300);
        private final RectF imageDst=new RectF();
        private final Path spout=new Path();
        private final Bitmap staticBackground;
        private PlantStore.Plant held,pressed,moving;
        private long holdStart;
        private boolean counted,pointerDown,dragging;
        private float initialX,initialY,pointerX,pointerY,offsetX,offsetY;
        private Runnable pendingLongPress,pendingWater;

        BeetCanvas(){
            super(BeetActivity.this);
            spritePaint.setFilterBitmap(false);
            staticBackground=Bitmap.createBitmap(800,600,Bitmap.Config.RGB_565);
            Canvas c=new Canvas(staticBackground);
            Paint backgroundPaint=new Paint();
            backgroundPaint.setStyle(Paint.Style.FILL);
            backgroundPaint.setColor(Color.rgb(225,212,194));
            c.drawRect(0,0,800,600,backgroundPaint);
            backgroundPaint.setStyle(Paint.Style.STROKE);
            backgroundPaint.setColor(0x606B5B4C);
            backgroundPaint.setStrokeWidth(1f);
            for(int x=0;x<=800;x+=20)c.drawLine(x,0,x,600,backgroundPaint);
            for(int y=0;y<=600;y+=20)c.drawLine(0,y,800,y,backgroundPaint);
            backgroundPaint.setColor(0xff65503e);
            backgroundPaint.setStrokeWidth(3f);
            c.drawRect(1.5f,1.5f,798.5f,598.5f,backgroundPaint);
        }
        @Override protected void onMeasure(int wSpec,int hSpec){
            int width=MeasureSpec.getSize(wSpec);
            setMeasuredDimension(width,Math.round(width*600f/800f));
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            c.save();
            c.scale(getWidth()/800f,getHeight()/600f);
            p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);
            c.drawBitmap(staticBackground,0,0,p);
            for(PlantStore.Plant plant:plants){
                if(plant.x+150<0||plant.x-150>800||plant.y<0||plant.y-300>600)continue;
                Bitmap img=sprite(plant);
                if(img==null)continue;
                imageDst.set(plant.x-150,plant.y-300,plant.x+150,plant.y);
                c.drawBitmap(img,imageSrc,imageDst,spritePaint);
                if(plant==moving){
                    p.setColor(0xff693e91);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
                    c.drawRect(imageDst,p);
                    p.setStyle(Paint.Style.FILL);
                }
            }
            if(wateringMode && pointerDown){
                drawCan(c,pointerX,pointerY);
                if(held!=null && !counted){
                    float progress=Math.min(1f,(SystemClock.elapsedRealtime()-holdStart)/5000f);
                    float x=Math.max(8f,Math.min(632f,held.x-80f));
                    float y=Math.max(10f,held.y-315f);
                    p.setStyle(Paint.Style.FILL);p.setColor(0xff5d5148);
                    c.drawRect(x,y,x+160,y+13,p);
                    p.setColor(0xff4e9e7b);
                    c.drawRect(x,y,x+160*progress,y+13,p);
                    postInvalidateDelayed(100);
                }
            }
            c.restore();
        }
        private void drawCan(Canvas c,float x,float y){
            p.setStyle(Paint.Style.FILL);p.setColor(0xff687d85);
            c.drawRoundRect(x-30,y-20,x+12,y+20,4,4,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);
            c.drawArc(x-49,y-15,x-22,y+10,90,180,false,p);
            spout.reset();spout.moveTo(x+10,y-13);spout.lineTo(x+33,y-20);
            c.drawPath(spout,p);c.drawLine(x+28,y-23,x+40,y-23,p);
            p.setStyle(Paint.Style.FILL);p.setColor(0xff488eb3);
            c.drawCircle(x+40,y-8,3,p);
        }
        private float wx(float x){return x*800f/Math.max(1,getWidth());}
        private float wy(float y){return y*600f/Math.max(1,getHeight());}
        private PlantStore.Plant plantAt(float x,float y){
            for(int i=plants.size()-1;i>=0;i--){
                PlantStore.Plant pl=plants.get(i);
                if(x>=pl.x-150&&x<=pl.x+150&&y>=pl.y-300&&y<=pl.y)return pl;
            }
            return null;
        }
        private void clearTimers(){
            if(pendingLongPress!=null)handler.removeCallbacks(pendingLongPress);
            if(pendingWater!=null)handler.removeCallbacks(pendingWater);
            pendingLongPress=null;pendingWater=null;
        }
        void cancelAll(){
            clearTimers();pointerDown=false;counted=false;held=null;pressed=null;
            moving=null;dragging=false;invalidate();
        }
        private void startWater(PlantStore.Plant candidate){
            if(candidate==held && pointerDown)return;
            if(pendingWater!=null)handler.removeCallbacks(pendingWater);
            held=candidate;counted=false;holdStart=SystemClock.elapsedRealtime();
            if(candidate==null||candidate.dead){invalidate();return;}
            final PlantStore.Plant expected=candidate;
            pendingWater=()->{
                if(!pointerDown||!wateringMode||held!=expected||counted)return;
                if(SystemClock.elapsedRealtime()-holdStart<5000L)return;
                counted=true;
                CareEngine.Result result=CareEngine.water(expected,System.currentTimeMillis());
                if(result.changed)enqueueStateSave(expected);
                updateStatus(expected);
                Toast.makeText(BeetActivity.this,result.message(),Toast.LENGTH_SHORT).show();
                invalidate();
            };
            handler.postDelayed(pendingWater,5000L);
            invalidate();
        }
        @Override public boolean onTouchEvent(MotionEvent ev){
            int action=ev.getActionMasked();
            if(ev.getPointerCount()>1){cancelAll();return true;}
            float x=wx(ev.getX()),y=wy(ev.getY());
            if(action==MotionEvent.ACTION_DOWN){
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                pointerDown=true;initialX=x;initialY=y;pointerX=x;pointerY=y;
                if(wateringMode)startWater(plantAt(x,y));
                else{
                    pressed=plantAt(x,y);
                    if(pressed!=null){
                        final PlantStore.Plant candidate=pressed;
                        pendingLongPress=()->{
                            if(!pointerDown||pressed!=candidate||wateringMode)return;
                            moving=candidate;dragging=true;
                            offsetX=candidate.x-pointerX;offsetY=candidate.y-pointerY;
                            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                            invalidate();
                        };
                        handler.postDelayed(pendingLongPress,longPressMs);
                    }
                }
                return true;
            }
            if(action==MotionEvent.ACTION_MOVE && pointerDown){
                pointerX=x;pointerY=y;
                if(wateringMode){
                    PlantStore.Plant target=plantAt(x,y);
                    if(target!=held)startWater(target);
                }else if(dragging && moving!=null){
                    moving.x=Math.max(20,Math.min(780,Math.round(x+offsetX)));
                    moving.y=Math.max(40,Math.min(590,Math.round(y+offsetY)));
                }else if(Math.hypot(x-initialX,y-initialY)>25){
                    if(pendingLongPress!=null)handler.removeCallbacks(pendingLongPress);
                    pressed=null;
                }
                postInvalidateOnAnimation();
                return true;
            }
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                if(action==MotionEvent.ACTION_UP && dragging && moving!=null)enqueueStateSave(moving);
                cancelAll();
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
            return true;
        }
        @Override protected void onDetachedFromWindow(){
            cancelAll();
            if(!staticBackground.isRecycled())staticBackground.recycle();
            super.onDetachedFromWindow();
        }
    }
}
