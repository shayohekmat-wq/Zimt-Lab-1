package de.naehe.gartenkueche;

import java.util.ArrayList;

/**
 * Finds a real inside/outside boundary in an arbitrary collection of pixel strokes.
 *
 * Unlike the old validator, this does NOT require one uninterrupted gesture,
 * touching start/end points, a connected stroke graph, or a lack of branches.
 * A shape is valid when at least one meaningful enclosed pixel region exists.
 */
public final class ContourDetector {
    private static final int MIN_INTERIOR = 12;
    private ContourDetector() {}

    public static final class Result {
        public final boolean[] outline;
        public final boolean[] interior;
        public final int area;
        public final boolean shortGapClosed;
        Result(boolean[] outline, boolean[] interior, int area, boolean bridged) {
            this.outline=outline; this.interior=interior; this.area=area;
            this.shortGapClosed=bridged;
        }
    }

    public static Result detect(boolean[] ink, int size) {
        if (size < 5 || ink.length != size*size)
            throw new IllegalArgumentException("Invalid logical pixel canvas");
        Result direct=detectEnclosure(ink,size,false);
        if(direct != null) return direct;

        // A small touch gap (up to four logical pixels) should not make an
        // otherwise enclosed freehand shape impossible to color. Try bridging
        // nearby tips, but ONLY keep bridges if the result encloses real pixels.
        ArrayList<Integer> tips=new ArrayList<>();
        for(int y=1;y<size-1;y++) for(int x=1;x<size-1;x++) {
            int i=y*size+x;
            if(!ink[i]) continue;
            int n=0;
            for(int yy=y-1;yy<=y+1;yy++) for(int xx=x-1;xx<=x+1;xx++)
                if((xx!=x || yy!=y) && ink[yy*size+xx]) n++;
            if(n<=1) tips.add(i);
        }
        // Only short candidate connections can heal a genuine <=4px gap.
        // Compute pair distances once, rank nearest tips first. At most 24
        // complete flood-fills are allowed (previously up to 12,720).
        if(tips.size()>160) return null;
        ArrayList<int[]> possible=new ArrayList<>();
        for(int a=0;a<tips.size();a++){
            int start=tips.get(a),x0=start%size,y0=start/size;
            for(int b=a+1;b<tips.size();b++){
                int end=tips.get(b),dx=end%size-x0,dy=end/size-y0;
                int d2=dx*dx+dy*dy;
                if(d2>=2 && d2<=16) possible.add(new int[]{a,b,d2});
            }
        }
        possible.sort((a,b)->Integer.compare(a[2],b[2]));
        int trials=Math.min(24,possible.size());
        for(int t=0;t<trials;t++){
            int[] pair=possible.get(t);
            int start=tips.get(pair[0]),end=tips.get(pair[1]);
            boolean[] candidate=ink.clone();
            connect(candidate,size,start%size,start/size,end%size,end/size);
            Result healed=detectEnclosure(candidate,size,true);
            if(healed!=null)return healed;
        }
        return null;
    }
    private static void connect(boolean[] target,int size,int x0,int y0,int x1,int y1) {
        int dx=Math.abs(x1-x0),dy=Math.abs(y1-y0);
        int sx=x0<x1?1:-1,sy=y0<y1?1:-1,error=dx-dy;
        while(true) {
            target[y0*size+x0]=true;
            if(x0==x1 && y0==y1) return;
            int twice=2*error;
            if(twice > -dy) {error-=dy;x0+=sx;}
            if(twice < dx) {error+=dx;y0+=sy;}
        }
    }
    private static int offer(int i,boolean[] visited,boolean[] wall,int[] queue,int tail) {
        if(!visited[i] && !wall[i]) {visited[i]=true;queue[tail++]=i;}
        return tail;
    }
    private static Result detectEnclosure(boolean[] drawn, int size,boolean healed) {
        int length=size*size;
        int[] queue=new int[length];
        boolean[] outside=new boolean[length];
        int head=0,tail=0;
        for(int i=0;i<size;i++) {
            tail=offer(i,outside,drawn,queue,tail);
            tail=offer((size-1)*size+i,outside,drawn,queue,tail);
            tail=offer(i*size,outside,drawn,queue,tail);
            tail=offer(i*size+size-1,outside,drawn,queue,tail);
        }
        while(head<tail) {
            int p=queue[head++],x=p%size,y=p/size;
            if(x>0) tail=offer(p-1,outside,drawn,queue,tail);
            if(x<size-1) tail=offer(p+1,outside,drawn,queue,tail);
            if(y>0) tail=offer(p-size,outside,drawn,queue,tail);
            if(y<size-1) tail=offer(p+size,outside,drawn,queue,tail);
        }

        // Accept multiple enclosed pockets: a crossing or branching silhouette
        // may subdivide its inside. Tiny accidental single-pixel pockets do not
        // make an otherwise open scribble into a plant.
        boolean[] interiors=new boolean[length],seen=new boolean[length];
        int total=0,maxRegion=0;
        for(int i=0;i<length;i++) {
            if(drawn[i] || outside[i] || seen[i]) continue;
            head=0;tail=0; seen[i]=true;queue[tail++]=i;
            boolean edge=false;
            while(head<tail) {
                int p=queue[head++],x=p%size,y=p/size;
                if(x==0||y==0||x==size-1||y==size-1) edge=true;
                if(x>0 && !drawn[p-1] && !outside[p-1] && !seen[p-1]){seen[p-1]=true;queue[tail++]=p-1;}
                if(x<size-1 && !drawn[p+1] && !outside[p+1] && !seen[p+1]){seen[p+1]=true;queue[tail++]=p+1;}
                if(y>0 && !drawn[p-size] && !outside[p-size] && !seen[p-size]){seen[p-size]=true;queue[tail++]=p-size;}
                if(y<size-1 && !drawn[p+size] && !outside[p+size] && !seen[p+size]){seen[p+size]=true;queue[tail++]=p+size;}
            }
            if(edge || tail < MIN_INTERIOR) continue;
            maxRegion=Math.max(maxRegion,tail);
            for(int q=0;q<tail;q++) interiors[queue[q]]=true;
            total+=tail;
        }
        if(maxRegion<MIN_INTERIOR) return null;

        // Keep only strokes touching a genuine enclosed region; dangling
        // decorative or stray lines outside the silhouette are automatically
        // removed on confirmation rather than blocking progress.
        boolean[] boundary=new boolean[length];
        int boundaryCount=0;
        for(int y=0;y<size;y++) for(int x=0;x<size;x++) {
            int i=y*size+x;
            if(!drawn[i]) continue;
            boolean touchesInside=false;
            for(int yy=Math.max(0,y-1);yy<=Math.min(size-1,y+1) && !touchesInside;yy++)
                for(int xx=Math.max(0,x-1);xx<=Math.min(size-1,x+1);xx++)
                    if(interiors[yy*size+xx]) {touchesInside=true;break;}
            if(touchesInside) {boundary[i]=true;boundaryCount++;}
        }
        if(boundaryCount < 8) return null;
        return new Result(boundary,interiors,total,healed);
    }
}
