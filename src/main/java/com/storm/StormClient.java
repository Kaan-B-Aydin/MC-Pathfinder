package com.storm;

import com.storm.commands.PathCommand;
import com.storm.movement.PathFollower;
import com.storm.util.RotationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.world.level.block.Rotation;

public class StormClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("StormClient loaded");

        PathCommand.register();
        PathFollower.register();
        System.out.println(RotationHandler.yawTo(0, 0, 0, 5));    // expect 0
        System.out.println(RotationHandler.yawTo(0, 0, 5, 0));    // expect -90
        System.out.println(RotationHandler.angleDiff(170, -170)); // expect 20
        System.out.println(RotationHandler.stepToward(0, 90, 15)); // expect 15
    }
}