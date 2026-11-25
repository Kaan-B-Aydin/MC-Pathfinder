package com.storm.pathfinding;

import net.minecraft.core.BlockPos;


public class PathNode implements Comparable<PathNode> {
    private final BlockPos position;
    private double g;
    private double h;
    private PathNode parent;

    public PathNode(double cost, double heuristic,BlockPos pos, PathNode node){
        g = cost;
        h = heuristic;
        position = pos;
        parent = node;
    }

    public double f() {
        return g + h;
    }

    public double getG(){
        return g;
    }

    public void setG(double g){
        this.g = g;
    }

    public double getH(){
        return h;
    }

    public BlockPos getPosition(){
        return position;
    }

    public PathNode getParent(){
        return parent;
    }

    public void setParent(PathNode parent){
        this.parent = parent;
    }

    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.f(), other.f());
    }
}


