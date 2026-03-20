package org.vmstudio.musicalrecord.core.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
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

    private boolean wasMainInside = false;
    private boolean wasOffInside = false;

    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        VRLocalPlayer vrPlayer = VisorAPI.client().getVRLocalPlayer();
        if (vrPlayer != null && VisorAPI.clientState().playMode().canPlayVR()) {
            PlayerPoseClient pose = vrPlayer.getPoseData(PlayerPoseType.TICK);

            wasMainInside = processHand(mc, pose.getMainHand(), HandType.MAIN, InteractionHand.MAIN_HAND, wasMainInside);
            wasOffInside = processHand(mc, pose.getOffhand(), HandType.OFFHAND, InteractionHand.OFF_HAND, wasOffInside);
        }
    }

    private boolean processHand(Minecraft mc, VRPose handPose, HandType hType, InteractionHand mcHand, boolean wasInside) {
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
                            targetPos.getX() + 0.35, targetPos.getY() + 0.6, targetPos.getZ() + 0.35,
                            targetPos.getX() + 0.65, targetPos.getY() + 0.9, targetPos.getZ() + 0.65
                        );
                        if (handBox.intersects(deepSlotBox)) {
                            if (!wasInside) {
                                BlockHitResult hitResult = new BlockHitResult(controllerPos, Direction.UP, targetPos, false);
                                mc.gameMode.useItemOn(mc.player, mcHand, hitResult);

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
}
