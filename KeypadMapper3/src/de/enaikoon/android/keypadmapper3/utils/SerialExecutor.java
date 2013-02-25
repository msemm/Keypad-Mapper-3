package de.enaikoon.android.keypadmapper3.utils;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class SerialExecutor implements Executor {

    private Runnable active;

    private final Executor executor;

    private final Queue<Runnable> tasks = new LinkedBlockingQueue<Runnable>();

    public SerialExecutor(Executor executor) {
        this.executor = executor;
    }

    public synchronized void cancelAll() {
        tasks.clear();
    }

    @Override
    public synchronized void execute(final Runnable r) {
        if (tasks.size() < 3) {
            tasks.offer(new Runnable() {
                @Override
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
        }
        if (active == null) {
            scheduleNext();
        }
    }

    protected synchronized void scheduleNext() {
        if ((active = tasks.poll()) != null) {
            executor.execute(active);
        }
    }

}
