package de.naehe.gartenkueche;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;

public class StageSetupActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
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
        android.widget.Button start = Ui.button(this, "Zur ersten Kontur");
        start.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("stage_count", stages.getValue());
            startActivity(intent);
        });
        Ui.add(page, start, 14);
        Ui.add(page, Ui.text(this,
                "Die vollständige Evolutionsverwaltung und der Todeszustand folgen nach dem Editor-Kern.",
                12, false), 0);
        setContentView(scroll);
    }
}
