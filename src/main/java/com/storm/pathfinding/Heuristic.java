package com.storm.pathfinding;

import net.minecraft.core.BlockPos;

public interface Heuristic {

    // the promise: every heuristic must be able to do this
    double estimate(BlockPos from, BlockPos goal);

    class Manhattan implements Heuristic {
        @Override
        public double estimate(BlockPos from, BlockPos goal) {
            return Math.abs(from.getX() - goal.getX())
                    + Math.abs(from.getY() - goal.getY())
                    + Math.abs(from.getZ() - goal.getZ());
        }
    }

    class Euclidean implements Heuristic {
        @Override
        public double estimate(BlockPos from, BlockPos goal) {
            double dx = from.getX() - goal.getX();
            double dy = from.getY() - goal.getY();
            double dz = from.getZ() - goal.getZ();
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
}