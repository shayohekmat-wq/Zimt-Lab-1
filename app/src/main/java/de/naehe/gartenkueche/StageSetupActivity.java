package de.naehe.gartenkueche;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;

/** Stage selector; the draft is inspected and cleared only on the I/O worker. */
public class StageSetupActivity extends Activity {
    private PlantStore store;
    private boolean hasDraft;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store=new PlantStore(getApplicationContext());
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout page=Ui.column(this);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(Ui.dp(this,25),Ui.dp(this,38),Ui.dp(this,25),Ui.dp(this,30));
        scroll.addView(page);
        Ui.add(page,Ui.text(this,"Neue Pflanze",29,true),15);
        Ui.add(page,Ui.text(this,"Wie viele Evolutionsstufen soll deine Pflanze besitzen?",17,false),16);
        NumberPicker stages=new NumberPicker(this);
        stages.setMinValue(1);stages.setMaxValue(10);stages.setValue(3);
        stages.setWrapSelectorWheel(false);
        Ui.add(page,stages,20);

        TextView draftInfo=Ui.text(this,"Gespeicherter Entwurf wird geprüft …",13,false);
        Ui.add(page,draftInfo,12);
        Button resume=Ui.button(this,"Gespeicherten Entwurf fortsetzen");
        resume.setVisibility(View.GONE);
        resume.setOnClickListener(v->openEditor(0,true));
        Ui.add(page,resume,15);
        Button create=Ui.button(this,"Neue Pflanze zeichnen");
        create.setEnabled(false);
        create.setOnClickListener(v->{
            if(hasDraft){
                new AlertDialog.Builder(this)
                    .setTitle("Bisherigen Entwurf ersetzen?")
                    .setMessage("Der bisherige unvollendete Entwurf wird gelöscht. Fertige Pflanzen bleiben erhalten.")
                    .setNegativeButton("Abbrechen",null)
                    .setPositiveButton("Neue Pflanze beginnen",(dialog,which)->{
                        create.setEnabled(false);
                        AppIo.execute(()->{
                            store.clearDraft();
                            AppIo.onMain(()->{
                                if(!isFinishing() && !isDestroyed())openEditor(stages.getValue(),false);
                            });
                        });
                    }).show();
            }else openEditor(stages.getValue(),false);
        });
        Ui.add(page,create,13);
        Ui.add(page,Ui.text(this,
            "Die vollständige Zeichnung wird automatisch gespeichert. Beim Weitermalen bleiben Konturen und Farben erhalten.",
            13,false),0);
        setContentView(scroll);

        AppIo.execute(()->{
            PlantStore.Draft saved=store.loadDraft();
            AppIo.onMain(()->{
                if(isFinishing() || isDestroyed())return;
                hasDraft=saved!=null;
                draftInfo.setText(hasDraft
                    ? "Unvollendeter Entwurf mit "+saved.requestedCount+" geplanten Evolutionsstufen vorhanden."
                    : "Noch kein unvollendeter Entwurf vorhanden.");
                resume.setVisibility(hasDraft?View.VISIBLE:View.GONE);
                create.setEnabled(true);
            });
        });
    }
    private void openEditor(int stageCount,boolean resume){
        Intent intent=new Intent(this,EditorActivity.class);
        intent.putExtra("stage_count",stageCount);
        intent.putExtra("resume_draft",resume);
        startActivity(intent);finish();
    }
}
