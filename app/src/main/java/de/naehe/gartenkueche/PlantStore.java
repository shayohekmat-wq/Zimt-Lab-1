package de.naehe.gartenkueche;

import android.content.Context;
import android.util.Log;
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
        Arrays.sort(files, Comparator.comparing(File::getName));
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
                plants.add(plant);
            } catch(IOException ex) { Log.e(TAG, "Skipping corrupted plant record: "+f.getName(), ex); }
        }
        return plants;
    }

    private interface Writer { void write(DataOutputStream out) throws IOException; }
    private interface Reader<T> { T read(DataInputStream in, int version) throws IOException; }
    private void write(File destination, Writer writer) throws IOException {
        File temp = new File(root, destination.getName() + ".tmp");
        try (FileOutputStream stream = new FileOutputStream(temp);
             GZIPOutputStream gzip = new GZIPOutputStream(stream);
             DataOutputStream out = new DataOutputStream(gzip)) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            writer.write(out);
        }
        // Android/Linux rename is atomic within the same directory.
        if (!temp.renameTo(destination)) {
            if (destination.exists() && destination.delete() && temp.renameTo(destination)) return;
            throw new IOException("Atomic write failed");
        }
    }
    private <T> T read(File source, Reader<T> reader) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new GZIPInputStream(new FileInputStream(source)))) {
            if (in.readInt() != MAGIC) throw new IOException("Unsupported storage magic");
            int version = in.readInt();
            if(version != 2 && version != VERSION) throw new IOException("Unsupported storage version");
            return reader.read(in, version);
        }
    }
}
