package io.github.yamin8000.yarca;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

class Util {

    /**
     * Executes the given runnable on the UI thread.
     *
     * @see retrofit2.Platform.Android.MainThreadExecutor
     */
    static final class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable r) {
            handler.post(r);
        }
    }
}
