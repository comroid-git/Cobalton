package org.comroid.util.polyfill;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.comroid.Cobalton;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class Timer {
    private final ScheduledExecutorService schedule;


    public Timer() {
        this.schedule = Cobalton.API.getThreadPool().getScheduler();
    }

    public ScheduledFuture<?> setInterval(ScriptObjectMirror command, long interval) {
        return this.schedule.scheduleAtFixedRate(() -> command.call(null), 0, interval, TimeUnit.MILLISECONDS);
    }

    public void clearInterval(ScheduledFuture<?> interval) {
        interval.cancel(true);
    }

    public ScheduledFuture<?> setTimeout(ScriptObjectMirror command, long timeout) {
        return this.schedule.scheduleWithFixedDelay(() -> command.call(null), 0, timeout, TimeUnit.MILLISECONDS);
    }

    public void clearTimeout(ScheduledFuture<?> timeout) {
        timeout.cancel(true);
    }

    public ScheduledFuture<?> timedInterval(ScriptObjectMirror command, long interval) {
        return this.timedInterval(command, interval, 5000);
    }

    public ScheduledFuture<?> timedInterval(ScriptObjectMirror command, long interval, long timeout) {
        ScheduledFuture<?> handle = this.setInterval(command, interval);
        return this.schedule.scheduleWithFixedDelay(() -> this.clearInterval(handle), 0, timeout, TimeUnit.MILLISECONDS);
    }
}