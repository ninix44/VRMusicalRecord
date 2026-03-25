package org.vmstudio.musicalrecord.core.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vmstudio.musicalrecord.core.handler.MusicalRecordHandler;

@Mixin(JukeboxBlock.class)
public class JukeboxBlockMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void musicalrecord$guardRapidReuse(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!level.isClientSide) {
            return;
        }

        if (MusicalRecordHandler.INSTANCE.shouldBypassInteractionLock(pos)) {
            return;
        }

        if (MusicalRecordHandler.INSTANCE.isInteractionLocked(pos)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (state.getValue(JukeboxBlock.HAS_RECORD)) {
            MusicalRecordHandler.INSTANCE.triggerCooldown(pos);
            return;
        }

        if (MusicalRecordHandler.INSTANCE.isCoolingDown(pos)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
