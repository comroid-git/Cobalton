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

    public ScheduledFuture<?> setInterval(Object command, Object interval) throws ClassCastException {
        return this.schedule.scheduleAtFixedRate((Runnable) command, 0, (long) interval, TimeUnit.MILLISECONDS);
    }

    public void clearInterval(Object interval) throws ClassCastException {
        ((ScheduledFuture<?>)interval).cancel(true);
    }

    public ScheduledFuture<?> setTimeout(Object command, Object timeout) throws ClassCastException {
        return this.schedule.scheduleWithFixedDelay((Runnable) command, 0, (long) timeout, TimeUnit.MILLISECONDS);
    }

    public void clearTimeout(Object timeout) throws ClassCastException {
        ((ScheduledFuture<?>)timeout).cancel(true);
    }
}