package org.vmstudio.musicalrecord.forge;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.musicalrecord.core.client.MusicalRecordAddonClient;
import org.vmstudio.musicalrecord.core.common.MusicalRecord;
import org.vmstudio.musicalrecord.core.server.MusicalRecordAddonServer;
import net.minecraftforge.fml.common.Mod;

@Mod(MusicalRecord.MOD_ID)
public class MusicalRecordMod {
    public MusicalRecordMod(){
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
