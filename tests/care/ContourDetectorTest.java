package de.naehe.gartenkueche;

/** JVM regression tests matching the user's three failed hand-drawn examples. */
public final class ContourDetectorTest {
    static final int S=300;
    static final int N=S*S;
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    static void pixel(boolean[] d,int x,int y){ if(x>=0&&x<S&&y>=0&&y<S) d[y*S+x]=true; }
    static void line(boolean[] d,int x0,int y0,int x1,int y1){
        int dx=Math.abs(x1-x0),dy=Math.abs(y1-y0);
        int sx=x0<x1?1:-1,sy=y0<y1?1:-1,err=dx-dy;
        while(true){
            pixel(d,x0,y0);
            if(x0==x1&&y0==y1)break;
            int e=err*2;
            if(e>-dy){err-=dy;x0+=sx;}
            if(e<dx){err+=dx;y0+=sy;}
        }
    }
    static boolean[] blank(){return new boolean[N];}
    static void loop(boolean[] d,int x,int y,int width,int height){
        // Deliberately separate gestures, not a single uninterrupted drawing.
        line(d,x,y,x+width,y);
        line(d,x+width,y,x+width,y+height);
        line(d,x+width,y+height,x,y+height);
        line(d,x,y+height,x,y);
    }
    static ContourDetector.Result valid(boolean[] d,String description){
        ContourDetector.Result r=ContourDetector.detect(d,S);
        check(r!=null && r.area>=12,description+" should have accepted a closed interior");
        return r;
    }
    public static void main(String[] args){
        boolean[] a=blank(); loop(a,60,60,90,100);
        valid(a,"four separate contour gestures");

        // Example 1: recognizable plant bulb with a long overhanging tail.
        a=blank();
        line(a,80,178,92,123);line(a,92,123,139,90);
        line(a,139,90,168,89);line(a,168,89,192,135);
        line(a,192,135,193,177);line(a,193,177,144,205);
        line(a,144,205,80,178);
        line(a,168,89,177,55); // dangling line must NOT invalidate
        ContourDetector.Result r=valid(a,"branching plant contour");
        check(!r.outline[55*S+177],"stray outer branch must be removed");
        check(r.interior[150*S+143],"interior must remain paintable");

        // Example 2: leaf with a separate extra pencil line and kinked top.
        a=blank();
        line(a,57,190,69,125);line(a,69,125,104,105);
        line(a,104,105,167,110);line(a,167,110,179,132);
        line(a,179,132,205,154);line(a,205,154,194,202);
        line(a,194,202,112,233);line(a,112,233,57,190);
        line(a,167,110,183,94);line(a,183,94,196,83);
        valid(a,"separate lines with a top offshoot");

        // Example 3: interwoven branches crossing outside a clear lower loop.
        a=blank();loop(a,90,115,70,75);
        line(a,113,115,156,77);line(a,156,77,216,113);
        line(a,115,68,198,126);
        valid(a,"crossing strokes above closed lower shape");

        a=blank();loop(a,45,45,40,30);loop(a,150,150,65,40);
        r=valid(a,"two disconnected enclosed regions");
        check(r.interior[55*S+55]&&r.interior[160*S+160],
                "both bounded regions must be fillable");

        a=blank(); // 2-pixel break on top edge: one short gap can be healed
        line(a,40,40,69,40);
        line(a,72,40,120,40);
        line(a,120,40,120,130);
        line(a,120,130,40,130);
        line(a,40,130,40,40);
        r=valid(a,"small touch sampling gap");
        check(r.shortGapClosed,"short gap must be repaired");

        a=blank(); // deliberately open 12-pixel gap must remain invalid
        line(a,40,40,66,40);
        line(a,80,40,120,40);
        line(a,120,40,120,130);
        line(a,120,130,40,130);
        line(a,40,130,40,40);
        check(ContourDetector.detect(a,S)==null,"large opening should remain invalid");

        a=blank();line(a,30,30,70,80);line(a,70,80,30,140);
        check(ContourDetector.detect(a,S)==null,"simple open U is not a contour");

        a=blank();line(a,40,40,140,130);line(a,140,130,35,100);
        check(ContourDetector.detect(a,S)==null,"two open strokes are not a contour");

        System.out.println("ContourDetectorTest: multi-stroke, branches, crossings, gap healing and genuine open contours passed.");
    }
}
