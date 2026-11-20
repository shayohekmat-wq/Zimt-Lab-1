package de.naehe.gartenkueche;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.SeekBar;
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
 * 0.0.6 — original-size, multi-contour sprites and local 1..80x care simulation.
 * Artwork is computed/cropped off-thread; user input and 5-second watering stay realtime.
 */
public class BeetActivity extends Activity {
    private final List<PlantStore.Plant> plants=new ArrayList<>();
    private static final class Sprite {
        final Bitmap bitmap;
        final PlantArt.Bounds bounds;
        Sprite(Bitmap bitmap,PlantArt.Bounds bounds){this.bitmap=bitmap;this.bounds=bounds;}
    }
    private final LinkedHashMap<String,Sprite> sprites=new LinkedHashMap<>(20,0.75f,true);
    private final HashSet<String> pendingSprites=new HashSet<>();
    private static final int MAX_CACHED_SPRITES=40;
    private final Handler ticker=new Handler(Looper.getMainLooper());
    private final Runnable tickerTask=this::tick;
    private PlantStore store;
    private TestClock clock;
    private BeetCanvas bed;
    private TextView status,rules,hint,speedValue,ageValue,selectedValue,dayLengthValue,clockHint;
    private Button wateringToggle;
    private boolean wateringMode,screenActive,loading;
    private int loadGeneration;
    private String selectedPlantId;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        store=new PlantStore(getApplicationContext());
        clock=new TestClock(getApplicationContext());
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout page=Ui.column(this);
        page.setPadding(Ui.dp(this,17),Ui.dp(this,26),Ui.dp(this,17),Ui.dp(this,24));
        scroll.addView(page);
        Ui.add(page,Ui.text(this,"Beet",29,true),5);
        Ui.add(page,Ui.text(this,"800 × 600 Pixel – deine selbst gezeichneten Pflanzen",13,false),14);
        bed=new BeetCanvas();
        Ui.add(page,bed,14);
        addTimelapseCard(page);
        status=Ui.text(this,"Pflanzen werden im Hintergrund geladen …",14,false);
        Ui.add(page,status,9);
        rules=Ui.text(this,"",13,false);
        Ui.add(page,rules,13);
        wateringToggle=Ui.button(this,"Gießkanne aktivieren");
        wateringToggle.setOnClickListener(v->{
            wateringMode=!wateringMode;
            wateringToggle.setText(wateringMode?"Gießkanne ablegen":"Gießkanne aktivieren");
            hint.setText(wateringMode
                ?"Finger fünf echte Sekunden über einer Pflanze halten. Ein Halt zählt einmal."
                :"Tippen: Pflanze auswählen. Lange drücken: gesamte Pflanze verschieben.");
            bed.cancelAll();
        });
        Ui.add(page,wateringToggle,8);
        Button create=Ui.button(this,"Neue Pflanze erstellen");
        create.setOnClickListener(v->startActivity(new Intent(this,StageSetupActivity.class)));
        Ui.add(page,create,9);
        hint=Ui.text(this,"Tippe auf eine Pflanze, um ihren Lebenstag zu sehen. Zum Verschieben lange drücken.",13,false);
        Ui.add(page,hint,9);
        Ui.add(page,Ui.text(this,
            "Zeitraffer ist ausschließlich lokal zum Testen. Bei 2–80× pausiert die simulierte Zeit außerhalb des Beets. Die Gießkanne benötigt weiterhin fünf echte Sekunden.",
            12,false),0);
        setContentView(scroll);
    }

    private TextView lightText(String text,int size,boolean bold){
        TextView tv=Ui.text(this,text,size,bold);
        tv.setTextColor(0xfff2ebf7);
        return tv;
    }
    private void addTimelapseCard(LinearLayout page){
        LinearLayout card=Ui.column(this);
        card.setPadding(Ui.dp(this,18),Ui.dp(this,17),Ui.dp(this,18),Ui.dp(this,17));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xff292830,0xff3a3044});
        bg.setCornerRadius(Ui.dp(this,19));
        bg.setStroke(Ui.dp(this,1),0xff705683);
        card.setBackground(bg);
        Ui.add(page,card,14);

        LinearLayout titleRow=Ui.row(this);
        TextView header=lightText("◷  ZEITRAFFER  ·  TESTLABOR",13,true);
        titleRow.addView(header,new LinearLayout.LayoutParams(0,-2,1f));
        speedValue=lightText(clock.speed()+"×",21,true);
        speedValue.setTextColor(0xffd6b1ff);
        titleRow.addView(speedValue);
        Ui.add(card,titleRow,5);

        SeekBar slider=new SeekBar(this);
        slider.setMax(79);slider.setProgress(clock.speed()-1);
        slider.setProgressTintList(ColorStateList.valueOf(0xffa880ed));
        slider.setThumbTintList(ColorStateList.valueOf(0xffcfb0ff));
        slider.setProgressBackgroundTintList(ColorStateList.valueOf(0xff665b75));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
                if(fromUser)speedValue.setText((progress+1)+"×");
            }
            @Override public void onStartTrackingTouch(SeekBar bar){}
            @Override public void onStopTrackingTouch(SeekBar bar){
                clock.setSpeed(bar.getProgress()+1); // continuous time, no jump
                updateTimeLabels();
                updateStatus(selectedPlant());
            }
        });
        Ui.add(card,slider,0);

        LinearLayout marks=Ui.row(this);
        String[] positions={"1×","20×","40×","60×","80×"};
        for(String item:positions){
            TextView tick=lightText(item,11,false);
            tick.setTextColor(0xffb5a6c4);
            tick.setGravity(android.view.Gravity.CENTER);
            marks.addView(tick,new LinearLayout.LayoutParams(0,-2,1f));
        }
        Ui.add(card,marks,14);

        View separator=new View(this);
        separator.setBackgroundColor(0xff5b4c66);
        card.addView(separator,new LinearLayout.LayoutParams(-1,Ui.dp(this,1)));
        LinearLayout space=Ui.column(this);
        Ui.add(card,space,4);

        LinearLayout ageRow=Ui.row(this);
        LinearLayout left=Ui.column(this);
        TextView ageCaption=lightText("PFLANZENALTER",11,true);
        ageCaption.setTextColor(0xffb4a5bf);
        Ui.add(left,ageCaption,2);
        ageValue=lightText("Tag –",25,true);
        Ui.add(left,ageValue,1);
        selectedValue=lightText("Pflanze auswählen",11,false);
        selectedValue.setTextColor(0xffbaa8c5);
        Ui.add(left,selectedValue,0);
        ageRow.addView(left,new LinearLayout.LayoutParams(0,-2,1f));

        LinearLayout right=Ui.column(this);
        TextView durationCaption=lightText("1 PFLANZENTAG",11,true);
        durationCaption.setTextColor(0xffb4a5bf);
        Ui.add(right,durationCaption,3);
        dayLengthValue=lightText("",14,true);
        Ui.add(right,dayLengthValue,2);
        clockHint=lightText("",11,false);
        clockHint.setTextColor(0xffbaa8c5);
        Ui.add(right,clockHint,0);
        ageRow.addView(right,new LinearLayout.LayoutParams(0,-2,1f));
        Ui.add(card,ageRow,0);
        updateTimeLabels();
    }

    @Override protected void onResume(){
        super.onResume();
        clock.resume();
        screenActive=true;
        requestReload();
        ticker.removeCallbacks(tickerTask);
        ticker.postDelayed(tickerTask,3000);
    }
    @Override protected void onPause(){
        screenActive=false;loadGeneration++;loading=false;
        ticker.removeCallbacks(tickerTask);
        clock.pause();
        if(bed!=null)bed.cancelAll();
        super.onPause();
    }
    @Override protected void onDestroy(){
        screenActive=false;loadGeneration++;
        ticker.removeCallbacks(tickerTask);
        if(bed!=null)bed.cancelAll();
        clearSprites();
        super.onDestroy();
    }
    private void clearSprites(){
        for(Sprite s:sprites.values())
            if(s.bitmap!=null && !s.bitmap.isRecycled())s.bitmap.recycle();
        sprites.clear();pendingSprites.clear();
    }
    private PlantStore.Plant selectedPlant(){
        for(PlantStore.Plant p:plants)if(p.id.equals(selectedPlantId))return p;
        return plants.isEmpty()?null:plants.get(0);
    }
    private void updateTimeLabels(){
        if(ageValue==null)return;
        PlantStore.Plant selected=selectedPlant();
        speedValue.setText(clock.speed()+"×");
        ageValue.setText(selected==null?"Tag –":"Tag "+clock.plantDay(
            selected.createdAtSimUtc>0?selected.createdAtSimUtc:selected.createdAtUtc));
        long daySeconds=Math.round(VirtualTimeMath.realMillisForOneDay(clock.speed())/1000.0);
        long hours=daySeconds/3600;
        long minutes=(daySeconds%3600)/60;
        long seconds=daySeconds%60;
        String readable=(hours>0 ? hours+" Std. " : "")+
            minutes+" Min."+(seconds>0?" "+seconds+" Sek.":"");
        dayLengthValue.setText(readable);
        clockHint.setText(clock.speed()>1?"pausiert außerhalb des Beets":"Echtzeit");
        int index=selected==null?-1:plants.indexOf(selected);
        selectedValue.setText(index<0?"Pflanze auswählen":"Pflanze "+(index+1)+" · Stufe "+selected.stage);
    }
    private void updateStatus(PlantStore.Plant chosen){
        if(status==null)return;
        int n=0,dead=0;
        for(PlantStore.Plant p:plants){n+=p.wateringCount;if(p.dead)dead++;}
        status.setText(plants.isEmpty()
            ?"Noch keine Pflanze im Beet. Erstelle deine erste Pixelpflanze."
            :plants.size()+(plants.size()==1?" Pflanze":" Pflanzen")+
             " im Beet · "+n+" Gießvorgänge"+(dead>0?" · "+dead+" tot":""));
        if(rules!=null)rules.setText(chosen==null?"":CareEngine.rulesFor(chosen,clock.now()));
        updateTimeLabels();
    }
    private void requestReload(){
        final int generation=++loadGeneration;
        loading=true;
        if(bed!=null)bed.cancelAll();
        if(status!=null)status.setText("Pflanzen werden geladen …");
        final long simulatedNow=clock.now();
        AppIo.execute(()->{
            long start=SystemClock.elapsedRealtime();
            List<PlantStore.Plant> loaded=store.loadPlants();
            for(PlantStore.Plant plant:loaded){
                boolean firstVirtualAge=plant.createdAtSimUtc<=0;
                if(firstVirtualAge)plant.createdAtSimUtc=simulatedNow;
                CareEngine.Result result=CareEngine.evaluate(plant,simulatedNow);
                if(result.changed || firstVirtualAge){
                    try{store.saveState(PlantStore.StateSnapshot.capture(plant));}
                    catch(IOException ex){android.util.Log.e("GartenKueche","Care state save",ex);}
                }
            }
            android.util.Log.d("GartenKuechePerf",
                "Loaded "+loaded.size()+" plants in "+(SystemClock.elapsedRealtime()-start)+"ms");
            AppIo.onMain(()->{
                if(!screenActive || generation!=loadGeneration || isFinishing() || isDestroyed())return;
                loading=false;
                clearSprites();
                plants.clear();plants.addAll(loaded);
                if(selectedPlantId==null && !plants.isEmpty())selectedPlantId=plants.get(0).id;
                updateStatus(selectedPlant());
                bed.invalidate();
            });
        });
    }
    /** Lightweight stage checks; at most once per 3 seconds, only when due. */
    private void tick(){
        if(!screenActive)return;
        if(!loading){
            long now=clock.now();
            boolean changed=false;
            for(PlantStore.Plant plant:plants){
                if(plant.dead)continue;
                boolean deadline=plant.windowEndUtc>0 && now>=plant.windowEndUtc;
                boolean evolution=plant.stage<plant.stages.size() &&
                    plant.evolutionDays>0 &&
                    now>=plant.stageEnteredAtUtc+plant.evolutionDays*CareRules.DAY_MS &&
                    plant.windowWaterings>=plant.minWater;
                if(deadline || evolution){
                    CareEngine.Result result=CareEngine.evaluate(plant,now);
                    if(result.changed){enqueueStateSave(plant);changed=true;}
                }
            }
            if(changed)bed.invalidate();
            updateTimeLabels();
            if(changed)updateStatus(selectedPlant());
        }
        ticker.postDelayed(tickerTask,3000);
    }

    private void enqueueStateSave(PlantStore.Plant plant){
        PlantStore.StateSnapshot snapshot=PlantStore.StateSnapshot.capture(plant);
        AppIo.execute(()->{
            try{store.saveState(snapshot);}
            catch(IOException ex){
                android.util.Log.e("GartenKueche","State save failed",ex);
                AppIo.onMain(()->{
                    if(screenActive && !isFinishing())Toast.makeText(this,
                        "Pflege- oder Positionsdaten konnten nicht gespeichert werden!",Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private String key(PlantStore.Plant p){return p.id+":"+p.stage+":"+p.dead;}
    private Sprite sprite(PlantStore.Plant plant){
        String k=key(plant);
        Sprite cached=sprites.get(k);
        if(cached!=null && !cached.bitmap.isRecycled())return cached;
        if(!pendingSprites.contains(k) &&
            sprites.size()+pendingSprites.size()>=MAX_CACHED_SPRITES)return null;
        if(pendingSprites.add(k)){
            final int generation=loadGeneration;
            final PlantArt art=plant.dead?plant.death:plant.stages.get(plant.stage-1);
            AppIo.execute(()->{
                PlantArt.Bounds bounds=art.bounds();
                Bitmap bmp=art.croppedBitmap(bounds);
                AppIo.onMain(()->{
                    pendingSprites.remove(k);
                    if(!screenActive || generation!=loadGeneration || isFinishing() || isDestroyed()){
                        if(bmp!=null)bmp.recycle();
                        return;
                    }
                    if(bmp==null)return;
                    sprites.put(k,new Sprite(bmp,bounds));
                    while(sprites.size()>MAX_CACHED_SPRITES){
                        Iterator<Map.Entry<String,Sprite>> it=sprites.entrySet().iterator();
                        Map.Entry<String,Sprite> oldest=it.next();
                        it.remove();
                        if(!oldest.getValue().bitmap.isRecycled())oldest.getValue().bitmap.recycle();
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
        private final Rect imageSrc=new Rect();
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
            Paint brush=new Paint();
            brush.setStyle(Paint.Style.FILL);
            brush.setColor(0xffe1d4c2);
            c.drawRect(0,0,800,600,brush);
            brush.setStyle(Paint.Style.STROKE);
            brush.setColor(0x606b5b4c);
            brush.setStrokeWidth(1f);
            for(int x=0;x<=800;x+=20)c.drawLine(x,0,x,600,brush);
            for(int y=0;y<=600;y+=20)c.drawLine(0,y,800,y,brush);
            brush.setColor(0xff65503e);
            brush.setStrokeWidth(3);
            c.drawRect(1.5f,1.5f,798.5f,598.5f,brush);
        }
        @Override protected void onMeasure(int wSpec,int hSpec){
            int width=MeasureSpec.getSize(wSpec);
            setMeasuredDimension(width,Math.round(width*600f/800f));
        }
        private void position(PlantStore.Plant plant,PlantArt.Bounds bounds,RectF destination){
            destination.set(
                plant.x-150+bounds.left, plant.y-300+bounds.top,
                plant.x-150+bounds.right, plant.y-300+bounds.bottom);
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            canvas.save();
            canvas.scale(getWidth()/800f,getHeight()/600f);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            canvas.drawBitmap(staticBackground,0,0,p);
            for(PlantStore.Plant plant:plants){
                Sprite s=sprite(plant);
                if(s==null)continue;
                position(plant,s.bounds,imageDst);
                if(imageDst.right<0 || imageDst.left>800 || imageDst.bottom<0 || imageDst.top>600)continue;
                imageSrc.set(0,0,s.bitmap.getWidth(),s.bitmap.getHeight());
                canvas.drawBitmap(s.bitmap,imageSrc,imageDst,spritePaint);
                if(plant==moving || (plant.id.equals(selectedPlantId) && !wateringMode)){
                    p.setColor(plant==moving?0xffb891e6:0x77764fa0);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(plant==moving?2f:1f);
                    canvas.drawRect(imageDst,p);
                    p.setStyle(Paint.Style.FILL);
                }
            }
            if(wateringMode && pointerDown){
                drawCan(canvas,pointerX,pointerY);
                if(held!=null && !counted){
                    float progress=Math.min(1f,(SystemClock.elapsedRealtime()-holdStart)/5000f);
                    float x=Math.max(8f,Math.min(632f,held.x-80f));
                    float y=Math.max(10f,held.y-315f);
                    p.setStyle(Paint.Style.FILL);p.setColor(0xff5d5148);
                    canvas.drawRect(x,y,x+160,y+13,p);
                    p.setColor(0xff4e9e7b);
                    canvas.drawRect(x,y,x+160*progress,y+13,p);
                    postInvalidateDelayed(100);
                }
            }
            canvas.restore();
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
                PlantStore.Plant plant=plants.get(i);
                Sprite s=sprite(plant);
                if(s==null)continue;
                position(plant,s.bounds,imageDst);
                // Touch area may be larger than actual artwork, without scaling the pixels.
                float minWorld=Ui.dp(BeetActivity.this,44)*800f/Math.max(1,getWidth());
                float dx=Math.max(6f,(minWorld-imageDst.width())/2f);
                float dy=Math.max(6f,(minWorld-imageDst.height())/2f);
                if(x>=imageDst.left-dx && x<=imageDst.right+dx &&
                   y>=imageDst.top-dy && y<=imageDst.bottom+dy)return plant;
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
                long elapsed=SystemClock.elapsedRealtime()-holdStart;
                if(elapsed<5000L){
                    handler.postDelayed(pendingWater,5000L-elapsed);
                    return;
                }
                counted=true;
                selectedPlantId=expected.id;
                CareEngine.Result result=CareEngine.water(expected,clock.now());
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
                if(wateringMode){
                    startWater(plantAt(x,y));
                }else{
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
                if(action==MotionEvent.ACTION_UP){
                    if(dragging && moving!=null){
                        selectedPlantId=moving.id;
                        enqueueStateSave(moving);
                        updateStatus(moving);
                    }else if(!wateringMode && pressed!=null){
                        selectedPlantId=pressed.id;
                        updateStatus(pressed);
                    }
                }
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
