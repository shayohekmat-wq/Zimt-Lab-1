package de.naehe.gartenkueche;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Version 2 disk format shared by every stage of an atelier draft. */
final class AtelierSnapshot {
    private static final int MAGIC=0x41544C39; // ATL9
    private static final int VERSION=2;

    static void write(OutputStream stream, AtelierDocument d) throws IOException {
        GZIPOutputStream gzip=new GZIPOutputStream(stream);
        DataOutputStream out=new DataOutputStream(gzip);
        out.writeInt(MAGIC);out.writeInt(VERSION);
        out.writeInt(d.count);out.writeInt(d.active);out.writeBoolean(d.deathMode);
        out.writeBoolean(d.deathNeedsReview);
        out.writeBoolean(d.deathCompleted);out.writeBoolean(d.colorMode);
        out.writeInt(d.palettePage);out.writeInt(d.selectedColor);out.writeInt(d.tool);
        out.writeInt(d.zoom);out.writeInt(d.panX);out.writeInt(d.panY);out.writeLong(d.revision);
        for(int i=0;i<d.count;i++){
            out.writeBoolean(d.completed[i]);out.writeInt(d.difficulty[i]);
            out.writeBoolean(d.needsReview[i]);
            d.stages[i].write(out);
            out.writeBoolean(d.accepted[i]!=null);
            if(d.accepted[i]!=null)d.accepted[i].write(out);
        }
        d.death.write(out);
        out.writeBoolean(d.acceptedDeath!=null);
        if(d.acceptedDeath!=null)d.acceptedDeath.write(out);
        out.flush();gzip.finish();
    }

    static AtelierDocument read(InputStream stream) throws IOException {
        try(DataInputStream in=new DataInputStream(new GZIPInputStream(stream))){
            if(in.readInt()!=MAGIC || in.readInt()!=VERSION)throw new IOException("Unsupported atelier version");
            int n=in.readInt();
            AtelierDocument d=new AtelierDocument(n);
            d.active=in.readInt();d.deathMode=in.readBoolean();
            d.deathNeedsReview=in.readBoolean();
            d.deathCompleted=in.readBoolean();d.colorMode=in.readBoolean();
            d.palettePage=in.readInt();d.selectedColor=in.readInt();d.tool=in.readInt();
            d.zoom=in.readInt();d.panX=in.readInt();d.panY=in.readInt();d.revision=in.readLong();
            if(d.active<0 || d.active>d.count || d.tool<0 || d.tool>4 ||
                    d.palettePage<0 || d.palettePage>5 || d.zoom<1 || d.zoom>10)
                throw new IOException("Corrupt editor state");
            for(int i=0;i<n;i++){
                d.completed[i]=in.readBoolean();d.difficulty[i]=in.readInt();
                d.needsReview[i]=in.readBoolean();
                if(d.difficulty[i]<0 || d.difficulty[i]>3)throw new IOException("Bad difficulty");
                d.stages[i]=PlantArt.read(in);
                if(in.readBoolean())d.accepted[i]=PlantArt.read(in);
            }
            d.death=PlantArt.read(in);
            if(in.readBoolean())d.acceptedDeath=PlantArt.read(in);
            // Reading to EOF also verifies the GZIP checksum and trailer.
            if(in.read()!=-1)throw new IOException("Unexpected data after atelier draft");
            return d;
        }catch(IllegalArgumentException malformed){
            throw new IOException("Invalid atelier draft", malformed);
        }
    }
}
