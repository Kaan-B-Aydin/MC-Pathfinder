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


    private static final int MAX_DROP = 40;

    // the four diagonal offsets (dx, dz)
    private static final int[][] DIAGONALS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    public static List<BlockPos> getNeighbors(Level level, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();

        // straight moves: walk, jump up one, or drop down
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            addColumn(level, pos, pos.relative(dir), true, neighbors);
        }

        // diagonal moves: walk or drop (no diagonal jumps), and never cut a corner
        for (int[] d : DIAGONALS) {
            BlockPos sideA = pos.offset(d[0], 0, 0);
            BlockPos sideB = pos.offset(0, 0, d[1]);
            if (!isFree(level, sideA) || !isFree(level, sideA.above())
                    || !isFree(level, sideB) || !isFree(level, sideB.above())) {
                continue;                                               // a wall on either side blocks the diagonal
            }
            addColumn(level, pos, pos.offset(d[0], 0, d[1]), false, neighbors);
        }
        return neighbors;
    }

    // scan one neighbor column for the first place we can stand (jump up one, same level, or the first landing below)
    private static void addColumn(Level level, BlockPos pos, BlockPos column, boolean allowJump, List<BlockPos> out) {
        for (int dy = allowJump ? 1 : 0; dy >= -MAX_DROP; dy--) {
            BlockPos candidate = column.above(dy);

            if (dy == 1 && !isFree(level, pos.above(2))) continue;           // no headroom to jump

            if (dy < 0 && !columnFree(level, column.above(dy + 2), column.above(1))) {
                break;                                                       // blocked on the way down
            }

            if (canStandAt(level, candidate)) {
                out.add(candidate);
                if (dy <= 0) break;                                          // first landing: can't fall past it
            }
        }
    }

    private static boolean isFree(Level level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }

    // are all blocks from bottom up to top (inclusive, same x/z) free?
    private static boolean columnFree(Level level, BlockPos bottom, BlockPos top) {
        for (int y = bottom.getY(); y <= top.getY(); y++) {
            if (!isFree(level, new BlockPos(bottom.getX(), y, bottom.getZ()))) return false;
        }
        return true;
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