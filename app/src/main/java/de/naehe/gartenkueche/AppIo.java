package de.naehe.gartenkueche;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** One ordered background I/O queue for all PlantStore operations. Java + API 23 safe. */
public final class AppIo {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "GartenKueche-PlantIO");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        }
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private AppIo() {}
    public static void execute(Runnable task) { WORKER.execute(task); }
    public static void onMain(Runnable task) { MAIN.post(task); }
}
