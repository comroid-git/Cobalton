package de.kaleidox.util.polyfill;

import de.kaleidox.JamesBot;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Timer {
    ScheduledExecutorService schedule;

    public Timer() {
        this.schedule = JamesBot.API.getThreadPool().getScheduler();
    }

    public ScheduledFuture<?> setInterval(Runnable command, long interval) {
        return this.schedule.scheduleAtFixedRate(command, 0, interval, TimeUnit.MILLISECONDS);
    }

    public void clearInterval(ScheduledFuture<?> interval) {
        interval.cancel(true);
    }

    public ScheduledFuture<?> setTimeout(Runnable command, long timeout) {
        return this.schedule.scheduleWithFixedDelay(command, 0, timeout, TimeUnit.MILLISECONDS);
    }

    public void clearTimeout(ScheduledFuture<?> timeout) {
        timeout.cancel(true);
    }
}