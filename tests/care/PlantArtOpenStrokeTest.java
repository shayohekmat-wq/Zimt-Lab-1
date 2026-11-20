package de.naehe.gartenkueche;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/** Tests the complete production PlantArt validation -> color -> crop -> save path. */
public final class PlantArtOpenStrokeTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void loop(PlantArt a,int x,int y,int width,int height){
        a.drawContourLine(x,y,x+width,y);
        a.drawContourLine(x+width,y,x+width,y+height);
        a.drawContourLine(x+width,y+height,x,y+height);
        a.drawContourLine(x,y+height,x,y);
    }
    public static void main(String[] args) throws Exception {
        PlantArt plant=new PlantArt();
        loop(plant,80,80,45,30);
        loop(plant,155,85,40,34);
        loop(plant,90,160,110,25);
        // Open stem / simple stroke entirely outside all enclosed regions.
        plant.drawContourLine(144,185,144,237);
        plant.drawContourLine(144,237,150,258);
        int stemTip=258*PlantArt.SIZE+150;
        check(plant.outline[stemTip],"input contains hand-drawn stem tip");
        boolean[] original=plant.outline.clone();
        check(plant.validate(),"3 shapes must validate");
        check(plant.detectedRegions()==3,"all 3 interior regions recognized");
        for(int i=0;i<PlantArt.TOTAL;i++){
            if(original[i])check(plant.outline[i],"validation deleted drawn pixel "+i);
        }
        check(plant.outline[stemTip],"thin open stem survives validation");
        check(plant.renderedPixel(150,258)==PlantArt.PURPLE,"stem renders violet");

        int green=Color.rgb(30,180,67);
        plant.fillAll(green);
        check(plant.renderedPixel(100,100)==green,"closed leaf interior colored");
        check(plant.renderedPixel(169,100)==green,"second leaf interior colored");
        check(plant.renderedPixel(120,170)==green,"bottom region colored");
        check(plant.renderedPixel(150,258)==PlantArt.PURPLE,"open stem not filled");
        plant.colorPixel(149,248,green);
        check(plant.renderedPixel(149,248)==0,"outside stem cannot be painted");

        PlantArt.Bounds bounds=plant.bounds();
        check(bounds.bottom==259,"crop must include full extended open stem");
        check(bounds.left==80 && bounds.right==201,"crop includes both other contours");
        Bitmap image=plant.croppedBitmap(bounds);
        check(image.getWidth()==121&&image.getHeight()==179,"tight union crop dimensions");
        check(image.getPixel(150-bounds.left,258-bounds.top)==PlantArt.PURPLE,
            "open stem present in cropped pixel art");

        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        plant.write(new DataOutputStream(bytes));
        PlantArt restored=PlantArt.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        check(restored.outline[stemTip],"serialized plant retains open stem");
        check(restored.renderedPixel(150,258)==PlantArt.PURPLE,"restored stem renders");
        check(restored.renderedPixel(100,100)==green,"restored fill retained");
        check(restored.bounds().bottom==259,"restored crop includes stem");

        PlantArt gray=plant.grayCopy();
        check(gray.renderedPixel(150,258)==0xFF565656,"gray death artwork keeps stem");
        check(gray.bounds().bottom==259,"gray death crop includes stem");
        System.out.println("PlantArtOpenStrokeTest: full 3-region stem, fill, crop, persistence and death grayscale passed.");
    }
}
