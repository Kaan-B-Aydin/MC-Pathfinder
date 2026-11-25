package com.storm.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Draws the current path as block overlays. Toggle with /storm debug.
// Uses Minecraft's gizmo system: gizmos added during a client tick are drawn until the next tick,
// so we just re-add them every tick.
public class PathDebug {

    public static boolean enabled = false;

    private static final int SHOW_AHEAD = 64;     // nodes drawn ahead of the follower
    private static final int SHOW_BEHIND = 6;     // already-walked nodes, faded

    // colours are ARGB
    private static final int PATH = 0xFF3FD06A;      // green: path ahead
    private static final int PASSED = 0xFF7A7A7A;    // grey: already walked
    private static final int NEXT = 0xFFFFD23F;      // yellow: node we must reach next
    private static final int AIM = 0xFF3FC8FF;       // cyan: point we steer toward
    private static final int GOAL = 0xFFFF4D4D;      // red: destination

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    // a highlighted block: translucent fill with a solid outline
    private static void block(BlockPos pos, int color, int fillAlpha) {
        Gizmos.cuboid(pos, 0.002f, GizmoStyle.strokeAndFill(color, 2f, withAlpha(color, fillAlpha)));
    }

    private static Vec3 centre(BlockPos node) {
        return new Vec3(node.getX() + 0.5, node.getY() + 0.05, node.getZ() + 0.5);
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!enabled || mc.player == null) return;
            List<BlockPos> path = PathFollower.getPath();
            if (path == null || path.isEmpty()) return;

            int index = Math.min(PathFollower.getIndex(), path.size() - 1);
            int from = Math.max(0, index - SHOW_BEHIND);
            int to = Math.min(path.size() - 1, index + SHOW_AHEAD);

            // the floor block under each node, plus a line along the route
            for (int i = from; i <= to; i++) {
                BlockPos node = path.get(i);
                boolean passed = i < index;
                block(node.below(), passed ? PASSED : PATH, passed ? 0x22 : 0x40);
                if (i < to) {
                    Gizmos.line(centre(node), centre(path.get(i + 1)), passed ? PASSED : PATH, 2f);
                }
            }

            BlockPos goal = path.get(path.size() - 1);
            block(goal.below(), GOAL, 0x60);
            block(path.get(index).below(), NEXT, 0x70);

            BlockPos aim = PathFollower.getAim();
            if (aim != null) {
                block(aim.below(), AIM, 0x70);
                Vec3 feet = mc.player.position().add(0, 0.05, 0);
                Gizmos.line(feet, centre(aim), AIM, 3f);       // the line the player is actually steering along
            }

            mc.player.sendOverlayMessage(Component.literal(
                    "node " + index + "/" + (path.size() - 1)
                            + "   aim " + (aim == null ? "-" : aim.toShortString())
                            + "   goal " + goal.toShortString()));
        });
    }
}
