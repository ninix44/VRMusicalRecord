package org.vmstudio.musicalrecord.core.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.VRLocalPlayer;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.common.player.VRPose;

public class MusicalRecordHandler {

    public static final MusicalRecordHandler INSTANCE = new MusicalRecordHandler();
    private static final int INSERT_ANIMATION_TICKS = 18;
    private static final int INTERACTION_COOLDOWN_TICKS = 12;
    private static final int POST_INSERT_COOLDOWN_TICKS = 40;
    private static final int INSERT_CONFIRM_TIMEOUT_TICKS = 40;

    private PendingInsertion pendingInsertion;

    private boolean wasMainInside = false;
    private boolean wasOffInside = false;
    private int interactionCooldownTicks = 0;
    private BlockPos cooldownPos;
    private BlockPos activeJukeboxPos;
    private boolean activeJukeboxConfirmedRecord;
    private int activeJukeboxUnlockDelayTicks;
    private int activeJukeboxConfirmTimeoutTicks;
    private boolean internalInsertionUse;

    private static class PendingInsertion {
        private final BlockPos targetPos;
        private final InteractionHand hand;
        private final HandType handType;
        private final ItemStack stack;
        private final int selectedSlot;
        private boolean handHidden;
        private int ticks;

        private PendingInsertion(BlockPos targetPos, InteractionHand hand, HandType handType, ItemStack stack, int selectedSlot) {
            this.targetPos = targetPos;
            this.hand = hand;
            this.handType = handType;
            this.stack = stack;
            this.selectedSlot = selectedSlot;
        }
    }

    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        tickActiveJukebox(mc);

        if (interactionCooldownTicks > 0) {
            interactionCooldownTicks--;
            if (interactionCooldownTicks == 0) {
                cooldownPos = null;
            }
        }

        tickPendingInsertion(mc);

