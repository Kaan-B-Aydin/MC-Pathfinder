package com.storm;

import com.storm.commands.PathCommand;
import com.storm.util.PathDebug;
import com.storm.util.PathFollower;
import com.storm.util.RotationHandler;
import com.storm.util.keybinds;
import net.fabricmc.api.ClientModInitializer;

public class StormClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("StormClient loaded");

        PathCommand.register();
        PathFollower.register();
        PathDebug.register();
        keybinds.register();
        System.out.println(RotationHandler.yawTo(0, 0, 0, 5));    // expect 0
        System.out.println(RotationHandler.yawTo(0, 0, 5, 0));    // expect -90
        System.out.println(RotationHandler.angleDiff(170, -170)); // expect 20
        System.out.println(RotationHandler.stepToward(0, 90, 15)); // expect 15
    }
}