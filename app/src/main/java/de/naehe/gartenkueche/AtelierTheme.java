package de.naehe.gartenkueche;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import android.widget.Button;

/** Low-cost material-inspired drawables: warm walnut, brass and gentle amethyst. */
final class AtelierTheme {
    static final int WALNUT=0xFF241916, PANEL=0xFF39241F, GOLD=0xFFDAB27A;
    static final int MUTED=0xFFBB9F83, AMETHYST=0xFFAA78C9;
    static final int WHITE=0xFFF8EBDD, INSET=0xFF201B20;
    private AtelierTheme(){}
    static Drawable background(Context c){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF342019,0xFF1A1313,0xFF38241A});
        d.setCornerRadius(Ui.dp(c,12));return d;
    }
    static Drawable panel(Context c){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF4D3025,0xFF2D201D,0xFF3A251E});
        d.setCornerRadius(Ui.dp(c,15));
        d.setStroke(Ui.dp(c,1),0xFF9D6A40);return d;
    }
    static Drawable button(Context c,boolean active){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            active?new int[]{0xFF956839,0xFF654124}:new int[]{0xFF54372C,0xFF30201C});
        d.setCornerRadius(Ui.dp(c,11));
        d.setStroke(Ui.dp(c,active?2:1),active?0xFFFFCB76:0xFF926B4A);
        return d;
    }
    static Drawable mainButton(Context c){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFFAC7342,0xFF714324,0xFF462417});
        d.setCornerRadius(Ui.dp(c,14));
        d.setStroke(Ui.dp(c,2),0xFFE9B36B);
        return d;
    }
    static TextView text(Context c,String value,int size,boolean bold){
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);
        t.setTextColor(bold?WHITE:MUTED);
        if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);
        return t;
    }
    static Button button(Context c,String text,boolean active){
        Button b=new Button(c);
        b.setText(text);b.setAllCaps(false);b.setTextSize(13);
        b.setTextColor(active?0xFFFFF4DC:WHITE);
        b.setBackground(button(c,active));
        b.setMinHeight(Ui.dp(c,46));b.setPadding(Ui.dp(c,4),Ui.dp(c,8),Ui.dp(c,4),Ui.dp(c,8));
        return b;
    }
}
