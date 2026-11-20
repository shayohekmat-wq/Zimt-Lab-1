package de.naehe.gartenkueche;

import android.content.Context;
import android.util.Log;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Prototype disk persistence; schema is explicitly versioned for future SQLite migration. */
public final class PlantStore {
    private static final int MAGIC = 0x5A504C32;
    private static final int VERSION = 3;
    private static final String TAG = "GartenKuecheStorage";
    public static final String[] DIFFICULTIES = {"Pflegeleicht", "Medium", "Profi", "Diva"};
    private final File root;
    public PlantStore(Context context) {
        root = new File(context.getFilesDir(), "plants_v2");
        if (!root.exists() && !root.mkdirs()) Log.w(TAG, "Could not create plant directory");
    }

    public static final class Draft {
        public int requestedCount = 1;
        public final ArrayList<PlantArt> stages = new ArrayList<>();
        public final ArrayList<Integer> difficulties = new ArrayList<>();
        public PlantArt active = new PlantArt();
        public boolean colorMode;
        public boolean drawingDeath;
        public static Draft empty(int count) {
            Draft d = new Draft();
            d.requestedCount = Math.max(1, Math.min(10, count));
            return d;
        }
    }
    public static final class Plant {
        public String id = UUID.randomUUID().toString();
        public long createdAtUtc = System.currentTimeMillis();
        public long createdAtSimUtc; // local timelapse age clock, not transferable
        public final ArrayList<PlantArt> stages = new ArrayList<>();
        public final ArrayList<Integer> difficulties = new ArrayList<>();
        public PlantArt death;
        public int stage = 1;
        public int x = 400;
        public int y = 420;
        public int wateringCount;
        public long lastWaterAtUtc;
        public boolean dead;
        // Care-engine snapshot; concrete random values never reroll when resuming.
        public long stageEnteredAtUtc, windowStartUtc, windowEndUtc, lastEvaluatedAtUtc, windowOrdinal;
        public int windowWaterings, minWater, maxWater, windowDays, evolutionDays, strikesAtStageOne;
        public final ArrayList<Long> waterEventsUtc = new ArrayList<>();
    }

    public synchronized void saveDraft(Draft d) throws IOException {
        if (d.stages.size() > 10 || d.requestedCount < 1 || d.requestedCount > 10)
            throw new IOException("Invalid stage count");
        write(new File(root, "draft.bin.gz"), out -> {
            out.writeInt(1); // draft record
            out.writeInt(d.requestedCount);
            out.writeInt(d.stages.size());
            for (int i = 0; i < d.stages.size(); i++) {
                out.writeInt(d.difficulties.get(i));
                d.stages.get(i).write(out);
            }
            out.writeBoolean(d.colorMode);
            out.writeBoolean(d.drawingDeath);
            d.active.write(out);
        });
    }
    public synchronized Draft loadDraft() {
        File file = new File(root, "draft.bin.gz");
        if (!file.isFile()) return null;
        try {
            return read(file, (in, version) -> {
                if (in.readInt() != 1) throw new IOException("Not a draft");
                Draft d = Draft.empty(in.readInt());
                int n = in.readInt();
                if (n < 0 || n > d.requestedCount) throw new IOException("Bad stage count");
                for (int i=0;i<n;i++) {
                    int difficulty = in.readInt();
                    if (difficulty < 0 || difficulty > 3) throw new IOException("Bad difficulty");
                    d.difficulties.add(difficulty);
                    d.stages.add(PlantArt.read(in));
                }
                d.colorMode = in.readBoolean();
                d.drawingDeath = in.readBoolean();
                d.active = PlantArt.read(in);
                if (d.colorMode && !d.active.validate())
                    throw new IOException("Invalid colored draft mask");
                return d;
            });
        } catch (IOException error) {
            Log.e(TAG, "Draft cannot be loaded", error);
            return null;
        }
    }
    public synchronized void clearDraft() {
        File draft = new File(root, "draft.bin.gz");
        if (draft.exists() && !draft.delete()) Log.w(TAG, "Could not clear draft");
    }

