package com.storm;

import com.storm.commands.PathCommand;
import net.fabricmc.api.ClientModInitializer;

public class StormClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("StormClient loaded");

        PathCommand.register();
    }
}