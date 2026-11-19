package de.naehe.gartenkueche;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BACKGROUND = Color.rgb(247, 241, 229);
    static final int INK = Color.rgb(59, 48, 40);
    static final int DEEP_PURPLE = Color.rgb(80, 38, 116);
    private Ui() {}
    static int dp(Context context, float value) {
        return Math.round(context.getResources().getDisplayMetrics().density * value);
    }
    static GradientDrawable shape(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        return bg;
    }
    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }
    static TextView text(Context c, String value, int size, boolean bold) {
        TextView tv = new TextView(c);
        tv.setText(value);
        tv.setTextSize(size);
        tv.setTextColor(INK);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }
    static Button button(Activity a, String caption) {
        Button b = new Button(a);
        b.setText(caption);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setBackground(shape(DEEP_PURPLE, dp(a, 12)));
        b.setPadding(dp(a, 16), dp(a, 10), dp(a, 16), dp(a, 10));
        return b;
    }
    static LinearLayout.LayoutParams spacing(Context c, int width, int height, int bottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        lp.bottomMargin = dp(c, bottomDp);
        return lp;
    }
    static void add(LinearLayout parent, View child, int bottomDp) {
        parent.addView(child, spacing(parent.getContext(),
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, bottomDp));
    }
}
