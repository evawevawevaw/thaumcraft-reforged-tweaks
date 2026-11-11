package dev.reforgedtweaks.client;

import dev.reforgedtweaks.ReforgedTweaks;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import thaumcraft.aspect.AspectRegistry;

/** No optional-viewer types: this subscriber can load without any viewer. */
@EventBusSubscriber(modid = ReforgedTweaks.MOD_ID, value = Dist.CLIENT)
public final class ClientLifecycle {
    private static AspectRegistry observed = AspectRegistry.active();
    private static Object connection;
    private static volatile boolean ready;
    private static final RefreshRequests REFRESH = new RefreshRequests();

    private ClientLifecycle() {}

    public static boolean ready() { return ready; }
    public static void listen(Runnable listener) { REFRESH.install(listener); }
    public static boolean unlisten(Runnable listener) { return REFRESH.remove(listener); }
    public static boolean owns(Runnable listener) { return REFRESH.owns(listener); }
    public static void resourcesChanged() { REFRESH.request(); }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Object currentConnection = minecraft.getConnection();
        boolean dirty = false;
        if (connection != currentConnection) {
            connection = currentConnection;
            ready = false;
            dirty = true;
        }
        AspectRegistry current = AspectRegistry.active();
        if (observed != current) {
            observed = current;
            // REFORGED replaces this object after decoding its own server payload.
            ready = connection != null && minecraft.level != null;
            dirty = true;
        }
        if (connection == null) ready = false;
        if (dirty) REFRESH.request();
        REFRESH.drain();
    }

    @SubscribeEvent
    public static void recipes(net.neoforged.neoforge.client.event.RecipesUpdatedEvent event) {
        REFRESH.request();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ready = false;
        connection = null;
        observed = AspectRegistry.active();
        REFRESH.request();
    }
}
