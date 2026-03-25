package org.vmstudio.musicalrecord.core.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vmstudio.musicalrecord.core.handler.MusicalRecordHandler;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void musicalrecord$blockLockedJukeboxUse(
        LocalPlayer player,
        InteractionHand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (MusicalRecordHandler.INSTANCE.shouldBypassInteractionLock(hitResult.getBlockPos())) {
            return;
        }

        if (MusicalRecordHandler.INSTANCE.isInteractionLocked(hitResult.getBlockPos())
            || MusicalRecordHandler.INSTANCE.isCoolingDown(hitResult.getBlockPos())) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
