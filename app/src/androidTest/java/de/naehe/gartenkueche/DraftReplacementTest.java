package de.naehe.gartenkueche;

import android.app.Instrumentation;
import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.nio.file.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class DraftReplacementTest {
    @Test public void cancelKeepsTheOriginalDraftByteForByte() throws Exception {
        verifyReplacement(false);
    }

    @Test public void confirmPersistsNewDraftBeforeEditorStarts() throws Exception {
        verifyReplacement(true);
    }

    private void verifyReplacement(boolean confirm) throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        AtelierStore store = new AtelierStore(context);
        assertNull("Use a clean test installation", store.load());
        Instrumentation.ActivityMonitor editor = instrumentation.addMonitor(
                AtelierEditorActivity.class.getName(), null, true);
        try {
            AtelierDocument original = new AtelierDocument(1);
            original.current().drawContourLine(5, 5, 50, 50);
            original.markReview();
            store.save(original);
            File file = new File(context.getFilesDir(), "atelier_draft_v1.gz");
            byte[] before = Files.readAllBytes(file.toPath());
            try (ActivityScenario<StageSetupActivity> setup = ActivityScenario.launch(StageSetupActivity.class)) {
                ContourReviewRecreationTest.awaitEditorIo();
                onView(withText("✎  Neue Pflanze zeichnen  →")).perform(scrollTo(), click());
                onView(withText(confirm ? "Neue Pflanze beginnen" : "Abbrechen")).perform(click());
                ContourReviewRecreationTest.awaitEditorIo();
                if (confirm) {
                    // The editor is intercepted, so only the replacement flow can have saved this.
                    assertEquals(1, editor.getHits());
                    AtelierDocument saved = store.load();
                    assertNotNull(saved);
                    assertEquals(3, saved.count);
                    assertFalse(saved.currentNeedsReview());
                    assertEquals(0, saved.current().points);
                } else {
                    assertEquals(0, editor.getHits());
                    assertArrayEquals(before, Files.readAllBytes(file.toPath()));
                }
            }
        } finally {
            instrumentation.removeMonitor(editor);
            store.clear();
        }
    }
}
