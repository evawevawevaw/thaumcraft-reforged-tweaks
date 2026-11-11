package dev.reforgedtweaks.client;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshRequestsTest {
    @Test
    public void startupAndManyEventsProduceOneRefreshAndIdleTicksProduceNone() {
        RefreshRequests requests = new RefreshRequests();
        AtomicInteger calls = new AtomicInteger();
        requests.install(calls::incrementAndGet);
        for (int i = 0; i < 50; i++) requests.request();
        requests.drain();
        for (int i = 0; i < 20; i++) requests.drain();
        assertEquals(1, calls.get());
    }

    @Test
    public void eventDuringRefreshSurvivesUntilNextTick() {
        RefreshRequests requests = new RefreshRequests();
        AtomicInteger calls = new AtomicInteger();
        requests.install(() -> {
            if (calls.incrementAndGet() == 1) requests.request();
        });
        requests.drain();
        assertEquals(1, calls.get());
        requests.drain();
        requests.drain();
        assertEquals(2, calls.get());
    }

    @Test
    public void oldRuntimeTeardownCannotDetachNewRuntime() {
        RefreshRequests requests = new RefreshRequests();
        AtomicInteger oldCalls = new AtomicInteger();
        AtomicInteger newCalls = new AtomicInteger();
        Runnable oldRuntime = oldCalls::incrementAndGet;
        Runnable newRuntime = newCalls::incrementAndGet;
        requests.install(oldRuntime);
        requests.request();
        requests.install(newRuntime);
        assertFalse(requests.remove(oldRuntime));
        assertFalse(requests.owns(oldRuntime));
        assertTrue(requests.owns(newRuntime));
        requests.drain();
        assertEquals(0, oldCalls.get());
        assertEquals(1, newCalls.get());
    }

    @Test
    public void invalidationsWithoutRuntimeDoNotLoseNextStartupRefresh() {
        RefreshRequests requests = new RefreshRequests();
        requests.request();
        requests.drain();
        AtomicInteger calls = new AtomicInteger();
        Runnable listener = calls::incrementAndGet;
        requests.install(listener);
        requests.drain();
        assertEquals(1, calls.get());
        assertTrue(requests.remove(listener));
        requests.request();
        requests.drain();
        assertEquals(1, calls.get());
    }
}
