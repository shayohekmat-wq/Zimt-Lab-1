package android.graphics;

/** Minimal JVM-only Android Color stand-in; NOT included in the APK. */
public final class Color {
    private Color(){}
    public static int rgb(int r,int g,int b){return argb(255,r,g,b);}
    public static int argb(int a,int r,int g,int b){
        return ((a&255)<<24)|((r&255)<<16)|((g&255)<<8)|(b&255);
    }
    public static int alpha(int v){return (v>>>24)&255;}
    public static int red(int v){return (v>>>16)&255;}
    public static int green(int v){return (v>>>8)&255;}
    public static int blue(int v){return v&255;}
}
