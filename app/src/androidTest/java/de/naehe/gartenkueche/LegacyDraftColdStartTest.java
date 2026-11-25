package de.naehe.gartenkueche;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

/** The CI harness installs the Lab 10 file, stops the process and invokes this test alone. */
public final class LegacyDraftColdStartTest {
    @Test public void rejectLegacyContourAfterColdStartKeepsPaint() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtelierStore store = new AtelierStore(context);
        AtelierDocument original = store.load();
        assertNotNull(original);
        assertEquals(2, original.count);
        assertTrue(original.currentNeedsReview());
        int pixel = 15 * PlantArt.SIZE + 15;
        assertTrue(original.current().outline[pixel]);
        assertFalse(original.acceptedCurrent().outline[pixel]);
        assertEquals(0xffa12233, original.current().pigments[pixel]);

        Intent resume = new Intent(context, AtelierEditorActivity.class)
                .putExtra("resume_draft", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try (ActivityScenario<AtelierEditorActivity> editor = ActivityScenario.launch(resume)) {
            ContourReviewRecreationTest.awaitEditorIo();
            ContourReviewRecreationTest.click(editor, "◉  Farbe");
            ContourReviewRecreationTest.awaitEditorIo();
            ContourReviewRecreationTest.click(editor, "Alte Kontur");
        }
        ContourReviewRecreationTest.awaitEditorIo();
        AtelierDocument saved = store.load();
        assertNotNull(saved);
        assertEquals(2, saved.count);
        assertFalse(saved.currentNeedsReview());
        assertFalse(saved.current().outline[pixel]);
        assertEquals(0xffa12233, saved.current().renderedPixel(15, 15));
    }
}
