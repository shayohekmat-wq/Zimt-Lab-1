package de.naehe.gartenkueche;
public final class SpriteGeometryTest {
    static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
    public static void main(String[] args){
        int side=300;
        boolean[] line=new boolean[side*side];
        int[] colors=new int[line.length];
        SpriteGeometry.Box b=SpriteGeometry.bounds(line,colors,side,side);
        check(b.width()==0 && b.height()==0,"empty art");
        // Exactly one tiny plant occupies only 8x12, not the whole 300x300.
        for(int y=75;y<87;y++)for(int x=91;x<99;x++)colors[y*side+x]=0xffaabbcc;
        b=SpriteGeometry.bounds(line,colors,side,side);
        check(b.left==91 && b.top==75 && b.width()==8 && b.height()==12,"tiny crop");
        // Additional disconnected contour is in the same crop, with preserved spacing.
        line[238*side+252]=true;
        b=SpriteGeometry.bounds(line,colors,side,side);
        check(b.left==91 && b.top==75 && b.right==253 && b.bottom==239,
            "multiple disconnected contours must remain together");
        check(SpriteGeometry.worldLeft(400,b.left,side)==341,"horizontal origin");
        check(SpriteGeometry.worldTop(420,b.top,side)==195,"vertical origin");
        // Evolution changes crop, not shared baseline; absolute pixels stay in place.
        SpriteGeometry.Box next=new SpriteGeometry.Box(40,21,270,280);
        check(SpriteGeometry.worldLeft(400,next.left,side)==290,"new crop origin");
        check(SpriteGeometry.worldTop(420,next.top,side)==141,"new crop top");
        // Alpha-zero colors must not extend crop, colored pixels do.
        colors[1]=0x00ffffff;
        SpriteGeometry.Box unchanged=SpriteGeometry.bounds(line,colors,side,side);
        check(unchanged.left==91 && unchanged.top==75,"transparent pixels ignored");
        System.out.println("SpriteGeometryTest: empty, tight crop, multi-contour and stable anchors passed");
    }
}
