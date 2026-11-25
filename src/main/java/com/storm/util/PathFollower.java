package com.storm.util;

import com.storm.pathfinding.Heuristic;
import com.storm.pathfinding.Pathfinder;
import com.storm.pathfinding.Walkability;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.List;

public class PathFollower {

    private static final float WALK_IF_FACING_WITHIN = 50f;   // degrees
    private static final int MAX_SKIP = 8;                   // how many nodes ahead we may aim straight at
    private static final double NODE_REACHED = 0.6;           // horizontal distance that counts as "at" a node
    private static final double GOAL_REACHED = 0.4;
    private static final double BODY_HALF_WIDTH = 0.35;       // player is 0.6 wide, plus a little margin
    private static final int LOOK_AHEAD = 3;

    private static boolean holdingForward = false;
    private static boolean holdingSpace = false;

    private static List<BlockPos> path = null;
    private static int index = 0;

    private static final int STUCK_TICKS = 40;
    private static final int MAX_REPLANS = 3;

    private static int ticksSinceProgress = 0;
    private static int replans = 0;
    private static BlockPos goal = null;

    public static void setPath(List<BlockPos> newPath) {
        path = newPath;
        index = 1;
        goal = newPath.get(newPath.size() - 1);   // the last node is the goal
        ticksSinceProgress = 0;
        replans = 0;
    }

    public static void stop() {
        path = null;
        index = 0;
        releaseForward();
        releaseSpace();
        RotationHandler.clearTarget();
    }

    private static void replan(Minecraft mc) {
        var player = mc.player;
        if (replans >= MAX_REPLANS) {
            System.out.println("Stuck: giving up");
            stop();
            return;
        }
        replans++;
        List<BlockPos> newPath = Pathfinder.aStarLoop(
                mc.level, player.blockPosition(), goal, new Heuristic.Octile());
        if (newPath == null) { stop(); return; }
        path = newPath;
        index = 1;
        ticksSinceProgress = 0;
    }

    private static void releaseForward() {
        if (holdingForward) {
            Minecraft.getInstance().options.keyUp.setDown(false);
            holdingForward = false;
        }
    }

    private static void releaseSpace() {
        if (holdingSpace) {
            Minecraft.getInstance().options.keyJump.setDown(false);
            holdingSpace = false;
        }
    }

    private static double horizDist(LocalPlayer player, BlockPos node) {
        double dx = node.getX() + 0.5 - player.getX();
        double dz = node.getZ() + 0.5 - player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    // move the index past every node we've already reached, even ones we cut past on a smoothed line
    private static void advanceIndex(LocalPlayer player) {
        int last = Math.min(index + MAX_SKIP, path.size() - 2);   // the goal itself uses the stricter check
        for (int j = last; j >= index; j--) {
            BlockPos node = path.get(j);
            if (horizDist(player, node) < NODE_REACHED && Math.abs(player.getY() - node.getY()) < 1.0) {
                index = j + 1;
                ticksSinceProgress = 0;
                break;
            }
        }

        // a smoothed line can pass a node slightly off-centre: count it once we're level with it and past it
        while (index < path.size() - 1) {
            BlockPos node = path.get(index);
            BlockPos next = path.get(index + 1);
            if (node.getY() != next.getY() || Math.abs(player.getY() - node.getY()) > 0.5) break;
            if (horizDist(player, node) > 1.2) break;
            double relX = player.getX() - (node.getX() + 0.5);
            double relZ = player.getZ() - (node.getZ() + 0.5);
            double segX = next.getX() - node.getX();
            double segZ = next.getZ() - node.getZ();
            if (relX * segX + relZ * segZ <= 0) break;     // not past it yet
            index++;
            ticksSinceProgress = 0;
        }
    }

    // can the player walk in a straight line from where they stand to the centre of `to`, all on one level?
    private static boolean straightWalk(Level level, LocalPlayer player, BlockPos to) {
        double sx = player.getX(), sz = player.getZ();
        double ex = to.getX() + 0.5, ez = to.getZ() + 0.5;
        double dx = ex - sx, dz = ez - sz;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-3) return true;

        // sideways unit vector, so we test both shoulders and not just the centre line
        double px = -dz / len * BODY_HALF_WIDTH;
        double pz = dx / len * BODY_HALF_WIDTH;
        int y = to.getY();

        for (double t = 0; t <= len; t += 0.25) {
            double cx = sx + dx * t / len;
            double cz = sz + dz * t / len;
            for (int side = -1; side <= 1; side++) {
                BlockPos p = BlockPos.containing(cx + px * side, y, cz + pz * side);
                if (!Walkability.canStandAt(level, p)) return false;
            }
        }
        return true;
    }

