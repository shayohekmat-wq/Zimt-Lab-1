package de.naehe.gartenkueche;

/** Shared pure-Java geometry for exact 1:1 pixel artwork, independent of Android UI. */
public final class SpriteGeometry {
    private SpriteGeometry(){}
    public static final class Box {
        public final int left, top, right, bottom;
        public Box(int l,int t,int r,int b){left=l;top=t;right=r;bottom=b;}
        public int width(){return right-left;}
        public int height(){return bottom-top;}
    }
    public static Box bounds(boolean[] outline,int[] rgba,int width,int height){
        if(width<1 || height<1 || outline.length!=width*height || rgba.length!=width*height)
            throw new IllegalArgumentException("Invalid raster geometry");
        int l=width,t=height,r=-1,b=-1;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int i=y*width+x;
            if(outline[i] || (rgba[i]>>>24)!=0){
                if(x<l)l=x;
                if(y<t)t=y;
                if(x>r)r=x;
                if(y>b)b=y;
            }
        }
        return r<l?new Box(0,0,0,0):new Box(l,t,r+1,b+1);
    }
    /** Crop offset in an original 300x300 stage, relative to a stable bottom-centre anchor. */
    public static int worldLeft(int plantX,int cropLeft,int canvasSize){
        return plantX-canvasSize/2+cropLeft;
    }
    public static int worldTop(int plantY,int cropTop,int canvasSize){
        return plantY-canvasSize+cropTop;
    }
}
