package de.naehe.gartenkueche;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout page = Ui.column(this);
        page.setPadding(Ui.dp(this, 22), Ui.dp(this, 42), Ui.dp(this, 22), Ui.dp(this, 24));
        scroll.addView(page);
        TextView title = Ui.text(this, "Garten & Küche", 30, true);
        Ui.add(page, title, 8);
        TextView subtitle = Ui.text(this, "Deine kleinen Welten – mit Liebe gemacht.", 14, false);
        subtitle.setTextColor(Color.rgb(114, 100, 90));
        Ui.add(page, subtitle, 30);

        LinearLayout top = Ui.row(this);
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 205));
        topLp.bottomMargin = Ui.dp(this, 13);
        page.addView(top, topLp);

        View beet = tile("BEET", "Pixelpflanzen erschaffen und pflegen",
                Color.rgb(197, 216, 179), true);
        LinearLayout.LayoutParams beetLp = new LinearLayout.LayoutParams(0, -1, 1.7f);
        beetLp.rightMargin = Ui.dp(this, 13);
        top.addView(beet, beetLp);
        beet.setOnClickListener(v -> startActivity(new Intent(this, BeetActivity.class)));

        View kitchen = tile("KÜCHE", "In Vorbereitung",
                Color.rgb(233, 217, 193), false);
        top.addView(kitchen, new LinearLayout.LayoutParams(0, -1, 1f));
        kitchen.setOnClickListener(v -> Toast.makeText(this, "Küche kommt später.", Toast.LENGTH_SHORT).show());

        View garden = tile("GARTEN", "In Vorbereitung",
                Color.rgb(216, 221, 206), false);
        page.addView(garden, new LinearLayout.LayoutParams(-1, Ui.dp(this, 130)));
        garden.setOnClickListener(v -> Toast.makeText(this, "Garten kommt später.", Toast.LENGTH_SHORT).show());

        TextView small = Ui.text(this, "ZIMT LAB 1   ·   Labor-Build 0.0.8   ·   Offline", 11, false);
        small.setTextColor(Color.rgb(110, 100, 90));
        small.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = Ui.spacing(this, -1, -2, 0);
        footerLp.topMargin = Ui.dp(this, 30);
        page.addView(small, footerLp);
        setContentView(scroll);
    }
    private View tile(String name, String description, int background, boolean active) {
        LinearLayout panel = Ui.column(this);
        panel.setGravity(Gravity.BOTTOM);
        panel.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));
        panel.setBackground(Ui.shape(background, Ui.dp(this, 15)));
        TextView label = Ui.text(this, name, 22, true);
        panel.addView(label);
        TextView detail = Ui.text(this, description, 13, false);
        detail.setTextColor(Color.rgb(88, 80, 71));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.dp(this, 6);
        panel.addView(detail, lp);
        panel.setContentDescription(name + ": " + description);
        return panel;
    }
}
