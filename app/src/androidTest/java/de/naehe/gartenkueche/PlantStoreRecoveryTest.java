package de.naehe.gartenkueche;

import android.content.Context;
import android.content.ContextWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class PlantStoreRecoveryTest {
    @Rule public TemporaryFolder storage = new TemporaryFolder();
    private Context context;
    private File root;

    @Before public void setUp() {
        context = new ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()) {
            @Override public File getFilesDir() { return storage.getRoot(); }
        };
        root = new File(storage.getRoot(), "plants_v2");
    }

    private void interrupt(String name, boolean leavePartialBase) throws Exception {
        File base = new File(root, name);
        assertTrue(base.renameTo(new File(root, name + ".bak")));
        if (leavePartialBase) {
            try (FileOutputStream out = new FileOutputStream(base)) { out.write(0); }
        }
    }

    @Test public void reopenRecoversArtworkAndWaterHistoryExactlyOnce() throws Exception {
        PlantStore store = new PlantStore(context);
        PlantStore.Plant plant = new PlantStore.Plant();
        PlantArt art = new PlantArt();
        art.drawContourLine(5, 5, 40, 5);
        plant.stages.add(art);
        plant.difficulties.add(2);
        plant.death = new PlantArt();
        store.savePlant(plant);
        plant.x = 71;
        plant.y = 158;
        plant.wateringCount = 2;
        plant.waterEventsUtc.addAll(Arrays.asList(111L, 222L));
        store.saveState(PlantStore.StateSnapshot.capture(plant));
        for (boolean partial : new boolean[]{false, true}) {
            interrupt("plant_" + plant.id + ".bin.gz", partial);
            interrupt("state_" + plant.id + ".bin", partial);
            List<PlantStore.Plant> restored = new PlantStore(context).loadPlants();
            assertEquals(1, restored.size());
            PlantStore.Plant result = restored.get(0);
            assertEquals(plant.id, result.id);
            assertArrayEquals(art.outline, result.stages.get(0).outline);
            assertEquals(plant.difficulties, result.difficulties);
            assertEquals(71, result.x);
            assertEquals(158, result.y);
            assertEquals(2, result.wateringCount);
            assertEquals(plant.waterEventsUtc, result.waterEventsUtc);
        }
    }

    @Test public void recoverAndExplicitlyClearAnOlderEditorDraft() throws Exception {
        PlantStore store = new PlantStore(context);
        PlantStore.Draft draft = PlantStore.Draft.empty(3);
        draft.active.drawContourLine(5, 5, 40, 5);
        store.saveDraft(draft);
        interrupt("draft.bin.gz", false);
        PlantStore.Draft restored = new PlantStore(context).loadDraft();
        assertNotNull(restored);
        assertEquals(3, restored.requestedCount);
        assertArrayEquals(draft.active.outline, restored.active.outline);
        interrupt("draft.bin.gz", true);
        store.clearDraft();
        assertNull(new PlantStore(context).loadDraft());
        assertFalse(new File(root, "draft.bin.gz.bak").exists());
    }
}
