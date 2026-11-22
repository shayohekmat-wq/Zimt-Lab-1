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
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.Locale;

/**
 * v0.0.9 native pixel atelier. Contour, fill and historical edits stay separate.
 * Navigating between modes/stages never clears artwork or its sparse undo stacks.
 */
public final class AtelierEditorActivity extends Activity {
    private static final int[] COLORS={
        0xFFFFFFFF,0xFFDAD5C9,0xFFA6A6A6,0xFF555555,0xFF1D1D1D,
        0xFFFAE6A0,0xFFF7BC45,0xFFF28433,0xFFD83B37,0xFF9F1839,
        0xFFFFB6A4,0xFFF5799A,0xFFB852A5,0xFF8052BD,0xFF472476,
        0xFFA8E2D5,0xFF54BCB1,0xFF218A8C,0xFF145D67,0xFFD9DF71,
        0xFFA8CB4B,0xFF6FA548,0xFF38733C,0xFF1E4A34,0xFFA0BD8C,
        0xFFE8C4A3,0xFFC89062,0xFFA36540,0xFF753B25,0xFF452A25,
        0xFFFAE7D7,0xFFDDAA8F,0xFFAD7471,0xFF75474E,0xFF322932,
        0xFF61969B,0xFFD2B279,0xFFF4DCE4,0xFFBC7F56,0xFF82BD76
    };
    private static final String[] TOOLS={"✎  Stift","🖌 Pinsel","▱ Radierer","⊙ Pipette","▤ Eimer"};
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable autosave=this::saveDraft;
    private AtelierDocument doc;
    private AtelierStore draftStore;
    private PlantStore plants;
    private PaletteMemory paletteMemory;
    private AtelierHistory[] histories;
    private PixelView pixelView;
    private LinearLayout root,layout,previewPanel,swatchRow;
    private TextView status,tip,toolLabel,pageLabel;
    private Button[] tools=new Button[5];
    private int pageGeneration;
    private boolean busy,complete,showGrid=true;
    private PlantArt pendingCandidate;
    private int pendingHidden;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        draftStore=new AtelierStore(this);
        plants=new PlantStore(this);
        paletteMemory=new PaletteMemory(this);
        getWindow().setStatusBarColor(0xFF1B1312);
        getWindow().setNavigationBarColor(0xFF1B1312);
        if(getIntent().getBooleanExtra("resume_draft",false)){
            setContentView(AtelierTheme.text(this,"Atelier wird wiederhergestellt …",19,true));
            AppIo.execute(()->{
                AtelierDocument saved=draftStore.load();
                AppIo.onMain(()->{
                    if(isFinishing()||isDestroyed())return;
                    doc=saved!=null?saved:new AtelierDocument(1);
                    initHistories();rebuild();
                });
            });
        }else{
            doc=new AtelierDocument(getIntent().getIntExtra("stage_count",3));
            initHistories();rebuild();
        }
    }
    private void initHistories(){
        histories=new AtelierHistory[doc.count+1];
        for(int i=0;i<histories.length;i++)histories[i]=new AtelierHistory();
    }
    private AtelierHistory history(){return histories[doc.deathMode?doc.count:Math.min(doc.active,doc.count-1)];}
    private PlantArt art(){return doc.current();}
    private void notifyShort(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private void scheduleSave(){
        if(complete||doc==null)return;
        handler.removeCallbacks(autosave);
        if(status!=null)status.setText("◌  Ungesicherte Änderung");
        handler.postDelayed(autosave,950);
    }
    private void saveDraft(){
        if(complete||doc==null)return;
        final AtelierDocument snapshot=doc.copy();
        if(status!=null)status.setText("◷  Sichert …");
        AppIo.execute(()->{
            IOException failure=null;
            try{draftStore.save(snapshot);}catch(IOException e){failure=e;}
            final IOException error=failure;
            AppIo.onMain(()->{
                if(isFinishing()||isDestroyed()||complete)return;
                if(status!=null)status.setText(error!=null?"!  Sicherung fehlgeschlagen":
                    doc.revision==snapshot.revision?"✓  Gesichert":"◌  Neue Änderungen");
                if(error!=null)android.util.Log.e("GartenKueche","Atelier autosave failed",error);
            });
        });
    }
    @Override protected void onPause(){
        handler.removeCallbacks(autosave);
        saveDraft();
        super.onPause();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(autosave);
        super.onDestroy();
    }

    private void rebuild(){
        if(doc==null)return;
        pageGeneration++;
        pendingCandidate=null;
        pixelView=null;
        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setBackgroundColor(AtelierTheme.WALNUT);
        layout=Ui.column(this);
        layout.setPadding(Ui.dp(this,12),Ui.dp(this,17),Ui.dp(this,12),Ui.dp(this,26));
        layout.setBackground(AtelierTheme.background(this));
        scroller.addView(layout);
        renderHeader();
        if(doc.deathChoice())renderDeathChoice();
        else renderEditor();
        setContentView(scroller);
    }
    private void renderHeader(){
        LinearLayout header=Ui.row(this);
        Button back=AtelierTheme.button(this,"‹",false);
        back.setTextSize(29);
        header.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,49),Ui.dp(this,48)));
        back.setOnClickListener(v->navigateBack());
        LinearLayout mid=Ui.column(this);
        mid.setGravity(Gravity.CENTER);
        TextView title=AtelierTheme.text(this,"✦  Pflanzen-Atelier  ✦",23,true);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(AtelierTheme.GOLD);
        Ui.add(mid,title,1);
        TextView step=AtelierTheme.text(this,doc.active==doc.count?"Todesform"
            :"Evolution "+(doc.active+1)+" von "+doc.count,12,false);
        step.setGravity(Gravity.CENTER);Ui.add(mid,step,0);
        header.addView(mid,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));
        Button grid=AtelierTheme.button(this,"▦",false);
        grid.setContentDescription("Pixelraster ein oder aus");
        header.addView(grid,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        grid.setOnClickListener(v->{showGrid=!showGrid;if(pixelView!=null)pixelView.invalidate();});
        Ui.add(layout,header,5);
        status=AtelierTheme.text(this,"✓  Automatische Sicherung",11,false);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        Ui.add(layout,status,9);
    }
    private void renderDeathChoice(){
        TextView explanation=AtelierTheme.text(this,"Eine Pflanze braucht auch eine Todesform. Du kannst deine erste Stufe in Graustufen übernehmen oder selbst eine neue Form zeichnen.",14,false);
        Ui.add(layout,explanation,14);
        android.widget.ImageView preview=new android.widget.ImageView(this);
        preview.setImageBitmap(doc.stages[0].grayCopy().bitmap());
        preview.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        preview.setBackgroundColor(0xFF26222A);
        layout.addView(preview,Ui.spacing(this,-1,Ui.dp(this,280),12));
        Button ok=AtelierTheme.button(this,"Graustufen übernehmen  ✓",true);
        ok.setOnClickListener(v->finishPlant(doc.stages[0].grayCopy()));
        Ui.add(layout,ok,10);
        Button custom=AtelierTheme.button(this,"Eigene Todesform zeichnen  ✎",false);
        custom.setOnClickListener(v->{
            doc.deathMode=true;doc.colorMode=doc.death.hasInterior();
            doc.revision++;saveDraft();rebuild();
        });
        Ui.add(layout,custom,13);
        Button previous=AtelierTheme.button(this,"‹  Vorherige Evolutionsstufe bearbeiten",false);
        previous.setOnClickListener(v->navigateStage(doc.count-1,true));
        Ui.add(layout,previous,10);
    }
    private void renderEditor(){
        if(!doc.deathMode)renderStageTabs();
        LinearLayout mode=Ui.row(this);
        Button outline=AtelierTheme.button(this,"✎  Kontur",!doc.colorMode);
        Button color=AtelierTheme.button(this,"◉  Farbe",doc.colorMode);
        addFlex(mode,outline,5);addFlex(mode,color,0);
        outline.setOnClickListener(v->{if(doc.colorMode){doc.beginContourEdit();scheduleSave();rebuild();}});
        color.setOnClickListener(v->{if(!doc.colorMode)validateOutline();});
        Ui.add(layout,mode,8);

        tip=AtelierTheme.text(this,doc.colorMode
            ?art().detectedRegions()+" geschlossene Bereiche · Offene Stiele bleiben sichtbar · Farben bleiben gespeichert"
            :"Zeichne Konturen und offene Stiele. Wechsel jederzeit zu Farbe – bestehende Farben bleiben erhalten.",12,false);
        tip.setTextColor(AtelierTheme.MUTED);
        Ui.add(layout,tip,8);

        FrameLayout canvasFrame=new FrameLayout(this);
        canvasFrame.setPadding(Ui.dp(this,5),Ui.dp(this,5),Ui.dp(this,5),Ui.dp(this,5));
        canvasFrame.setBackground(AtelierTheme.panel(this));
        pixelView=new PixelView();
        canvasFrame.addView(pixelView,new FrameLayout.LayoutParams(-1,-2));
        Ui.add(layout,canvasFrame,8);

        // This area is intentionally immediately below the recomputed picture.
        previewPanel=Ui.column(this);
        previewPanel.setVisibility(View.GONE);
        Ui.add(layout,previewPanel,8);

        LinearLayout zoomRow=Ui.row(this);
        Button minus=AtelierTheme.button(this,"− Zoom",false);
        Button plus=AtelierTheme.button(this,"+ Zoom",false);
        Button undo=AtelierTheme.button(this,"↶",false);
        Button redo=AtelierTheme.button(this,"↷",false);
        addFlex(zoomRow,minus,4);addFlex(zoomRow,plus,4);
        zoomRow.addView(undo,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,46)));
        redo.setTextSize(20);
        zoomRow.addView(redo,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,46)));
        minus.setOnClickListener(v->pixelView.zoom(false));
        plus.setOnClickListener(v->pixelView.zoom(true));
        undo.setOnClickListener(v->historyAction(false));
        redo.setOnClickListener(v->historyAction(true));
        Ui.add(layout,zoomRow,9);

        LinearLayout toolbar=Ui.row(this);
        for(int i=0;i<5;i++){
            final int tool=i;
            tools[i]=AtelierTheme.button(this,TOOLS[i],doc.tool==i);
            tools[i].setTextSize(12);
            tools[i].setContentDescription(TOOLS[i]+(doc.tool==i?", ausgewählt":""));
            tools[i].setOnClickListener(v->{
                if(busy)return;
                if(tool==4&&!doc.colorMode){notifyShort("Eimer nur im Farbmodus");return;}
                doc.tool=tool;doc.revision++;
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                refreshToolbar();scheduleSave();
            });
            addFlex(toolbar,tools[i],i==4?0:3);
        }
        Ui.add(layout,toolbar,5);
        toolLabel=AtelierTheme.text(this,"",12,false);
        toolLabel.setTextColor(AtelierTheme.GOLD);
        toolLabel.setGravity(Gravity.CENTER);
        Ui.add(layout,toolLabel,10);
        refreshToolbar();

        renderPalette();
        if(!doc.deathMode)renderDifficulty();

        LinearLayout navigation=Ui.row(this);
        Button prev=AtelierTheme.button(this,"‹  Zurück",false);
        Button next=AtelierTheme.button(this,doc.colorMode?
            (doc.deathMode?"Pflanze abschließen  ✓":"Weiter  →"):"Zu Farbe  →",true);
        next.setBackground(AtelierTheme.mainButton(this));
        addFlex(navigation,prev,6);addFlex(navigation,next,0);
        prev.setOnClickListener(v->navigateBack());
        next.setOnClickListener(v->{
            if(busy)return;
            if(!doc.colorMode)validateOutline();
            else advanceStage();
        });
        Ui.add(layout,navigation,10);
        TextView footer=AtelierTheme.text(this,
            "Alle Konturen, offenen Linien und Farbpixel bleiben erhalten. Zwei Finger verschieben den Ausschnitt. Entwürfe werden lokal gesichert.",11,false);
        Ui.add(layout,footer,0);
    }
    private void renderStageTabs(){
        HorizontalScrollView sc=new HorizontalScrollView(this);
        sc.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=Ui.row(this);
        for(int i=0;i<doc.count;i++){
            final int position=i;
            Button chip=AtelierTheme.button(this,(doc.completed[i]?"✓ ":"")+(i+1),
                doc.active==i);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(Ui.dp(this,55),Ui.dp(this,44));
            lp.rightMargin=Ui.dp(this,5);
            chips.addView(chip,lp);
            chip.setContentDescription("Evolution "+(i+1)+(doc.completed[i]?", abgeschlossen":""));
            chip.setOnClickListener(v->navigateStage(position,doc.stages[position].hasInterior()));
        }
        sc.addView(chips);Ui.add(layout,sc,9);
    }
    private void addFlex(LinearLayout row,View view,int rightDp){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1);
        lp.rightMargin=Ui.dp(this,rightDp);
        row.addView(view,lp);
    }
    private void refreshToolbar(){
        for(int i=0;i<5;i++){
            if(tools[i]==null)continue;
            tools[i].setBackground(AtelierTheme.button(this,doc.tool==i));
            tools[i].setContentDescription(TOOLS[i]+(doc.tool==i?", ausgewählt":""));
        }
        if(toolLabel!=null){
            String tool=TOOLS[Math.max(0,Math.min(4,doc.tool))];
            toolLabel.setText("◆ "+tool+" · "+(doc.tool==1?"3 px":"1 px")+
                " · "+(doc.colorMode?"Farbe":"Kontur")+
                " · "+String.format(Locale.US,"#%06X",doc.selectedColor&0xffffff));
        }
    }
    private void renderPalette(){
        LinearLayout container=Ui.column(this);
        container.setBackground(AtelierTheme.panel(this));
        container.setPadding(Ui.dp(this,9),Ui.dp(this,8),Ui.dp(this,9),Ui.dp(this,9));
        LinearLayout controls=Ui.row(this);
        Button left=AtelierTheme.button(this,"‹",false);
        controls.addView(left,new LinearLayout.LayoutParams(Ui.dp(this,41),Ui.dp(this,42)));
        pageLabel=AtelierTheme.text(this,"",12,false);pageLabel.setGravity(Gravity.CENTER);
        controls.addView(pageLabel,new LinearLayout.LayoutParams(0,Ui.dp(this,42),1));
        Button right=AtelierTheme.button(this,"›",false);
        controls.addView(right,new LinearLayout.LayoutParams(Ui.dp(this,41),Ui.dp(this,42)));
        left.setOnClickListener(v->{doc.palettePage=(doc.palettePage+4)%5;refreshSwatches();scheduleSave();});
        right.setOnClickListener(v->{doc.palettePage=(doc.palettePage+1)%5;refreshSwatches();scheduleSave();});
        Ui.add(container,controls,5);
        swatchRow=Ui.row(this);
        Ui.add(container,swatchRow,5);
        Button wheel=AtelierTheme.button(this,"◉ Farbkreis  ·  eigene Farbe wählen",false);
        wheel.setOnClickListener(v->openWheel());
        Ui.add(container,wheel,0);
        Ui.add(layout,container,12);
        refreshSwatches();
    }
    private void refreshSwatches(){
        if(swatchRow==null)return;
        swatchRow.removeAllViews();
        int[] group;
        int page=doc.palettePage;
        if(page<4){
            group=new int[10];
            System.arraycopy(COLORS,page*10,group,0,10);
            pageLabel.setText("Farben  "+(page+1)+" / 4");
        }else{
            group=paletteMemory.frequent(10);
            pageLabel.setText("✦  Häufig verwendet");
        }
        if(group.length==0){
            TextView message=AtelierTheme.text(this,"Noch keine Farben gemalt",12,false);
            swatchRow.addView(message);
        }
        for(int rgba:group){
            View swatch=new View(this);
            android.graphics.drawable.GradientDrawable bg=Ui.shape(rgba,Ui.dp(this,7));
            bg.setStroke(Ui.dp(this,doc.selectedColor==rgba?3:1),
                doc.selectedColor==rgba?0xffffd585:0xff6e5344);
            swatch.setBackground(bg);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,37),1f);
            lp.setMargins(Ui.dp(this,1),0,Ui.dp(this,1),0);
            swatchRow.addView(swatch,lp);
            swatch.setContentDescription("Farbe "+String.format(Locale.US,"#%06X",rgba&0xffffff)
                +(doc.selectedColor==rgba?", ausgewählt":""));
            swatch.setOnClickListener(v->{
                doc.selectedColor=rgba;doc.revision++;
                refreshSwatches();refreshToolbar();scheduleSave();
            });
        }
    }
    private void openWheel(){
        HueWheelView wheel=new HueWheelView(this);
        wheel.setColor(doc.selectedColor);
        LinearLayout panel=Ui.column(this);
        panel.setPadding(Ui.dp(this,16),Ui.dp(this,10),Ui.dp(this,16),Ui.dp(this,12));
        panel.addView(wheel,new LinearLayout.LayoutParams(-1,Ui.dp(this,230)));
        TextView sample=Ui.text(this,"",16,true);
        Ui.add(panel,sample,7);
        SeekBar brightness=new SeekBar(this);
        float[] hsv=new float[3];
        Color.colorToHSV(doc.selectedColor,hsv);
        brightness.setMax(100);brightness.setProgress(Math.round(hsv[2]*100));
        Ui.add(panel,Ui.text(this,"Helligkeit",13,false),3);
        Ui.add(panel,brightness,4);
        wheel.setListener(rgba->{
            sample.setText(String.format(Locale.US,"Farbe: #%06X",rgba&0xffffff));
            sample.setTextColor(rgba);
        });
        wheel.setBrightness(Math.max(.05f,hsv[2]));
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean fromUser){
                wheel.setBrightness(Math.max(.05f,progress/100f));
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        new AlertDialog.Builder(this).setTitle("Dein Farbkreis")
            .setView(panel)
            .setNegativeButton("Abbrechen",null)
            .setPositiveButton("Farbe auswählen",(dialog,which)->{
                doc.selectedColor=wheel.color();doc.revision++;
                refreshSwatches();refreshToolbar();scheduleSave();
            }).show();
    }
    private void renderDifficulty(){
        LinearLayout panel=Ui.column(this);
        panel.setBackground(AtelierTheme.panel(this));
        panel.setPadding(Ui.dp(this,9),Ui.dp(this,10),Ui.dp(this,9),Ui.dp(this,10));
        Ui.add(panel,AtelierTheme.text(this,"✦  Schwierigkeit dieser Evolutionsstufe",14,true),7);
        LinearLayout row=Ui.row(this);
        for(int i=0;i<4;i++){
            final int difficulty=i;
            Button b=AtelierTheme.button(this,PlantStore.DIFFICULTIES[i],
                doc.difficulty[doc.active]==i);
            b.setTextSize(11);
            addFlex(row,b,i==3?0:4);
            b.setOnClickListener(v->{
                doc.difficulty[doc.active]=difficulty;doc.revision++;
                for(int j=0;j<4;j++)row.getChildAt(j).setBackground(
                    AtelierTheme.button(this,j==difficulty));
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                scheduleSave();
            });
        }
        Ui.add(panel,row,0);
        Ui.add(layout,panel,12);
    }

    private void historyAction(boolean redo){
        if(busy)return;
        boolean changed=redo?history().redo(art()):history().undo(art());
        if(!changed){notifyShort(redo?"Nichts zu wiederholen":"Nichts rückgängig");return;}
        // If we undid a topology validation, update the accepted baseline as well.
        if(doc.colorMode && art().hasInterior()){
            if(doc.deathMode)doc.acceptedDeath=art().copy();
            else doc.accepted[doc.active]=art().copy();
        }
        doc.revision++;
        if(pixelView!=null)pixelView.dirty();
        scheduleSave();
    }
    private void navigateBack(){
        if(busy){notifyShort("Bitte zuerst die Konturentscheidung abschließen");return;}
        if(doc.deathChoice()){
            navigateStage(doc.count-1,true);
        }else if(doc.colorMode){
            doc.beginContourEdit();scheduleSave();rebuild();
        }else if(doc.deathMode){
            doc.deathMode=false;doc.active=doc.count;doc.revision++;scheduleSave();rebuild();
        }else if(doc.active>0){
            navigateStage(doc.active-1,doc.stages[doc.active-1].hasInterior());
        }else{
            saveDraft();finish();
        }
    }
    @Override public void onBackPressed(){navigateBack();}
    private void navigateStage(int i,boolean goColor){
        if(busy)return;
        if(i<0||i>=doc.count)return;
        doc.active=i;doc.deathMode=false;
        doc.colorMode=goColor;
        doc.revision++;scheduleSave();rebuild();
    }
    private void validateOutline(){
        if(busy)return;
        busy=true;
        final int generation=pageGeneration;
        final PlantArt candidate=art().copy();
        if(tip!=null)tip.setText("Konturen und Innenflächen werden geprüft …");
        if(pixelView!=null)pixelView.setEnabled(false);
        AppIo.execute(()->{
            boolean ok=candidate.validate();
            AppIo.onMain(()->{
                if(isFinishing()||isDestroyed()||generation!=pageGeneration)return;
                if(!ok){
                    busy=false;
                    if(pixelView!=null)pixelView.setEnabled(true);
                    if(tip!=null)tip.setText("Mindestens eine geschlossene Fläche benötigt. Offene Stiele bleiben erhalten.");
                    notifyShort("Noch keine ausmalbare Innenfläche erkannt");
                    return;
                }
                int hidden=doc.threatenedColors(candidate);
                if(hidden>0){
                    pendingCandidate=candidate;pendingHidden=hidden;
                    showTopologyPreview();
                    return;
                }
                commitContour(candidate);
            });
        });
    }
    private void showTopologyPreview(){
        if(pixelView!=null)pixelView.preview(pendingCandidate);
        previewPanel.removeAllViews();
        previewPanel.setVisibility(View.VISIBLE);
        previewPanel.setPadding(Ui.dp(this,12),Ui.dp(this,11),Ui.dp(this,12),Ui.dp(this,11));
        previewPanel.setBackground(AtelierTheme.panel(this));
        Ui.add(previewPanel,AtelierTheme.text(this,"Neue Kontur behalten?",17,true),4);
        Ui.add(previewPanel,AtelierTheme.text(this,
            pendingHidden+" bemalte Pixel wären vorübergehend verdeckt. Sie bleiben gespeichert und können wieder sichtbar werden.",13,false),7);
        LinearLayout buttons=Ui.row(this);
        Button keep=AtelierTheme.button(this,"Behalten & weiter",true);
        Button revert=AtelierTheme.button(this,"Alte Kontur",false);
        addFlex(buttons,keep,5);addFlex(buttons,revert,0);
        keep.setOnClickListener(v->{
            if(pendingCandidate!=null)commitContour(pendingCandidate);
        });
        revert.setOnClickListener(v->{
            history().begin(art());
            doc.restoreAcceptedOutline();
            history().record(art());
            doc.colorMode=true;doc.revision++;
            busy=false;pendingCandidate=null;
            scheduleSave();rebuild();
        });
        Ui.add(previewPanel,buttons,5);
        Button difference=AtelierTheme.button(this,"Unterschied anzeigen / ausblenden",false);
        difference.setOnClickListener(v->{if(pixelView!=null)pixelView.toggleDifference();});
        Ui.add(previewPanel,difference,0);
    }
    private void commitContour(PlantArt candidate){
        history().begin(art());
        doc.acceptCurrent(candidate);
        history().record(art());
        doc.colorMode=true;
        doc.revision++;
        busy=false;pendingCandidate=null;
        scheduleSave();rebuild();
    }
    private void advanceStage(){
        if(busy)return;
        if(doc.deathMode){
            if(!art().hasInterior()){notifyShort("Todesform benötigt eine geschlossene Fläche");return;}
            finishPlant(art().copy());
            return;
        }
        if(!art().hasInterior()){
            validateOutline();return;
        }
        doc.completed[doc.active]=true;
        if(doc.active+1<doc.count){
            doc.active++;doc.colorMode=doc.stages[doc.active].hasInterior();
        }else{
            doc.active=doc.count;
            doc.deathMode=false;
        }
        doc.revision++;scheduleSave();rebuild();
    }
    private void finishPlant(PlantArt deathArt){
        if(busy||complete)return;
        for(int i=0;i<doc.count;i++)if(!doc.completed[i]||!doc.stages[i].hasInterior()){
            notifyShort("Bitte zuerst Evolution "+(i+1)+" abschließen");
            navigateStage(i,doc.stages[i].hasInterior());
            return;
        }
        busy=true;
        PlantStore.Plant p=new PlantStore.Plant();
        for(int i=0;i<doc.count;i++){
            p.stages.add(doc.stages[i].copy());
            p.difficulties.add(doc.difficulty[i]);
        }
        p.death=deathArt.copy();
        if(status!=null)status.setText("◷  Pflanze wird gesichert …");
        AppIo.execute(()->{
            IOException failure=null;
            try{
                plants.savePlant(p);
                draftStore.clear();
            }catch(IOException e){failure=e;}
            final IOException error=failure;
            AppIo.onMain(()->{
                if(isFinishing()||isDestroyed())return;
                busy=false;
                if(error!=null){
                    new AlertDialog.Builder(this).setTitle("Speichern fehlgeschlagen")
                        .setMessage(error.getMessage()).setPositiveButton("OK",null).show();
                    return;
                }
                complete=true;handler.removeCallbacks(autosave);
                notifyShort("Pflanze im Beet gespeichert");
                Intent intent=new Intent(this,BeetActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);finish();
            });
        });
    }

    private final class PixelView extends View {
        private final Paint bitmapPaint=new Paint(), overlayPaint=new Paint(),gridPaint=new Paint(),highlightPaint=new Paint();
        private final Rect src=new Rect();
        private final RectF target=new RectF();
        private final int[] scratch=new int[PlantArt.TOTAL];
        private Bitmap buffer,previous;
        private PlantArt previewArt;
        private boolean dirty=true,showDifference,panning;
        private int zoom=1,ox,oy,oldX=-1,oldY=-1;
        private float centerX,centerY,oldDistance,tipX=-1,tipY=-1;
        private boolean gestureBlocked,strokeChanged;
        PixelView(){
            super(AtelierEditorActivity.this);
            bitmapPaint.setFilterBitmap(false);
            overlayPaint.setFilterBitmap(false);
            overlayPaint.setAlpha(51);
            gridPaint.setColor(0x407E7180);
            highlightPaint.setStyle(Paint.Style.STROKE);
            highlightPaint.setStrokeWidth(Ui.dp(getContext(),1.5f));
            buffer=Bitmap.createBitmap(PlantArt.SIZE,PlantArt.SIZE,Bitmap.Config.ARGB_8888);
            if(!doc.deathMode && doc.active>0)previous=doc.stages[doc.active-1].bitmap();
            zoom=Math.max(1,doc.zoom);ox=Math.max(0,doc.panX);oy=Math.max(0,doc.panY);
        }
        @Override protected void onMeasure(int w,int h){
            int width=MeasureSpec.getSize(w);setMeasuredDimension(width,width);
        }
        void dirty(){dirty=true;postInvalidateOnAnimation();}
        void preview(PlantArt p){previewArt=p;dirty(); }
        void toggleDifference(){showDifference=!showDifference;invalidate();}
        void zoom(boolean more){
            int[] options={1,2,3,5,10};int i=0;
            for(int k=0;k<options.length;k++)if(options[k]<=zoom)i=k;
            int next=options[Math.max(0,Math.min(options.length-1,i+(more?1:-1)))];
            int before=PlantArt.SIZE/zoom,after=PlantArt.SIZE/next;
            ox=Math.max(0,Math.min(PlantArt.SIZE-after,ox+(before-after)/2));
            oy=Math.max(0,Math.min(PlantArt.SIZE-after,oy+(before-after)/2));
            zoom=next;doc.zoom=zoom;doc.panX=ox;doc.panY=oy;doc.revision++;invalidate();scheduleSave();
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float side=Math.min(getWidth(),getHeight());
            int visible=PlantArt.SIZE/zoom;
            ox=Math.max(0,Math.min(PlantArt.SIZE-visible,ox));
            oy=Math.max(0,Math.min(PlantArt.SIZE-visible,oy));
            src.set(ox,oy,ox+visible,oy+visible);
            target.set(0,0,side,side);
            canvas.drawColor(AtelierTheme.INSET);
            if(previous!=null)canvas.drawBitmap(previous,src,target,overlayPaint);
            if(dirty){
                (previewArt!=null?previewArt:art()).renderInto(buffer,scratch);
                dirty=false;
            }
            canvas.drawBitmap(buffer,src,target,bitmapPaint);
            int step=zoom>=5?1:zoom>=2?5:10;
            if(showGrid) {
                for(int i=ox;i<=ox+visible;i++)if(i%step==0){
                    float x=(i-ox)*side/visible;
                    canvas.drawLine(x,0,x,side,gridPaint);
                }
                for(int j=oy;j<=oy+visible;j++)if(j%step==0){
                    float y=(j-oy)*side/visible;
                    canvas.drawLine(0,y,side,y,gridPaint);
                }
            }
            if(showDifference && previewArt!=null && doc.acceptedCurrent()!=null){
                PlantArt before=doc.acceptedCurrent();
                highlightPaint.setColor(0xFFFFC05A);
                float pitch=side/visible;
                for(int y=oy;y<oy+visible;y++)for(int x=ox;x<ox+visible;x++){
                    int idx=y*PlantArt.SIZE+x;
                    if(art().pigments[idx]!=0 && before.inside!=null && before.inside[idx]
                        && (previewArt.inside==null || !previewArt.inside[idx])){
                        float px=(x-ox)*pitch,py=(y-oy)*pitch;
                        canvas.drawRect(px,py,px+pitch,py+pitch,highlightPaint);
                    }
                }
            }
            if(tipX>=0 && tipY>=0){
                highlightPaint.setColor(0xFFFFCF88);
                float cx=tipX+Ui.dp(getContext(),12),cy=tipY-Ui.dp(getContext(),26);
                canvas.drawCircle(cx,cy,Ui.dp(getContext(),6),highlightPaint);
                canvas.drawLine(cx-10,cy,cx+10,cy,highlightPaint);
                canvas.drawLine(cx,cy-10,cx,cy+10,highlightPaint);
            }
        }
        private int px(float x){return Math.max(0,Math.min(299,ox+(int)(x*(300/zoom)/Math.max(1,getWidth()))));}
        private int py(float y){return Math.max(0,Math.min(299,oy+(int)(y*(300/zoom)/Math.max(1,getHeight()))));}
        private float separation(MotionEvent e){
            return (float)Math.hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));
        }
        private void stamp(int x,int y){
            PlantArt current=art();
            int radius=doc.tool==1?1:0;
            for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++){
                int xx=x+dx,yy=y+dy;
                if(xx<0||yy<0||xx>=300||yy>=300)continue;
                if(doc.colorMode){
                    if(doc.tool==2)current.colorPixel(xx,yy,Color.TRANSPARENT);
                    else current.colorPixel(xx,yy,doc.selectedColor);
                }else{
                    if(doc.tool==2)current.eraseContour(xx,yy);
                    else current.drawContour(xx,yy,doc.selectedColor);
                }
                buffer.setPixel(xx,yy,current.renderedPixel(xx,yy));
            }
            strokeChanged=true;
        }
        private void drawLine(int x0,int y0,int x1,int y1){
            int dx=Math.abs(x1-x0),dy=Math.abs(y1-y0);
            int sx=x0<x1?1:-1,sy=y0<y1?1:-1,err=dx-dy;
            while(true){
                stamp(x0,y0);
                if(x0==x1&&y0==y1)break;
                int twice=2*err;
                if(twice>-dy){err-=dy;x0+=sx;}
                if(twice<dx){err+=dx;y0+=sy;}
            }
        }
        private void moveTo(float x,float y){
            int nx=px(x),ny=py(y);
            if(oldX>=0)drawLine(oldX,oldY,nx,ny);else stamp(nx,ny);
            oldX=nx;oldY=ny;tipX=x;tipY=y;
            postInvalidateOnAnimation();
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(!isEnabled()||busy)return true;
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                tipX=e.getX();tipY=e.getY();
                if(doc.tool==3){
                    int picked=art().eyedropper(px(e.getX()),py(e.getY()));
                    if(picked!=0){
                        doc.selectedColor=picked;doc.revision++;
                        refreshToolbar();refreshSwatches();scheduleSave();
                    }else notifyShort("Hier ist keine Farbe");
                    invalidate();return true;
                }
                if(doc.tool==4){
                    history().begin(art());
                    art().fillRegion(px(e.getX()),py(e.getY()),doc.selectedColor);
                    history().record(art());
                    paletteMemory.used(doc.selectedColor);
                    doc.revision++;dirty();scheduleSave();
                    return true;
                }
                history().begin(art());strokeChanged=false;
                oldX=-1;oldY=-1;
                moveTo(e.getX(),e.getY());return true;
            }
            if(action==MotionEvent.ACTION_POINTER_DOWN && e.getPointerCount()>=2){
                // First gesture ends as a valid undoable stroke before panning.
                if(oldX>=0){history().record(art());if(strokeChanged){
                    if(doc.tool!=2)paletteMemory.used(doc.selectedColor);
                    doc.revision++;scheduleSave();
                }}
                oldX=-1;oldY=-1;
                panning=true;gestureBlocked=true;
                centerX=(e.getX(0)+e.getX(1))/2;
                centerY=(e.getY(0)+e.getY(1))/2;
                oldDistance=separation(e);
                return true;
            }
            if(action==MotionEvent.ACTION_MOVE && panning && e.getPointerCount()>=2){
                float nx=(e.getX(0)+e.getX(1))/2,ny=(e.getY(0)+e.getY(1))/2;
                int span=300/zoom;
                ox=Math.max(0,Math.min(300-span,Math.round(ox-(nx-centerX)*span/getWidth())));
                oy=Math.max(0,Math.min(300-span,Math.round(oy-(ny-centerY)*span/getHeight())));
                float dist=separation(e);
                if(oldDistance>0&&dist/oldDistance>1.3f){zoom(true);oldDistance=dist;}
                else if(dist>0&&oldDistance/dist>1.3f){zoom(false);oldDistance=dist;}
                centerX=nx;centerY=ny;doc.panX=ox;doc.panY=oy;invalidate();return true;
            }
            if(action==MotionEvent.ACTION_POINTER_UP){
                gestureBlocked=true;oldX=-1;oldY=-1;return true;
            }
            if(action==MotionEvent.ACTION_MOVE && !gestureBlocked&&!panning&&oldX>=0){
                for(int k=0;k<e.getHistorySize();k++)moveTo(e.getHistoricalX(k),e.getHistoricalY(k));
                moveTo(e.getX(),e.getY());
                return true;
            }
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                if(oldX>=0){
                    history().record(art());
                    if(strokeChanged){
                        if(doc.tool!=2)paletteMemory.used(doc.selectedColor);
                        doc.revision++;scheduleSave();
                    }
                }
                oldX=-1;oldY=-1;tipX=-1;tipY=-1;panning=false;gestureBlocked=false;
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                invalidate();return true;
            }
            return true;
        }
        @Override protected void onDetachedFromWindow(){
            if(buffer!=null){buffer.recycle();buffer=null;}
            if(previous!=null){previous.recycle();previous=null;}
            super.onDetachedFromWindow();
        }
    }
}
