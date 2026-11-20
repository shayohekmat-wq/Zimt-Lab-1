package android.graphics;

/** Minimal JVM-only Bitmap stand-in for testing PlantArt serialization/cropping. */
public final class Bitmap {
    public enum Config { ARGB_8888 }
    private final int width, height;
    private final int[] argb;
    private Bitmap(int w,int h){width=w;height=h;argb=new int[w*h];}
    public static Bitmap createBitmap(int w,int h,Config config){
        if(w<1||h<1)throw new IllegalArgumentException("Invalid dimensions");
        return new Bitmap(w,h);
    }
    public int getWidth(){return width;}
    public int getHeight(){return height;}
    public int getPixel(int x,int y){return argb[y*width+x];}
    public void setPixels(int[] input,int offset,int stride,int x,int y,int w,int h){
        for(int row=0;row<h;row++)
            System.arraycopy(input,offset+row*stride,argb,(y+row)*width+x,w);
    }
}
