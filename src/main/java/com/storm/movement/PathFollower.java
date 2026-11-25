package com.storm.movement;

import com.storm.util.RotationHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

public class PathFollower {

    private static final float MAX_TURN_PER_TICK = 15f;
    private static final float WALK_IF_FACING_WITHIN = 30f;   // new: degrees
    private static final int LOOK_AHEAD = 2;
    private static boolean holdingForward = false;

    private static List<BlockPos> path = null;
    private static int index = 0;

    public static void setPath(List<BlockPos> newPath) {
        path = newPath;
        index = 1;
    }

    public static void stop() {
        path = null;
        index = 0;
        releaseForward();                                      // new
    }

    private static void releaseForward() {
        if (holdingForward) {
            Minecraft.getInstance().options.keyUp.setDown(false);
            holdingForward = false;
        }
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            var player = mc.player;
            if (player == null || path == null) {
                releaseForward();
                return;
            }


            if (index >= path.size()) {
                System.out.println("Path finished");
                stop();
                return;
            }

            BlockPos target = path.get(index);
            double targetX = target.getX() + 0.5;
            double targetZ = target.getZ() + 0.5;
            double dx = targetX - player.getX();
            double dz = targetZ - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist < 0.4) {
                index++;
                return;
            }

            // yaw
            RotationHandler.faceToward(player, targetX, targetZ, MAX_TURN_PER_TICK);

            // pitch (look-ahead)
            int lookIndex = Math.min(index + LOOK_AHEAD, path.size() - 1);
            BlockPos lookTarget = path.get(lookIndex);
            double lookDx = lookTarget.getX() + 0.5 - player.getX();
            double lookDz = lookTarget.getZ() + 0.5 - player.getZ();
            double lookDist = Math.sqrt(lookDx * lookDx + lookDz * lookDz);
            double lookDy = lookTarget.getY() - player.getY();
            float wantedPitch = RotationHandler.pitchTo(lookDy, lookDist, 0.5f, 20f);
            player.setXRot(RotationHandler.stepPitchToward(player.getXRot(), wantedPitch, 6f));

            // new: walk only when roughly facing the target
            float wantedYaw = RotationHandler.yawTo(player.getX(), player.getZ(), targetX, targetZ);
            float off = Math.abs(RotationHandler.angleDiff(player.getYRot(), wantedYaw));
            boolean walk = off < WALK_IF_FACING_WITHIN;
            mc.options.keyUp.setDown(walk);
            holdingForward = walk;
        });
    }
}