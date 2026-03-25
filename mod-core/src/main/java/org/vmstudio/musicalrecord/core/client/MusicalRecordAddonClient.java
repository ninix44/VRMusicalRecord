package org.vmstudio.musicalrecord.core.client;

import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.client.render.RenderPipelineStage;
import org.vmstudio.visor.api.common.addon.VisorAddon;
import org.vmstudio.musicalrecord.core.client.overlays.VROverlayMusicalRecord;
import org.vmstudio.musicalrecord.core.common.MusicalRecord;
import org.vmstudio.musicalrecord.core.handler.MusicalRecordHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MusicalRecordAddonClient implements VisorAddon {
    @Override
    public void onAddonLoad() {
        VisorAPI.addonManager().getRegistries()
                .overlays()
                .registerComponents(
                        List.of(
                                new VROverlayMusicalRecord(
                                        this,
                                        VROverlayMusicalRecord.ID
                                )
                        )
                );

        ModLoader.get().addToRenderPipeline(
                RenderPipelineStage.AFTER_TRANSLUCENT,
                MusicalRecordHandler.INSTANCE::renderInsertionAnimation
        );
    }

    @Override
    public @Nullable String getAddonPackagePath() {
        return "org.vmstudio.musicalrecord.core.client";
    }

    @Override
    public @NotNull String getAddonId() {
        return MusicalRecord.MOD_ID;
    }
    @Override
    public @NotNull Component getAddonName() {
        return Component.literal(MusicalRecord.MOD_NAME);
    }
    @Override
    public String getModId() {
        return MusicalRecord.MOD_ID;
    }
}
