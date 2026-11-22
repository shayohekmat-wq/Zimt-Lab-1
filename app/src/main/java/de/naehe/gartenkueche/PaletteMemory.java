package de.naehe.gartenkueche;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/** Most-used colors count completed strokes/fills, not swatch taps. */
final class PaletteMemory {
    private final SharedPreferences prefs;
    PaletteMemory(Context c){prefs=c.getApplicationContext().getSharedPreferences("atelier_palette",Context.MODE_PRIVATE);}
    void used(int argb){
        String key="n_"+Integer.toHexString(argb);
        prefs.edit().putInt(key,Math.min(1000000,prefs.getInt(key,0)+1))
             .putLong("r_"+Integer.toHexString(argb),System.currentTimeMillis()).apply();
    }
    int[] frequent(int limit){
        Map<String,?> all=prefs.getAll();
        ArrayList<String> keys=new ArrayList<>();
        for(String k:all.keySet())if(k.startsWith("n_"))keys.add(k);
        Collections.sort(keys,(a,b)->{
            int counts=prefs.getInt(b,0)-prefs.getInt(a,0);
            if(counts!=0)return counts;
            return Long.compare(prefs.getLong("r_"+b.substring(2),0),
                                prefs.getLong("r_"+a.substring(2),0));
        });
        int n=Math.min(limit,keys.size());
        int[] colors=new int[n];
        for(int i=0;i<n;i++)colors[i]=(int)Long.parseLong(keys.get(i).substring(2),16);
        return colors;
    }
}
