package de.naehe.gartenkueche;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Sparse reversible pixel edits, independent of stage/mode/tool navigation. */
final class AtelierHistory {
    private static final class OutlineState {
        final int points, startX, startY, endX, endY;

        OutlineState(PlantArt art) {
            points = art.points;
            startX = art.startX;
            startY = art.startY;
            endX = art.endX;
            endY = art.endY;
        }

        boolean matches(PlantArt art) {
            return points == art.points && startX == art.startX && startY == art.startY
                && endX == art.endX && endY == art.endY;
        }

        void restore(PlantArt art) {
            art.points = points;
            art.startX = startX;
            art.startY = startY;
            art.endX = endX;
            art.endY = endY;
        }
    }

    static final class Change {
        OutlineState oldStroke, newStroke;
        int[] ids, oldColors,newColors,oldPigments,newPigments;
        boolean[] oldOutline,newOutline,oldInside,newInside;
        int oldRegionCount,newRegionCount;
    }
    private final ArrayDeque<Change> undo=new ArrayDeque<>();
    private final ArrayDeque<Change> redo=new ArrayDeque<>();
    private PlantArt before;
    private static final int MAX=32;
    void begin(PlantArt art){before=art.copy();}
    void cancel(){before=null;}
    void record(PlantArt current){
        if(before==null)return;
        // Two primitive-array passes avoid boxing 90,000 Integer objects
        // after a full-area bucket fill on older Android devices.
        int n=0;
        for(int i=0;i<PlantArt.TOTAL;i++) {
            if(before.outline[i]!=current.outline[i] ||
               before.outlineColors[i]!=current.outlineColors[i] ||
               before.pigments[i]!=current.pigments[i])n++;
        }
        boolean maskChanged=(!Arrays.equals(before.inside,current.inside))
            || before.detectedRegions!=current.detectedRegions;
        OutlineState oldStroke = new OutlineState(before);
        boolean strokeChanged = !oldStroke.matches(current);
        if(n==0&&!maskChanged&&!strokeChanged){before=null;return;}
        Change c=new Change();
        if (strokeChanged) {
            c.oldStroke = oldStroke;
            c.newStroke = new OutlineState(current);
        }
        c.ids=new int[n];c.oldColors=new int[n];c.newColors=new int[n];
        c.oldPigments=new int[n];c.newPigments=new int[n];
        c.oldOutline=new boolean[n];c.newOutline=new boolean[n];
        int k=0;
        for(int i=0;i<PlantArt.TOTAL;i++){
            if(before.outline[i]==current.outline[i] &&
                before.outlineColors[i]==current.outlineColors[i] &&
                before.pigments[i]==current.pigments[i])continue;
            c.ids[k]=i;
            c.oldColors[k]=before.outlineColors[i];c.newColors[k]=current.outlineColors[i];
            c.oldPigments[k]=before.pigments[i];c.newPigments[k]=current.pigments[i];
            c.oldOutline[k]=before.outline[i];c.newOutline[k]=current.outline[i];
            k++;
        }
        if(maskChanged){
            c.oldInside=before.inside==null?null:before.inside.clone();
            c.newInside=current.inside==null?null:current.inside.clone();
            c.oldRegionCount=before.detectedRegions;c.newRegionCount=current.detectedRegions;
        } else {
            c.oldRegionCount=-1;c.newRegionCount=-1;
        }
        undo.addLast(c);
        while(undo.size()>MAX)undo.removeFirst();
        redo.clear();before=null;
    }
    boolean undo(PlantArt art){
        if(undo.isEmpty())return false;
        Change c=undo.removeLast();apply(art,c,false);redo.addLast(c);return true;
    }
    boolean redo(PlantArt art){
        if(redo.isEmpty())return false;
        Change c=redo.removeLast();apply(art,c,true);undo.addLast(c);return true;
    }
    private void apply(PlantArt art,Change c,boolean forward){
        if (c.oldStroke != null) {
            (forward ? c.newStroke : c.oldStroke).restore(art);
        }
        for(int k=0;k<c.ids.length;k++){
            int i=c.ids[k];
            art.outline[i]=forward?c.newOutline[k]:c.oldOutline[k];
            art.outlineColors[i]=forward?c.newColors[k]:c.oldColors[k];
            art.pigments[i]=forward?c.newPigments[k]:c.oldPigments[k];
        }
        if(c.oldRegionCount>=0){
            boolean[] mask=forward?c.newInside:c.oldInside;
            art.inside=mask==null?null:mask.clone();
            art.detectedRegions=forward?c.newRegionCount:c.oldRegionCount;
        }
    }
}
