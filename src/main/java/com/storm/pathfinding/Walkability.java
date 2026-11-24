package com.storm.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Walkability {
    public static boolean canStandAt(Level level, BlockPos pos){
        // positions
        BlockPos floorPos = pos.below();
        BlockPos headPos  = pos.above();

// block states
        BlockState floorState = level.getBlockState(floorPos);
        BlockState feetState  = level.getBlockState(pos);
        BlockState headState  = level.getBlockState(headPos);

// collision shapes
        VoxelShape floorShape = floorState.getCollisionShape(level, floorPos);
        VoxelShape feetShape  = feetState.getCollisionShape(level, pos);
        VoxelShape headShape  = headState.getCollisionShape(level, headPos);

// lava (keep these for when you allow water later)
        boolean feetLava = feetState.is(Blocks.LAVA);
        boolean headLava = headState.is(Blocks.LAVA);

        if (floorShape.isEmpty() || !feetShape.isEmpty() || !headShape.isEmpty() || feetLava || headLava){
            return false;
        }
    return true;
    }

}
