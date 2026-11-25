package de.naehe.gartenkueche;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Premium atelier entry. Recovery uses the same atomic AtelierStore as the editor. */
public class StageSetupActivity extends Activity {
    private AtelierStore store;
    private boolean hasDraft;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        store=new AtelierStore(this);
        getWindow().setStatusBarColor(0xFF1B1312);
        getWindow().setNavigationBarColor(0xFF1B1312);
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(AtelierTheme.WALNUT);
        LinearLayout page=Ui.column(this);
        page.setPadding(Ui.dp(this,20),Ui.dp(this,44),Ui.dp(this,20),Ui.dp(this,20));
        page.setBackground(AtelierTheme.background(this));
        scroll.addView(page);
        TextView head=AtelierTheme.text(this,"✦  Eine neue Pflanze  ✦",27,true);
        head.setTextColor(AtelierTheme.GOLD);head.setGravity(Gravity.CENTER);
        Ui.add(page,head,12);
        TextView subtitle=AtelierTheme.text(this,
            "Wie viele Evolutionsstufen soll sie bekommen?",16,false);
        subtitle.setGravity(Gravity.CENTER);Ui.add(page,subtitle,24);
        LinearLayout box=Ui.column(this);
        box.setPadding(Ui.dp(this,18),Ui.dp(this,21),Ui.dp(this,18),Ui.dp(this,16));
        box.setBackground(AtelierTheme.panel(this));
        TextView count=AtelierTheme.text(this,"3 Evolutionsstufen",24,true);
        count.setTextColor(AtelierTheme.GOLD);count.setGravity(Gravity.CENTER);
        Ui.add(box,count,8);
        SeekBar slider=new SeekBar(this);
        slider.setMax(9);slider.setProgress(2);
        slider.setProgressTintList(android.content.res.ColorStateList.valueOf(AtelierTheme.GOLD));
        slider.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFFFDCA7));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int v,boolean fromUser){
                count.setText((v+1)+(v==0?" Evolutionsstufe":" Evolutionsstufen"));
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        Ui.add(box,slider,4);
        Ui.add(box,AtelierTheme.text(this,"1                                                 10",11,false),0);
        Ui.add(page,box,22);

        TextView info=AtelierTheme.text(this,"Gespeichertes Atelier wird geprüft …",13,false);
        Ui.add(page,info,12);
        Button resume=AtelierTheme.button(this,"Gespeicherten Entwurf fortsetzen",true);
        resume.setVisibility(View.GONE);
        resume.setOnClickListener(v->openEditor(0,true));
        Ui.add(page,resume,11);
        Button create=AtelierTheme.button(this,"✎  Neue Pflanze zeichnen  →",true);
        create.setBackground(AtelierTheme.mainButton(this));
        create.setEnabled(false);
        create.setOnClickListener(v->{
            if(hasDraft){
                new AlertDialog.Builder(this).setTitle("Alten Entwurf ersetzen?")
                    .setMessage("Nur der unvollendete Atelier-Entwurf wird ersetzt. Fertige Pflanzen im Beet bleiben erhalten.")
                    .setNegativeButton("Abbrechen",null)
                    .setPositiveButton("Neue Pflanze beginnen",(dialog,which)->{
                        int stages = slider.getProgress() + 1;
                        create.setEnabled(false);
                        resume.setEnabled(false);
                        slider.setEnabled(false);
                        AppIo.execute(()->{
                            try {
                                // Commit the replacement before navigation so either draft can be resumed.
                                store.save(new AtelierDocument(stages));
                            } catch (java.io.IOException error) {
                                android.util.Log.e("GartenKueche", "Cannot replace atelier draft", error);
                                AppIo.onMain(() -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    info.setText("Der neue Entwurf konnte nicht gespeichert werden. Bitte erneut versuchen.");
                                    create.setEnabled(true);
                                    resume.setEnabled(true);
                                    slider.setEnabled(true);
                                });
                                return;
                            }
                            AppIo.onMain(()->{
                                if(!isFinishing()&&!isDestroyed())openEditor(stages,true);
                            });
                        });
                    }).show();
            }else openEditor(slider.getProgress()+1,false);
        });
        Ui.add(page,create,16);
        Ui.add(page,AtelierTheme.text(this,
            "Im Atelier kannst du Konturen und Farben später jederzeit korrigieren, zwischen Stufen wechseln und frühere Arbeiten wieder öffnen.",13,false),0);
        setContentView(scroll);

        AppIo.execute(()->{
            final AtelierDocument saved;
            try{
                saved=store.load();
            }catch(java.io.IOException unreadable){
                android.util.Log.e("GartenKueche","Cannot read existing atelier draft",unreadable);
                AppIo.onMain(()->{
                    if(isFinishing()||isDestroyed())return;
                    hasDraft=true; // A new drawing requires explicit replacement.
                    info.setText("Der vorhandene Entwurf konnte nicht gelesen werden. Du kannst die Wiederherstellung erneut versuchen.");
                    resume.setVisibility(View.VISIBLE);
                    create.setEnabled(true);
                });
                return;
            }
            AppIo.onMain(()->{
                if(isFinishing()||isDestroyed())return;
                hasDraft=saved!=null;
                info.setText(hasDraft
                    ?"Ein sicher gespeicherter Entwurf mit "+saved.count+" Stufen wartet auf dich."
                    :"Noch kein Entwurf vorhanden. Du kannst direkt beginnen.");
                resume.setVisibility(hasDraft?View.VISIBLE:View.GONE);
                create.setEnabled(true);
            });
        });
    }
    private void openEditor(int count,boolean resume){
        Intent intent=new Intent(this,AtelierEditorActivity.class);
        intent.putExtra("stage_count",count);
        intent.putExtra("resume_draft",resume);
        startActivity(intent);finish();
    }
}
