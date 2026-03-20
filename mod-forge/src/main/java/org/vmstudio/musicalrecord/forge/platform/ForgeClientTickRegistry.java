package org.vmstudio.musicalrecord.forge.platform;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = "musicalrecord", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientTickRegistry {
    private static Consumer<Minecraft> tickHandler;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && tickHandler != null) {
            tickHandler.accept(Minecraft.getInstance());
        }
    }

    public static void init(Consumer<Minecraft> tickHandler) {
        ForgeClientTickRegistry.tickHandler = tickHandler;
    }
}
