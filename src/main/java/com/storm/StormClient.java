package com.storm;

import com.storm.pathfinding.Heuristic;
import com.storm.pathfinding.Pathfinder;
import com.storm.pathfinding.Walkability;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import static com.storm.pathfinding.Pathfinder.aStarLoop;
import static com.storm.pathfinding.Walkability.canStandAt;

public class StormClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("StormClient loaded");

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            var player = mc.player;
            var level = mc.level;
            if (player == null || level == null) return;
            if (player.tickCount % 40 != 0) return;

            var feet = player.blockPosition();

            var path = Pathfinder.aStarLoop(level, feet, feet.north(5), new Heuristic.Manhattan());
            System.out.println("Manhattan: " + (path == null ? "null" : path.size()));

            var path2 = Pathfinder.aStarLoop(level, feet, feet.north(5), new Heuristic.Euclidean());
            System.out.println("Euclidean: " + (path2 == null ? "null" : path2.size()));
        });
    }
}