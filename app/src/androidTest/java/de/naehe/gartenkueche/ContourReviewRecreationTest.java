package de.naehe.gartenkueche;

import android.content.Context;
import android.content.Intent;
import android.util.AtomicFile;
import android.view.View;
import android.widget.Button;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class ContourReviewRecreationTest {
    private static final int PAINT = 0xffbc4152;

    @Test public void acceptAfterRecreationKeepsPaintUnderNewOutline() throws Exception {
        verifyDecision(true);
    }

    @Test public void rejectAfterRecreationRevealsOriginalPaint() throws Exception {
        verifyDecision(false);
    }

    @Test public void advancingToEditedStageStillRequiresReview() throws Exception {
        verifyNavigationToEditedContour(false);
    }

    @Test public void reopeningCustomDeathDrawingStillRequiresReview() throws Exception {
        verifyNavigationToEditedContour(true);
    }

    @Test public void interruptedDraftWriteRecoversPreviousDrawing() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtelierStore store = new AtelierStore(context);
        assertNull("Test requires an installation without an existing atelier draft", store.load());
        try {
            AtelierDocument saved = draftWithPaintedContourChange();
            store.save(saved);
            AtomicFile file = new AtomicFile(new File(context.getFilesDir(), "atelier_draft_v1.gz"));
            // Simulate process loss after opening a new revision, before finishWrite or failWrite.
            try (FileOutputStream interrupted = file.startWrite()) {
                interrupted.write(0x1f);
            }
            AtelierDocument recovered = new AtelierStore(context).load();
            assertNotNull(recovered);
            assertEquals(saved.revision, recovered.revision);
            assertTrue(recovered.currentNeedsReview());
            int center = 60 * PlantArt.SIZE + 60;
            assertTrue(recovered.current().outline[center]);
            assertFalse(recovered.acceptedCurrent().outline[center]);
            assertEquals(PAINT, recovered.current().pigments[center]);
            recovered.restoreAcceptedOutline();
            assertEquals(PAINT, recovered.current().renderedPixel(60, 60));
        } finally {
            store.clear();
        }
    }

    private static void verifyNavigationToEditedContour(boolean death) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtelierStore store = new AtelierStore(context);
        assertNull("Test requires an installation without an existing atelier draft", store.load());
        try (AutoCloseable cleanup = () -> {
            awaitEditorIo();
            store.clear();
        }) {
            AtelierDocument edited = draftWithPaintedContourChange();
            AtelierDocument draft = new AtelierDocument(death ? 1 : 2);
            draft.acceptCurrent(edited.acceptedCurrent().copy());
            draft.colorMode = true;
            if (death) {
                draft.active = draft.count;
                draft.completed[0] = true;
                draft.death = edited.current();
                draft.acceptedDeath = edited.acceptedCurrent();
                draft.deathNeedsReview = true;
            } else {
                draft.stages[1] = edited.current();
                draft.accepted[1] = edited.acceptedCurrent();
                draft.needsReview[1] = true;
            }
            store.save(draft);
            Intent intent = new Intent(context, AtelierEditorActivity.class)
                .putExtra("resume_draft", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try (ActivityScenario<AtelierEditorActivity> scenario = ActivityScenario.launch(intent)) {
                awaitEditorIo();
                click(scenario, death ? "Eigene Todesform zeichnen  ✎" : "Weiter  →");
                click(scenario, "Zu Farbe  →");
                awaitEditorIo();
                click(scenario, "Alte Kontur");
            }
            awaitEditorIo();
            AtelierDocument restored = store.load();
            assertNotNull(restored);
            assertEquals(death, restored.deathMode);
            assertTrue(restored.colorMode);
            assertFalse(restored.currentNeedsReview());
            assertFalse(restored.current().outline[60 * PlantArt.SIZE + 60]);
            assertEquals(PAINT, restored.current().renderedPixel(60, 60));
        }
    }

    private static void verifyDecision(boolean accept) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtelierStore store = new AtelierStore(context);
        assertNull("Test requires an installation without an existing atelier draft", store.load());
        try (AutoCloseable cleanup = () -> {
            awaitEditorIo();
            store.clear();
        }) {
            store.save(draftWithPaintedContourChange());
            Intent intent = new Intent(context, AtelierEditorActivity.class)
                .putExtra("resume_draft", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try (ActivityScenario<AtelierEditorActivity> scenario = ActivityScenario.launch(intent)) {
                awaitEditorIo();
                click(scenario, "◉  Farbe");
                awaitEditorIo();
                scenario.onActivity(activity -> button(activity, "Alte Kontur"));
                scenario.recreate();
                // Restoring loads the draft, then validates its contour on the same worker.
                awaitEditorIo();
                awaitEditorIo();
                click(scenario, accept ? "Behalten & weiter" : "Alte Kontur");
            }
            awaitEditorIo();
            AtelierDocument result = store.load();
            assertNotNull(result);
            assertTrue(result.colorMode);
            assertFalse(result.currentNeedsReview());
            int pixel = 60 * PlantArt.SIZE + 60;
            assertEquals(accept, result.acceptedCurrent().outline[pixel]);
            assertEquals(accept, result.current().outline[pixel]);
            assertEquals(PAINT, result.current().pigments[pixel]);
            if (!accept) assertEquals(PAINT, result.current().renderedPixel(60, 60));
        }
    }

    private static AtelierDocument draftWithPaintedContourChange() {
        AtelierDocument draft = new AtelierDocument(1);
        PlantArt art = draft.current();
        art.drawContourLine(40, 40, 80, 40);
        art.drawContourLine(80, 40, 80, 80);
        art.drawContourLine(80, 80, 40, 80);
        art.drawContourLine(40, 80, 40, 40);
        assertTrue(art.validate());
        draft.acceptCurrent(art);
        art.colorPixel(60, 60, PAINT);
        draft.beginContourEdit();
        art.drawContourLine(60, 40, 60, 80);
        draft.markReview();
        return draft;
    }

    static void awaitEditorIo() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AppIo.execute(done::countDown);
        assertTrue("Editor I/O stalled", done.await(10, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    static void click(ActivityScenario<AtelierEditorActivity> scenario, String label) {
        scenario.onActivity(activity -> assertTrue(button(activity, label).performClick()));
    }

    private static Button button(AtelierEditorActivity activity, String label) {
        ArrayList<View> matches = new ArrayList<>();
        activity.getWindow().getDecorView().findViewsWithText(matches, label, View.FIND_VIEWS_WITH_TEXT);
        for (View view : matches) {
            if (view instanceof Button && view.isShown() && label.contentEquals(((Button)view).getText())) {
                return (Button)view;
            }
        }
        throw new AssertionError("Missing editor button: " + label);
    }
}
