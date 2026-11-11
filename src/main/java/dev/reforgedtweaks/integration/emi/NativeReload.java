package dev.reforgedtweaks.integration.emi;

import java.util.concurrent.atomic.AtomicBoolean;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.reforgedtweaks.client.ClientLifecycle;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Standalone only: consume data invalidations once, after any current EMI reload has finished. */
final class NativeReload {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean DIRTY = new AtomicBoolean();
    static void install() {
        if (!INSTALLED.compareAndSet(false,true)) return;
        ClientLifecycle.listen(() -> DIRTY.set(true));
        NeoForge.EVENT_BUS.addListener(NativeReload::tick);
    }
    static void registering() { DIRTY.set(false); }
    private static void tick(ClientTickEvent.Post event) {
        if (!DIRTY.get() || !ClientLifecycle.ready() || !EmiReloadManager.isLoaded()) return;
        if (DIRTY.compareAndSet(true,false)) {
            try { EmiReloadManager.reload(); }
            catch (RuntimeException failure) { org.slf4j.LoggerFactory.getLogger(NativeReload.class).error("EMI data reload failed; waiting for next invalidation",failure); }
        }
    }
}
