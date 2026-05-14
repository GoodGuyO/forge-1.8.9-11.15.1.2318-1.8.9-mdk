package com.example.examplemod.AutoWalk;

import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;
import java.util.stream.Collectors;

public class PathFinder {
    public PathFinder(){
        MinecraftForge.EVENT_BUS.register(this);
    }
    public static LinkedList<BlockPos> findPath(World world, BlockPos start, BlockPos end) {
        LinkedList<PathNode> path = new LinkedList<>();
        HashSet<BlockPos> visitedPos=new HashSet<>();
        path.add(new PathNode(start, 0, getHeuristic(start, end)));
        BlockPos current=start;
        int maxIterations = 100; // 防止死循环的最大迭代次数
        int iterations = 0;
        while(!current.equals(end) && iterations < maxIterations){
            iterations++;
            visitedPos.add(current);
            HashSet<BlockPos> neighbors=getNeighbors(world, current);
            neighbors=neighbors.stream()
                    .filter(blockPos -> {
                        for (BlockPos pos : visitedPos) {
                            if(blockPos.equals(pos)){
                                return false;
                            }
                         }
                        return true;
                    })
                    .collect(Collectors.toCollection(HashSet::new));
            if(neighbors.isEmpty()){
                path.removeLast();
                if(path.isEmpty()){
                    break;
                }
                current=path.getLast().pos;
                continue;
            }
            BlockPos next= neighbors.stream().min(Comparator.comparingDouble(pos -> )).get();
            path.add(new PathNode(next, ));
            current=next;
        }

        return recConstructionPath(path);
    }
    public static HashSet<BlockPos> getNeighbors(World world, BlockPos pos){
        HashSet<BlockPos> ret=new HashSet<>();
        if(isStandable(world, pos.up())){
            BlockPos up=pos.up();
            //平面
            if (isStandable(world, up.east())) {
                ret.add(up.east());
                ret.add(up.east().north());
                ret.add(up.east().south());
            }
            if (isStandable(world, up.west())) {
                ret.add(up.west());
                ret.add(up.west().north());
                ret.add(up.west().south());
            }
            if (isStandable(world, up.north())) {
                ret.add(up.north());
                ret.add(up.north().west());
                ret.add(up.north().east());
            }
            if (isStandable(world, up.south())) {
                ret.add(up.south());
                ret.add(up.south().west());
                ret.add(up.south().east());
            }
        }
        //平面
        if (isStandable(world, pos.east())) {
            ret.add(pos.east());
            ret.add(pos.east().north());
            ret.add(pos.east().south());

            ret.add(pos.east().down());
            ret.add(pos.east().north().down());
            ret.add(pos.east().south().down());

            ret.add(pos.east().down(2));
            ret.add(pos.east().north().down(2));
            ret.add(pos.east().south().down(2));
        }
        if (isStandable(world, pos.west())) {
            ret.add(pos.west());
            ret.add(pos.west().north());
            ret.add(pos.west().south());

            ret.add(pos.west().down());
            ret.add(pos.west().north().down());
            ret.add(pos.west().south().down());

            ret.add(pos.west().down(2));
            ret.add(pos.west().north().down(2));
            ret.add(pos.west().south().down(2));
        }
        if (isStandable(world, pos.north())) {
            ret.add(pos.north());
            ret.add(pos.north().west());
            ret.add(pos.north().east());

            ret.add(pos.north().down());
            ret.add(pos.north().west().down());
            ret.add(pos.north().east().down());

            ret.add(pos.north().down(2));
            ret.add(pos.north().west().down(2));
            ret.add(pos.north().east().down(2));
        }
        if (isStandable(world, pos.south())) {
            ret.add(pos.south());
            ret.add(pos.south().west());
            ret.add(pos.south().east());

            ret.add(pos.south().down());
            ret.add(pos.south().west().down());
            ret.add(pos.south().east().down());

            ret.add(pos.south().down(2));
            ret.add(pos.south().west().down(2));
            ret.add(pos.south().east().down(2));
        }
        return ret;
    }
    public static boolean isStandable(World world, BlockPos pos){
        return world.getBlockState(pos).getBlock()== Blocks.air
                && world.getBlockState(pos.up()).getBlock()== Blocks.air;
    }
    /**
     * 启发式函数：曼哈顿距离（适用于网格移动）,ai生成
     */
    public static double getHeuristic(BlockPos current, BlockPos end) {
        int dx = Math.abs(current.getX() - end.getX());
        int dy = Math.abs(current.getY() - end.getY());
        int dz = Math.abs(current.getZ() - end.getZ());
        // 给予垂直移动更高的代价，让算法优先水平移动
        return dx + dz + dy * 1.5;
    }
    public static LinkedList<BlockPos> recConstructionPath(LinkedList<PathNode> path){
        LinkedList<BlockPos> ret=new LinkedList<>();
        for (PathNode node:path){
            ret.add(node.pos);
        }
        return ret;
    }
    public static class PathNode {
        BlockPos pos;
        double gCost;  // 从起点到当前节点的实际代价
        double hCost;  // 从当前节点到终点的启发式估计代价
        double fCost;  // gCost + hCost
        PathNode parent;  // 父节点，用于重建路径

        public PathNode(BlockPos pos, double gCost, double hCost) {
            this.pos = pos;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
            this.parent = null;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PathNode pathNode = (PathNode) obj;
            return pos.equals(pathNode.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }
    /**
     * 计算两个 BlockPos 之间的横向距离（XZ平面距离，忽略Y轴）,ai生成
     *
     * @param pos1 第一个方块位置
     * @param pos2 第二个方块位置
     * @return 两个位置之间的横向距离
     */
    public static double getHorizontalDistance(BlockPos pos1, BlockPos pos2) {
        double deltaX = pos2.getX() - pos1.getX();
        double deltaZ = pos2.getZ() - pos1.getZ();
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
}
