package de.naehe.gartenkueche;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class BeetActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout page = Ui.column(this);
        page.setPadding(Ui.dp(this, 18), Ui.dp(this, 28), Ui.dp(this, 18), Ui.dp(this, 22));
        scroll.addView(page);
        TextView title = Ui.text(this, "Beet", 30, true);
        Ui.add(page, title, 6);
        TextView description = Ui.text(this, "800 × 600 logische Pixel · Dein Platz für Pflanzen", 13, false);
        Ui.add(page, description, 18);
        GridPreview grid = new GridPreview();
        Ui.add(page, grid, 16);
        TextView empty = Ui.text(this, "Noch keine Pflanze im Beet.\nErstelle deine erste Pixelpflanze.", 15, false);
        Ui.add(page, empty, 20);
        android.widget.Button create = Ui.button(this, "Pflanze erstellen");
        create.setOnClickListener(v -> startActivity(new Intent(this, StageSetupActivity.class)));
        Ui.add(page, create, 10);
        TextView note = Ui.text(this,
                "Prototyp 0.0.1: Die dauerhafte Platzierung, Gießkanne und Pflege-Engine folgen in den nächsten Meilensteinen.",
                12, false);
        Ui.add(page, note, 12);
        setContentView(scroll);
    }

    private class GridPreview extends View {
        final Paint p = new Paint();
        GridPreview() { super(BeetActivity.this); }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int w = MeasureSpec.getSize(widthSpec);
            setMeasuredDimension(w, Math.round(w * 600f / 800f));
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float sx = getWidth() / 800f, sy = getHeight() / 600f;
            canvas.save(); canvas.scale(sx, sy);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(227, 213, 192));
            canvas.drawRect(0, 0, 800, 600, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1f);
            p.setColor(Color.argb(90, 102, 82, 64));
            for (int x = 0; x <= 800; x += 25) canvas.drawLine(x, 0, x, 600, p);
            for (int y = 0; y <= 600; y += 25) canvas.drawLine(0, y, 800, y, p);
            p.setColor(Color.rgb(101, 75, 55));
            p.setStrokeWidth(3f);
            canvas.drawRect(1.5f, 1.5f, 798.5f, 598.5f, p);
            canvas.restore();
        }
    }
}
