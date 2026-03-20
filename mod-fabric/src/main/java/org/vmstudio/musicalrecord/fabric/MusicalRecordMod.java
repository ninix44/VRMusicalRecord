package org.vmstudio.musicalrecord.fabric;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.musicalrecord.core.client.MusicalRecordAddonClient;
import org.vmstudio.musicalrecord.core.server.MusicalRecordAddonServer;
import net.fabricmc.api.ModInitializer;

public class MusicalRecordMod implements ModInitializer {
    @Override
    public void onInitialize() {
        if(ModLoader.get().isDedicatedServer()){
            VisorAPI.registerAddon(
                    new MusicalRecordAddonServer()
            );
        }else{
            VisorAPI.registerAddon(
                    new MusicalRecordAddonClient()
            );
        }
    }
}
