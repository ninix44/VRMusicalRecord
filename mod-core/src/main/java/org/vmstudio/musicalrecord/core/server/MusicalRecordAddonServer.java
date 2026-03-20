package org.vmstudio.musicalrecord.core.server;

import org.vmstudio.visor.api.common.addon.VisorAddon;
import org.vmstudio.musicalrecord.core.common.MusicalRecord;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MusicalRecordAddonServer implements VisorAddon {
    @Override
    public void onAddonLoad() {

    }

    @Override
    public @Nullable String getAddonPackagePath() {
        return "org.vmstudio.musicalrecord.core.server";
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
