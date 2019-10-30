package de.kaleidox.util.polyfill;

import com.sun.istack.internal.NotNull;
import de.kaleidox.JamesBot;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Timeout {
    ScheduledExecutorService schedule;

    public Timeout() {
        this.schedule = JamesBot.API.getThreadPool().getScheduler();
    }

    public ScheduledFuture<?> setInterval(@NotNull Runnable command, long interval) {
        return this.schedule.scheduleAtFixedRate(command, 0, interval, TimeUnit.MILLISECONDS);
    }

    public void clearInterval(@NotNull ScheduledFuture<?> interval) {
        interval.cancel(true);
    }

    public ScheduledFuture<?> setTimeout(@NotNull Runnable command, long timeout) {
        return this.schedule.scheduleWithFixedDelay(command, 0, timeout, TimeUnit.MILLISECONDS);
    }

    public void clearTimeout(@NotNull ScheduledFuture<?> timeout) {
        timeout.cancel(true);
    }
}