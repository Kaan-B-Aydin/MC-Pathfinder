package com.storm.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class Costs {

    // ---- constants: all the numbers live here ----
    public static final double WALK = 1.0;
    public static final double WATER_PENALTY = 3.3;          // water step costs 1 + 3.3 = 4.3
    public static final double LAVA_NEIGHBOR_PENALTY = 20.0;
    public static final double JUMP_UP = 2.0;                // unused until you add step-up moves

    // ---- the cost of one step ----
    public static double stepCost(Level level, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        double cost = WALK * Math.sqrt(dx * dx + dz * dz);    // 1 for straight, ~1.41 for diagonal

        // rule: water at the destination
        if (level.getFluidState(to).is(FluidTags.WATER)) {
            cost += WATER_PENALTY;
        }

        // rule: lava beside the destination
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(to.relative(dir)).is(Blocks.LAVA)) {
                cost += LAVA_NEIGHBOR_PENALTY;
            }
        }

        return cost;
    }
}