    // the furthest node ahead that we can head straight for: this rounds corners and turns
    // staircase zig-zags into smooth lines
    private static BlockPos pickAimNode(Level level, LocalPlayer player) {
        BlockPos next = path.get(index);
        if (Math.abs(player.getY() - next.getY()) > 0.5 || !player.onGround()) {
            return next;                                    // climbing, dropping, or mid-air: go node by node
        }
        BlockPos best = next;
        int last = Math.min(index + MAX_SKIP, path.size() - 1);
        for (int j = index + 1; j <= last; j++) {
            BlockPos node = path.get(j);
            if (node.getY() != next.getY()) break;          // stop at the next height change
            if (!straightWalk(level, player, node)) break;
            best = node;
        }
        return best;
    }

    public static void register() {
        // the camera turns every rendered frame, so motion is smooth instead of stepping 20 times a second
        LevelRenderEvents.END_MAIN.register(context -> {
            if (path != null) RotationHandler.updateFrame(Minecraft.getInstance().player);
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            var player = mc.player;
            if (player == null || path == null) {
                releaseForward();
                releaseSpace();
                RotationHandler.clearTarget();
                return;
            }

            advanceIndex(player);

            if (index >= path.size() - 1) {
                BlockPos end = path.get(path.size() - 1);
                if (horizDist(player, end) < GOAL_REACHED && Math.abs(player.getY() - end.getY()) < 1.0) {
                    System.out.println("Path finished");
                    stop();
                    return;
                }
                index = path.size() - 1;
            }

            BlockPos target = path.get(index);              // the next node we must actually reach
            BlockPos aim = pickAimNode(mc.level, player);  // where we steer toward
            double aimX = aim.getX() + 0.5;
            double aimZ = aim.getZ() + 0.5;
            double dist = horizDist(player, target);

            // yaw toward the aim point
            float wantedYaw = RotationHandler.yawTo(player.getX(), player.getZ(), aimX, aimZ);

            // pitch: glance a few nodes ahead, a bit down like a person watching their footing
            int lookIndex = Math.min(index + LOOK_AHEAD, path.size() - 1);
            BlockPos lookTarget = path.get(lookIndex);
            double lookDist = horizDist(player, lookTarget);
            double lookDy = lookTarget.getY() - player.getY();
            float wantedPitch = RotationHandler.pitchTo(lookDy, lookDist, 0.5f, 20f) + 8f;
            wantedPitch = Mth.clamp(wantedPitch, -20f, 25f);

            RotationHandler.setTarget(wantedYaw, wantedPitch);

            // walk while roughly facing the way we're going; humans keep moving through gentle turns
            float off = Math.abs(RotationHandler.angleDiff(player.getYRot(), wantedYaw));
            boolean walk = off < WALK_IF_FACING_WITHIN;
            mc.options.keyUp.setDown(walk);
            holdingForward = walk;

            // jump: the next node is higher than our feet, we're on the ground, and close enough to the ledge
            boolean needJump = walk
                    && target.getY() > player.getY() + 0.1
                    && player.onGround()
                    && dist < 1.2;
            mc.options.keyJump.setDown(needJump);
            holdingSpace = needJump;

            ticksSinceProgress++;
            if (ticksSinceProgress > STUCK_TICKS) {
                System.out.println("Replanning (" + (replans + 1) + ")");
                replan(mc);
            }
        });
    }
}
