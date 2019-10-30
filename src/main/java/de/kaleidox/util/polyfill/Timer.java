package de.kaleidox.util.polyfill;

import de.kaleidox.JamesBot;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Timer {
    private final ScheduledExecutorService schedule;


    public Timer() {
        this.schedule = JamesBot.API.getThreadPool().getScheduler();
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
}