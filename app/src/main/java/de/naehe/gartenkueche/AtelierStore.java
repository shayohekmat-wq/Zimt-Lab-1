package de.naehe.gartenkueche;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;

/**
 * Atomic atelier draft snapshots.
 * All calls happen on AppIo's serial worker; never block the drawing thread.
 */
public final class AtelierStore {
    private final AtomicFile file;
    public AtelierStore(Context context){
        file=new AtomicFile(new File(context.getApplicationContext().getFilesDir(),"atelier_draft_v1.gz"));
    }
    public void clear(){file.delete();}
    public void save(AtelierDocument d) throws IOException{
        FileOutputStream stream=file.startWrite();
        try{
            AtelierSnapshot.write(stream,d);
            file.finishWrite(stream);
        }catch(IOException | RuntimeException ex){
            file.failWrite(stream);throw ex;
        }
    }
    public AtelierDocument load() throws IOException {
        // openRead also restores a valid AtomicFile backup after interrupted writes.
        try(FileInputStream raw=file.openRead()){
            return AtelierSnapshot.read(raw);
        }catch(FileNotFoundException absent){
            File base=file.getBaseFile();
            if(base.exists() || new File(base.getPath()+".bak").exists())throw absent;
            return null;
        }
    }
}
