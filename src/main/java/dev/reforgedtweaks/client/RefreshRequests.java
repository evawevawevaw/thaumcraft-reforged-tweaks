package dev.reforgedtweaks.client;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Coalesces invalidations; the client tick is the only consumer. */
public final class RefreshRequests {
    private final AtomicBoolean pending = new AtomicBoolean();
    private final AtomicReference<Runnable> listener = new AtomicReference<>();

    public void install(Runnable next) {
        listener.set(Objects.requireNonNull(next));
        request();
    }

    public boolean remove(Runnable expected) {
        return listener.compareAndSet(expected, null);
    }

    public boolean owns(Runnable expected) {
        return listener.get() == expected;
    }

    public void request() {
        pending.set(true);
    }

    public void drain() {
        if (!pending.getAndSet(false)) return;
        Runnable current = listener.get();
        if (current != null) current.run();
    }
}
