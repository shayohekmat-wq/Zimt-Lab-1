package de.naehe.gartenkueche;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;

public class StageSetupActivity extends Activity {
    private PlantStore store;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new PlantStore(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout page = Ui.column(this);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(Ui.dp(this, 25), Ui.dp(this, 38), Ui.dp(this, 25), Ui.dp(this, 30));
        scroll.addView(page);
        Ui.add(page, Ui.text(this, "Neue Pflanze", 29, true), 15);
        Ui.add(page, Ui.text(this, "Wie viele Evolutionsstufen soll deine Pflanze besitzen?", 17, false), 16);
        NumberPicker stages = new NumberPicker(this);
        stages.setMinValue(1);
        stages.setMaxValue(10);
        stages.setValue(3);
        stages.setWrapSelectorWheel(false);
        Ui.add(page, stages, 20);
        PlantStore.Draft draft = store.loadDraft();
        if (draft != null) {
            Ui.add(page, Ui.text(this, "Ein unvollendeter Entwurf mit " + draft.requestedCount
                + " geplanten Stufen ist gespeichert.", 14, false), 10);
            Button continueButton = Ui.button(this, "Gespeicherten Entwurf fortsetzen");
            continueButton.setOnClickListener(v -> openEditor(0, true));
            Ui.add(page, continueButton, 18);
        }
        Button start = Ui.button(this, "Neue Pflanze zeichnen");
        start.setOnClickListener(v -> {
            if (store.loadDraft() != null) {
                new AlertDialog.Builder(this)
                    .setTitle("Bisherigen Entwurf ersetzen?")
                    .setMessage("Der gespeicherte unvollendete Entwurf wird gelöscht. Bereits fertiggestellte Pflanzen bleiben erhalten.")
                    .setNegativeButton("Abbrechen", null)
                    .setPositiveButton("Neue Pflanze beginnen", (dlg,which) -> {
                        store.clearDraft();
                        openEditor(stages.getValue(), false);
                    }).show();
            } else openEditor(stages.getValue(), false);
        });
        Ui.add(page, start, 14);
        Ui.add(page, Ui.text(this,
            "Jede Stufe wird mit geschlossener Kontur, 40 Farben und eigenem Schwierigkeitsgrad gestaltet.",
            13, false), 0);
        setContentView(scroll);
    }
    private void openEditor(int stageCount, boolean resume) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("stage_count", stageCount);
        intent.putExtra("resume_draft", resume);
        startActivity(intent);
        finish();
    }
}