        VRLocalPlayer vrPlayer = VisorAPI.client().getVRLocalPlayer();
        if (vrPlayer != null && VisorAPI.clientState().playMode().canPlayVR()) {
            PlayerPoseClient pose = vrPlayer.getPoseData(PlayerPoseType.TICK);

            wasMainInside = processHand(mc, pose.getMainHand(), HandType.MAIN, InteractionHand.MAIN_HAND, wasMainInside);
            wasOffInside = processHand(mc, pose.getOffhand(), HandType.OFFHAND, InteractionHand.OFF_HAND, wasOffInside);
        }
    }

    private boolean processHand(Minecraft mc, VRPose handPose, HandType hType, InteractionHand mcHand, boolean wasInside) {
        if (pendingInsertion != null || interactionCooldownTicks > 0 || isAnyJukeboxLocked()) {
            return true;
        }

        if (!(mc.player.getItemInHand(mcHand).getItem() instanceof RecordItem)) {
            return false;
        }

        Vector3fc posJoml = handPose.getPosition();
        Vec3 controllerPos = new Vec3(posJoml.x(), posJoml.y(), posJoml.z());

        AABB handBox = new AABB(controllerPos, controllerPos).inflate(0.05);

        BlockPos centerPos = BlockPos.containing(controllerPos.x, controllerPos.y, controllerPos.z);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos targetPos = centerPos.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(targetPos);

                    if (state.is(Blocks.JUKEBOX) && !state.getValue(JukeboxBlock.HAS_RECORD)) {
                        AABB deepSlotBox = new AABB(
                            targetPos.getX() + 0.43, targetPos.getY() + 0.83, targetPos.getZ() + 0.43,
                            targetPos.getX() + 0.57, targetPos.getY() + 1.06, targetPos.getZ() + 0.57
                        );
                        if (handBox.intersects(deepSlotBox)) {
                            if (!wasInside) {
                                pendingInsertion = new PendingInsertion(
                                    targetPos,
                                    mcHand,
                                    hType,
                                    mc.player.getItemInHand(mcHand).copy(),
                                    mc.player.getInventory().selected
                                );
                                mc.player.setItemInHand(mcHand, ItemStack.EMPTY);
                                pendingInsertion.handHidden = true;
                                VisorAPI.client().getInputManager().triggerHapticPulse(hType, 160f, 1.0f, 0.1f);
                            }

                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void tickPendingInsertion(Minecraft mc) {
        if (pendingInsertion == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }

        lockPendingHandSlot(mc.player.getInventory());

        BlockState state = mc.level.getBlockState(pendingInsertion.targetPos);
        if (!state.is(Blocks.JUKEBOX) || state.getValue(JukeboxBlock.HAS_RECORD)) {
            restoreHiddenHand(mc);
            triggerCooldown(pendingInsertion.targetPos, INTERACTION_COOLDOWN_TICKS);
            pendingInsertion = null;
            return;
        }

        if (!pendingInsertion.handHidden && !(mc.player.getItemInHand(pendingInsertion.hand).getItem() instanceof RecordItem)) {
            triggerCooldown(pendingInsertion.targetPos, INTERACTION_COOLDOWN_TICKS);
            pendingInsertion = null;
            return;
        }

        pendingInsertion.ticks++;
        if (pendingInsertion.ticks >= INSERT_ANIMATION_TICKS) {
            restoreHiddenHand(mc);
            Vec3 usePos = Vec3.atCenterOf(pendingInsertion.targetPos).add(0.0, 0.02, 0.0);
            BlockHitResult hitResult = new BlockHitResult(usePos, Direction.UP, pendingInsertion.targetPos, false);
            internalInsertionUse = true;
            mc.gameMode.useItemOn(mc.player, pendingInsertion.hand, hitResult);
            internalInsertionUse = false;
            activeJukeboxPos = pendingInsertion.targetPos.immutable();
            activeJukeboxConfirmedRecord = false;
            activeJukeboxUnlockDelayTicks = 0;
            activeJukeboxConfirmTimeoutTicks = INSERT_CONFIRM_TIMEOUT_TICKS;
            if (mc.player.getItemInHand(pendingInsertion.hand).getItem() instanceof RecordItem) {
                mc.player.setItemInHand(pendingInsertion.hand, ItemStack.EMPTY);
            }
            VisorAPI.client().getInputManager().triggerHapticPulse(pendingInsertion.handType, 220f, 1.0f, 0.12f);
            pendingInsertion = null;
        }
    }

    private void tickActiveJukebox(Minecraft mc) {
        if (activeJukeboxPos == null || mc.level == null) {
            return;
        }

        BlockState state = mc.level.getBlockState(activeJukeboxPos);
        if (!state.is(Blocks.JUKEBOX)) {
            clearActiveJukebox();
            return;
        }

        if (state.getValue(JukeboxBlock.HAS_RECORD)) {
            if (!activeJukeboxConfirmedRecord) {
                activeJukeboxConfirmedRecord = true;
                activeJukeboxUnlockDelayTicks = POST_INSERT_COOLDOWN_TICKS;
            } else if (activeJukeboxUnlockDelayTicks > 0) {
                activeJukeboxUnlockDelayTicks--;
            }
            return;
        }

        if (!activeJukeboxConfirmedRecord) {
            if (activeJukeboxConfirmTimeoutTicks > 0) {
                activeJukeboxConfirmTimeoutTicks--;
            } else {
                clearActiveJukebox();
            }
            return;
        }

        mc.level.levelEvent(1011, activeJukeboxPos, 0);
        clearActiveJukebox();
    }

    private void restoreHiddenHand(Minecraft mc) {
        if (pendingInsertion == null || !pendingInsertion.handHidden || mc.player == null) {
            return;
        }

        lockPendingHandSlot(mc.player.getInventory());

        if (mc.player.getItemInHand(pendingInsertion.hand).isEmpty()) {
            mc.player.setItemInHand(pendingInsertion.hand, pendingInsertion.stack.copy());
        }
        pendingInsertion.handHidden = false;
    }

    private void lockPendingHandSlot(Inventory inventory) {
        if (pendingInsertion != null && pendingInsertion.hand == InteractionHand.MAIN_HAND) {
            inventory.selected = pendingInsertion.selectedSlot;
        }
    }

    public boolean isInteractionLocked(BlockPos pos) {
        if (pendingInsertion != null && pendingInsertion.targetPos.equals(pos)) {
            return true;
        }

        return activeJukeboxPos != null
            && activeJukeboxPos.equals(pos)
            && (!activeJukeboxConfirmedRecord || activeJukeboxUnlockDelayTicks > 0);
    }

    public boolean isCoolingDown(BlockPos pos) {
        return interactionCooldownTicks > 0 && pos.equals(cooldownPos);
    }

    public void triggerCooldown(BlockPos pos) {
        triggerCooldown(pos, INTERACTION_COOLDOWN_TICKS);
    }

    public void triggerCooldown(BlockPos pos, int ticks) {
        interactionCooldownTicks = ticks;
        cooldownPos = pos.immutable();
    }

    public boolean shouldBypassInteractionLock(BlockPos pos) {
        return internalInsertionUse
            && pendingInsertion != null
            && pendingInsertion.targetPos.equals(pos);
    }

    private boolean isAnyJukeboxLocked() {
        return activeJukeboxPos != null && (!activeJukeboxConfirmedRecord || activeJukeboxUnlockDelayTicks > 0);
    }

    private void clearActiveJukebox() {
        activeJukeboxPos = null;
        activeJukeboxConfirmedRecord = false;
        activeJukeboxUnlockDelayTicks = 0;
        activeJukeboxConfirmTimeoutTicks = 0;
    }

    public void renderInsertionAnimation(PoseStack poseStack, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (pendingInsertion == null || mc.player == null || mc.level == null) {
            return;
        }

        float progress = Math.min((pendingInsertion.ticks + partialTicks) / INSERT_ANIMATION_TICKS, 1.0f);
        float easedProgress = 1.0f - ((1.0f - progress) * (1.0f - progress));
        Vec3 renderPos = Vec3.atCenterOf(pendingInsertion.targetPos).add(0.0, 0.66 - (easedProgress * 0.64), 0.0);
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y, renderPos.z - cameraPos.z);
        poseStack.scale(0.55f, 0.55f, 0.55f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));

        mc.getItemRenderer().renderStatic(
            pendingInsertion.stack,
            ItemDisplayContext.FIXED,
            0x00F000F0,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            mc.level,
            0
        );

        poseStack.popPose();
        bufferSource.endBatch();
    }
}
