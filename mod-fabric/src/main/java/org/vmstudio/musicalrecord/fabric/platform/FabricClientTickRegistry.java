package org.vmstudio.musicalrecord.fabric.platform;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import java.util.function.Consumer;

public class FabricClientTickRegistry {
    public static void init(Consumer<Minecraft> tickHandler) {
        ClientTickEvents.END_CLIENT_TICK.register(tickHandler::accept);
    }
}
