package de.naehe.gartenkueche;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class CanvasGestureTest {
    private static final int PAINT = 0xffe13546;

    @Test public void pinchWithFillLeavesPaintUntouched() throws Exception {
        checkGesture(4, MotionEvent.ACTION_POINTER_DOWN);
    }

    @Test public void pinchWithPenLeavesPaintUntouched() throws Exception {
        checkGesture(0, MotionEvent.ACTION_POINTER_DOWN);
    }

    @Test public void cancelledBrushLeavesPaintUntouched() throws Exception {
        checkGesture(1, MotionEvent.ACTION_CANCEL);
    }

    @Test public void completedPenStrokeStillPaints() throws Exception {
        checkGesture(0, MotionEvent.ACTION_UP);
    }

    @Test public void completedFillStillFills() throws Exception {
        checkGesture(4, MotionEvent.ACTION_UP);
    }

    private static void checkGesture(int tool, int outcome) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtelierStore store = new AtelierStore(context);
        assertNull("Gesture tests require an empty installation", store.load());
        try (AutoCloseable cleanup = () -> {
            ContourReviewRecreationTest.awaitEditorIo();
            store.clear();
        }) {
            AtelierDocument draft = new AtelierDocument(1);
            PlantArt art = draft.current();
            art.drawContourLine(50, 50, 250, 50);
            art.drawContourLine(250, 50, 250, 250);
            art.drawContourLine(250, 250, 50, 250);
            art.drawContourLine(50, 250, 50, 50);
            assertTrue(art.validate());
            draft.acceptCurrent(art);
            draft.colorMode = true;
            draft.selectedColor = PAINT;
            draft.tool = tool;
            store.save(draft);
            Intent intent = new Intent(context, AtelierEditorActivity.class)
                .putExtra("resume_draft", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try (ActivityScenario<AtelierEditorActivity> scenario = ActivityScenario.launch(intent)) {
                ContourReviewRecreationTest.awaitEditorIo();
                scenario.onActivity(activity -> {
                    View canvas = findCanvas(activity.findViewById(android.R.id.content));
                    assertNotNull(canvas);
                    long start = SystemClock.uptimeMillis();
                    send(canvas, start, MotionEvent.ACTION_DOWN, .40f);
                    send(canvas, start, MotionEvent.ACTION_MOVE, tool == 4 ? .40f : .48f);
                    if (outcome == MotionEvent.ACTION_POINTER_DOWN) {
                        int second = 1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                        send(canvas, start, MotionEvent.ACTION_POINTER_DOWN | second, .48f, .62f);
                        send(canvas, start, MotionEvent.ACTION_MOVE, .34f, .77f);
                        send(canvas, start, MotionEvent.ACTION_POINTER_UP | second, .34f, .77f);
                        send(canvas, start, MotionEvent.ACTION_MOVE, .30f);
                        send(canvas, start, MotionEvent.ACTION_UP, .30f);
                    } else {
                        send(canvas, start, outcome, tool == 4 ? .40f : .48f);
                    }
                });
            }
            ContourReviewRecreationTest.awaitEditorIo();
            AtelierDocument saved = store.load();
            assertNotNull(saved);
            if (outcome == MotionEvent.ACTION_UP) {
                assertEquals(PAINT, saved.current().renderedPixel(120, 150));
                assertEquals(tool == 4 ? PAINT : 0, saved.current().renderedPixel(180, 150));
            } else {
                assertArrayEquals(art.pigments, saved.current().pigments);
                assertTrue(Arrays.equals(art.outline, saved.current().outline));
                if (outcome == MotionEvent.ACTION_POINTER_DOWN) assertTrue(saved.zoom > 1);
            }
        }
    }

    private static View findCanvas(View view) {
        if (view.getClass().getSimpleName().equals("PixelView")) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findCanvas(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void send(View canvas, long start, int action, float... fractions) {
        MotionEvent.PointerProperties[] pointers = new MotionEvent.PointerProperties[fractions.length];
        MotionEvent.PointerCoords[] positions = new MotionEvent.PointerCoords[fractions.length];
        for (int i = 0; i < fractions.length; i++) {
            pointers[i] = new MotionEvent.PointerProperties();
            pointers[i].id = i;
            pointers[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            positions[i] = new MotionEvent.PointerCoords();
            positions[i].x = canvas.getWidth() * fractions[i];
            positions[i].y = canvas.getHeight() * .5f;
            positions[i].pressure = 1;
        }
        MotionEvent event = MotionEvent.obtain(start, SystemClock.uptimeMillis(), action,
            fractions.length, pointers, positions, 0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0);
        try {
            assertTrue(canvas.dispatchTouchEvent(event));
        } finally {
            event.recycle();
        }
    }
}
