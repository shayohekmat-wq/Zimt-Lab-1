package de.naehe.gartenkueche;

import android.graphics.Color;

/** Contract tests: colour survives contour edits, stage navigation and undo. */
public final class AtelierWorkflowTest {
    static void check(boolean value,String error){if(!value)throw new AssertionError(error);}
    static void rectangle(PlantArt a,int x,int y,int w,int h){
        a.drawContourLine(x,y,x+w,y);
        a.drawContourLine(x+w,y,x+w,y+h);
        a.drawContourLine(x+w,y+h,x,y+h);
        a.drawContourLine(x,y+h,x,y);
    }
    public static void main(String[] args) throws Exception {
        AtelierDocument d=new AtelierDocument(3);
        AtelierHistory[] h={new AtelierHistory(),new AtelierHistory(),new AtelierHistory()};
        PlantArt art=d.current();
        rectangle(art,40,40,80,80);
        art.drawContourLine(85,120,86,166); // open stem must remain
        check(art.validate(),"initial body validates");
        d.acceptCurrent(art.copy());
        d.colorMode=true;
        final int red=Color.rgb(200,41,78);
        art=d.current();
        h[0].begin(art);
        art.fillAll(red);
        h[0].record(art);
        check(art.renderedPixel(80,82)==red,"colored flower body");

        // Return from colour to contour, draw a new branch THROUGH the colored
        // interior. The new outline will cover paint, but never erase it.
        d.beginContourEdit();
        h[0].begin(art);
        art.drawContourLine(80,40,80,120);
        h[0].record(art);
        d.markReview();
        PlantArt candidate=art.copy();
        check(candidate.validate(),"revised outline valid");
        check(d.threatenedColors(candidate)>0,"review prompted for covered paint");
        check(art.pigments[80+82*300]==red,"pigment remains in uncommitted art");
        PlantArt beforeCommit=d.acceptedCurrent().copy();
        System.arraycopy(art.pigments,0,beforeCommit.pigments,0,PlantArt.TOTAL);
        h[0].begin(beforeCommit);
        d.acceptCurrent(candidate);
        h[0].record(d.current());
        d.colorMode=true;
        check(d.current().pigments[80+82*300]==red,"pigment not erased after approval");
        check(d.current().renderedPixel(80,82)!=red,"outline temporarily covers paint");

        // Undo the newly accepted topology, then go to another stage, and back.
        check(h[0].undo(d.current()),"topology undo available across mode switch");
        check(d.current().renderedPixel(80,82)==red,"paint visible again after undo");
        d.completed[0]=true;
        d.active=1;d.colorMode=false;
        rectangle(d.current(),25,25,60,45);
        check(d.current().validate(),"stage 2 valid");
        d.acceptCurrent(d.current().copy());
        d.completed[1]=true;
        d.active=0;d.colorMode=true;
        check(d.current().renderedPixel(80,82)==red,"stage navigation preserved original paint");
        check(d.current().outline[86+166*300],"open stem preserved after stage navigation");

        // Undo/redo survives transitions between edit phases and stages.
        check(h[0].redo(d.current()),"topology redo available after stage navigation");
        check(d.current().pigments[80+82*300]==red,"redo did not destroy original paint");

        // Restore accepted outline without erasing any pigments.
        d.restoreAcceptedOutline();
        check(d.current().pigments[80+82*300]==red,"restoration never deletes pigment");
        AtelierDocument recovered=d.copy();
        check(recovered.count==3 && recovered.completed[1],"deep copy preserves stage progress");
        recovered.active=0;
        check(recovered.current().pigments[80+82*300]==red,"deep copy preserves color");
        check(recovered.stages[1].hasInterior(),"deep copy preserves previous stage");

        // Reopen exactly the bytes saved while a contour decision is pending.
        d.beginContourEdit();
        d.markReview();
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        AtelierSnapshot.write(bytes,d);
        byte[] snapshot=bytes.toByteArray();
        AtelierDocument reopened=AtelierSnapshot.read(new java.io.ByteArrayInputStream(snapshot));
        check(reopened.active==0 && reopened.currentNeedsReview(),"pending stage survived reload");
        check(reopened.stages[1].hasInterior(),"second stage survived reload");
        reopened.restoreAcceptedOutline();
        check(reopened.current().pigments[80+82*300]==red,"rejecting after reload preserves paint");
        try{
            AtelierSnapshot.read(new java.io.ByteArrayInputStream(snapshot,0,snapshot.length-1));
            throw new AssertionError("A truncated draft must raise a read failure");
        }catch(java.io.IOException expected){
            // This is an unreadable draft, distinct from an absent draft.
        }

        System.out.println("AtelierWorkflowTest: non-destructive topology review, undo/redo, stages and stem passed.");
    }
}
