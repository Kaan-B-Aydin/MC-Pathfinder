package com.storm.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.storm.movement.PathFollower;
import com.storm.pathfinding.Heuristic;
import com.storm.pathfinding.Pathfinder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PathCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(
                    ClientCommands.literal("storm")
                            .then(ClientCommands.literal("goto")
                                    .then(ClientCommands.argument("x", IntegerArgumentType.integer())
                                            .then(ClientCommands.argument("y", IntegerArgumentType.integer())
                                                    .then(ClientCommands.argument("z", IntegerArgumentType.integer())
                                                            .executes(context -> {
                                                                int x = IntegerArgumentType.getInteger(context, "x");
                                                                int y = IntegerArgumentType.getInteger(context, "y");
                                                                int z = IntegerArgumentType.getInteger(context, "z");
                                                                BlockPos goal = new BlockPos(x, y, z);

                                                                var source = context.getSource();
                                                                var player = source.getPlayer();
                                                                var level = source.getLevel();

                                                                List<BlockPos> path = Pathfinder.aStarLoop(
                                                                        level, player.blockPosition(), goal, new Heuristic.Manhattan());

                                                                if (path == null) {
                                                                    source.sendError(Component.literal("No path found"));
                                                                    return 0;
                                                                }
                                                                source.sendFeedback(Component.literal("Path found: " + path.size() + " blocks"));
                                                                PathFollower.setPath(path);
                                                                return 1;
                                                            })
                                                    )
                                            )
                                    )
                            )
            );
        });
    }
}