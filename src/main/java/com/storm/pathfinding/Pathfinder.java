package com.storm.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import static com.storm.pathfinding.Walkability.canStandAt;

public class Pathfinder {

    private static final int MAX_NODES = 5000;

    // which blocks next to pos can be stood on?
    public static List<BlockPos> getNeighbors(Level level, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = pos.relative(dir);
            if (canStandAt(level, candidate)) neighbors.add(candidate);
        }
        return neighbors;
    }

    // the real cost of one step (this is g). Your custom costs go here later.
    private static double stepCost(BlockPos from, BlockPos to) {
        return 1;
    }

    public static List<BlockPos> aStarLoop(Level level, BlockPos start, BlockPos goal, Heuristic heuristic) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        HashMap<BlockPos, PathNode> nodes = new HashMap<>();
        HashSet<BlockPos> closed = new HashSet<>();

        PathNode startNode = new PathNode(0, heuristic.estimate(start, goal), start, null);
        openSet.add(startNode);
        nodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();

            if (current.getPosition().equals(goal)) {
                List<BlockPos> path = new ArrayList<>();
                PathNode node = current;
                while (node != null) {
                    path.add(node.getPosition());
                    node = node.getParent();
                }
                Collections.reverse(path);
                return path;
            }

            if (closed.contains(current.getPosition())) continue;
            closed.add(current.getPosition());
            if (closed.size() > MAX_NODES) return null;

            for (BlockPos neighborPos : getNeighbors(level, current.getPosition())) {
                if (closed.contains(neighborPos)) continue;

                double newG = current.getG() + Costs.stepCost(level, current.getPosition(), neighborPos);
                PathNode existing = nodes.get(neighborPos);

                if (existing == null || newG < existing.getG()) {
                    PathNode neighbor = new PathNode(newG, heuristic.estimate(neighborPos, goal), neighborPos, current);
                    nodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);
                }
            }
        }
        return null;
    }
}