    public synchronized void savePlant(Plant plant) throws IOException {
        if (plant.stages.isEmpty() || plant.stages.size() > 10 || plant.death == null)
            throw new IOException("Incomplete plant");
        if (!plant.id.matches("[A-Za-z0-9-]{1,64}")) throw new IOException("Invalid id");
        write(new File(root, "plant_" + plant.id + ".bin.gz"), out -> {
            out.writeInt(2); // plant record
            out.writeUTF(plant.id);
            out.writeLong(plant.createdAtUtc);
            out.writeInt(plant.stages.size());
            for(int i=0;i<plant.stages.size();i++){
                out.writeInt(plant.difficulties.get(i));
                plant.stages.get(i).write(out);
            }
            plant.death.write(out);
            out.writeInt(plant.stage); out.writeInt(plant.x); out.writeInt(plant.y);
            out.writeInt(plant.wateringCount); out.writeLong(plant.lastWaterAtUtc);
            out.writeBoolean(plant.dead);
            out.writeLong(plant.stageEnteredAtUtc);
            out.writeLong(plant.windowStartUtc);
            out.writeLong(plant.windowEndUtc);
            out.writeLong(plant.lastEvaluatedAtUtc);
            out.writeLong(plant.windowOrdinal);
            out.writeInt(plant.windowWaterings);
            out.writeInt(plant.minWater);
            out.writeInt(plant.maxWater);
            out.writeInt(plant.windowDays);
            out.writeInt(plant.evolutionDays);
            out.writeInt(plant.strikesAtStageOne);
            out.writeInt(plant.waterEventsUtc.size());
            for(long at : plant.waterEventsUtc) out.writeLong(at);
        });
    }

    public synchronized List<Plant> loadPlants() {
        ArrayList<Plant> plants = new ArrayList<>();
        File[] files = root.listFiles((dir, name) -> name.startsWith("plant_") && name.endsWith(".bin.gz"));
        if (files == null) return plants;
        Arrays.sort(files, (first, second) -> first.getName().compareTo(second.getName()));
        for (File f : files) {
            try {
                Plant plant = read(f, (in, version) -> {
                    if(in.readInt() != 2) throw new IOException("Not plant record");
                    Plant p = new Plant();
                    p.id=in.readUTF();
                    p.createdAtUtc=in.readLong();
                    int n=in.readInt();
                    if(n<1 || n>10) throw new IOException("Bad stage count");
                    for(int i=0;i<n;i++) {
                        int difficulty=in.readInt();
                        if (difficulty < 0 || difficulty > 3) throw new IOException("Bad difficulty");
                        p.difficulties.add(difficulty);
                        p.stages.add(PlantArt.read(in));
                    }
                    p.death=PlantArt.read(in);
                    p.stage=in.readInt();
                    p.x=in.readInt(); p.y=in.readInt();
                    p.wateringCount=in.readInt(); p.lastWaterAtUtc=in.readLong();
                    p.dead=in.readBoolean();
                    if(version >= 3) {
                        p.stageEnteredAtUtc=in.readLong();
                        p.windowStartUtc=in.readLong();
                        p.windowEndUtc=in.readLong();
                        p.lastEvaluatedAtUtc=in.readLong();
                        p.windowOrdinal=in.readLong();
                        p.windowWaterings=in.readInt();
                        p.minWater=in.readInt();
                        p.maxWater=in.readInt();
                        p.windowDays=in.readInt();
                        p.evolutionDays=in.readInt();
                        p.strikesAtStageOne=in.readInt();
                        int events=in.readInt();
                        if(events<0 || events>100000) throw new IOException("Invalid event count");
                        for(int j=0;j<events;j++) p.waterEventsUtc.add(in.readLong());
                    }
                    if(p.stage<1 || p.stage>n || p.x<0 || p.x>800 || p.y<0 || p.y>600 ||
                        p.wateringCount<0 || !p.id.matches("[A-Za-z0-9-]{1,64}"))
                        throw new IOException("Bad plant properties");
                    return p;
                });
                loadState(plant); // v0.0.5 status sidecar overrides old full-file snapshot
                plants.add(plant);
            } catch(IOException ex) { Log.e(TAG, "Skipping corrupted plant record: "+f.getName(), ex); }
        }
        return plants;
    }

    private interface Writer { void write(DataOutputStream out) throws IOException; }
    private interface Reader<T> { T read(DataInputStream in, int version) throws IOException; }
    private void write(File destination, Writer writer) throws IOException {
        AtomicFile atomic = new AtomicFile(destination);
        FileOutputStream stream = atomic.startWrite();
        try {
            GZIPOutputStream gzip = new GZIPOutputStream(stream);
            DataOutputStream out = new DataOutputStream(gzip);
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            writer.write(out);
            out.flush();
            gzip.finish();
            atomic.finishWrite(stream);
        } catch (IOException | RuntimeException error) {
            atomic.failWrite(stream);
            throw error;
        }
    }
    private <T> T read(File source, Reader<T> reader) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new GZIPInputStream(new AtomicFile(source).openRead()))) {
            if (in.readInt() != MAGIC) throw new IOException("Unsupported storage magic");
            int version = in.readInt();
            if(version != 2 && version != VERSION) throw new IOException("Unsupported storage version");
            return reader.read(in, version);
        }
    }
    /**
     * Small mutable plant state. Save this instead of the multi-stage artwork
     * when watering, dragging, or advancing care windows.
     * Capture on the UI thread, write the immutable snapshot on the IO worker.
     */
    public static final class StateSnapshot {
        public final String id;
        public final int stage, x, y, wateringCount;
        public final boolean dead;
        public final long createdAtSimUtc, lastWaterAtUtc, stageEnteredAtUtc, windowStartUtc,
                windowEndUtc, lastEvaluatedAtUtc, windowOrdinal;
        public final int windowWaterings, minWater, maxWater, windowDays,
                evolutionDays, strikesAtStageOne;
        public final long[] waterEventsUtc;
        private StateSnapshot(Plant p) {
            id=p.id; stage=p.stage; x=p.x; y=p.y; wateringCount=p.wateringCount;
            dead=p.dead; createdAtSimUtc=p.createdAtSimUtc;
            lastWaterAtUtc=p.lastWaterAtUtc;
            stageEnteredAtUtc=p.stageEnteredAtUtc; windowStartUtc=p.windowStartUtc;
            windowEndUtc=p.windowEndUtc; lastEvaluatedAtUtc=p.lastEvaluatedAtUtc;
            windowOrdinal=p.windowOrdinal; windowWaterings=p.windowWaterings;
            minWater=p.minWater; maxWater=p.maxWater;
            windowDays=p.windowDays; evolutionDays=p.evolutionDays;
            strikesAtStageOne=p.strikesAtStageOne;
            waterEventsUtc=new long[p.waterEventsUtc.size()];
            for(int i=0;i<waterEventsUtc.length;i++) waterEventsUtc[i]=p.waterEventsUtc.get(i);
        }
        public static StateSnapshot capture(Plant p) { return new StateSnapshot(p); }
        void restore(Plant p) throws IOException {
            if(!id.equals(p.id) || stage<1 || stage>p.stages.size() ||
               x<0 || x>800 || y<0 || y>600 || wateringCount<0 ||
               strikesAtStageOne<0 || waterEventsUtc.length>100000)
                throw new IOException("Invalid plant state");
            p.stage=stage;p.x=x;p.y=y;p.wateringCount=wateringCount;p.dead=dead;
            p.createdAtSimUtc=createdAtSimUtc;
            p.lastWaterAtUtc=lastWaterAtUtc;p.stageEnteredAtUtc=stageEnteredAtUtc;
            p.windowStartUtc=windowStartUtc;p.windowEndUtc=windowEndUtc;
            p.lastEvaluatedAtUtc=lastEvaluatedAtUtc;p.windowOrdinal=windowOrdinal;
            p.windowWaterings=windowWaterings;p.minWater=minWater;p.maxWater=maxWater;
            p.windowDays=windowDays;p.evolutionDays=evolutionDays;
            p.strikesAtStageOne=strikesAtStageOne;
            p.waterEventsUtc.clear();
            for(long t:waterEventsUtc)p.waterEventsUtc.add(t);
        }
    }

    public synchronized void saveState(StateSnapshot snapshot) throws IOException {
        if(!snapshot.id.matches("[A-Za-z0-9-]{1,64}"))throw new IOException("Invalid id");
        AtomicFile file=new AtomicFile(new File(root,"state_"+snapshot.id+".bin"));
        FileOutputStream stream=file.startWrite();
        try {
            DataOutputStream out=new DataOutputStream(new BufferedOutputStream(stream));
            out.writeInt(0x5A535435); // ZST5
            out.writeInt(2);
            out.writeUTF(snapshot.id);
            out.writeInt(snapshot.stage);out.writeInt(snapshot.x);out.writeInt(snapshot.y);
            out.writeInt(snapshot.wateringCount);out.writeBoolean(snapshot.dead);
            out.writeLong(snapshot.createdAtSimUtc);
            out.writeLong(snapshot.lastWaterAtUtc);out.writeLong(snapshot.stageEnteredAtUtc);
            out.writeLong(snapshot.windowStartUtc);out.writeLong(snapshot.windowEndUtc);
            out.writeLong(snapshot.lastEvaluatedAtUtc);out.writeLong(snapshot.windowOrdinal);
            out.writeInt(snapshot.windowWaterings);out.writeInt(snapshot.minWater);
            out.writeInt(snapshot.maxWater);out.writeInt(snapshot.windowDays);
            out.writeInt(snapshot.evolutionDays);out.writeInt(snapshot.strikesAtStageOne);
            out.writeInt(snapshot.waterEventsUtc.length);
            for(long time:snapshot.waterEventsUtc)out.writeLong(time);
            out.flush();
            file.finishWrite(stream);
        }catch(IOException | RuntimeException error){
            file.failWrite(stream);
            throw error;
        }
    }
    private void loadState(Plant p) {
        File path=new File(root,"state_"+p.id+".bin");
        if(!path.exists())return; // pre-0.0.5 archive or new definition
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(
                new AtomicFile(path).openRead()))) {
            if(in.readInt()!=0x5A535435)throw new IOException("Bad state schema");
            int stateVersion=in.readInt();
            if(stateVersion!=1 && stateVersion!=2)throw new IOException("Bad state version");
            Plant copy=new Plant();
            copy.id=in.readUTF();
            copy.stage=in.readInt();copy.x=in.readInt();copy.y=in.readInt();
            copy.wateringCount=in.readInt();copy.dead=in.readBoolean();
            if(stateVersion>=2)copy.createdAtSimUtc=in.readLong();
            copy.lastWaterAtUtc=in.readLong();copy.stageEnteredAtUtc=in.readLong();
            copy.windowStartUtc=in.readLong();copy.windowEndUtc=in.readLong();
            copy.lastEvaluatedAtUtc=in.readLong();copy.windowOrdinal=in.readLong();
            copy.windowWaterings=in.readInt();copy.minWater=in.readInt();
            copy.maxWater=in.readInt();copy.windowDays=in.readInt();
            copy.evolutionDays=in.readInt();copy.strikesAtStageOne=in.readInt();
            int count=in.readInt();
            if(count<0 || count>100000)throw new IOException("Bad history length");
            for(int i=0;i<count;i++)copy.waterEventsUtc.add(in.readLong());
            // Snapshot on a state-only copy, then restore with validation.
            StateSnapshot.capture(copy).restore(p);
        }catch(IOException error){
            Log.e(TAG,"State unreadable; keeping base plant archive: "+p.id,error);
        }
    }

}
