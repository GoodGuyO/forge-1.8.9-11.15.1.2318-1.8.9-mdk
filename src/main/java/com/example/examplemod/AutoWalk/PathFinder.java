package com.example.examplemod.AutoWalk;

import net.minecraft.entity.Entity;
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
        if (!isWalkable(world, end)) {
            return new LinkedList<>();
        }

        if (start.equals(end)) {
            LinkedList<BlockPos> path = new LinkedList<>();
            path.add(start);
            return path;
        }

        if (!isStandable(world, start)) {
            return new LinkedList<>();
        }

        PriorityQueue<PathNode> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        HashSet<BlockPos> openSet = new HashSet<>();
        HashMap<BlockPos, PathNode> allNodes = new HashMap<>();
        HashSet<BlockPos> closedSet = new HashSet<>();

        PathNode startNode = new PathNode(start, 0, getHeuristic(start, end));
        openList.add(startNode);
        openSet.add(start);
        allNodes.put(start, startNode);

        int maxIterations = 200000;
        int iterations = 0;

        while (!openList.isEmpty()) {
            iterations++;

            if (iterations > maxIterations) {
                break;
            }

            PathNode current = openList.poll();
            openSet.remove(current.pos);

            if (current.pos.equals(end)) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            HashSet<BlockPos> neighbors = getNeighbors(world, current.pos);

            for (BlockPos neighborPos : neighbors) {
                if (closedSet.contains(neighborPos)) {
                    continue;
                }

                double tentativeGCost = current.gCost + getMovementCost(current.pos, neighborPos);

                PathNode neighborNode = allNodes.get(neighborPos);
                boolean isNewPath = false;

                if (neighborNode == null) {
                    neighborNode = new PathNode(neighborPos, Double.MAX_VALUE, getHeuristic(neighborPos, end));
                    allNodes.put(neighborPos, neighborNode);
                    isNewPath = true;
                } else if (tentativeGCost < neighborNode.gCost) {
                    isNewPath = true;
                }

                if (isNewPath) {
                    neighborNode.gCost = tentativeGCost;
                    neighborNode.fCost = tentativeGCost + neighborNode.hCost;
                    neighborNode.parent = current;

                    if (!openSet.contains(neighborPos)) {
                        openList.add(neighborNode);
                        openSet.add(neighborPos);
                    }
                }
            }
        }

        return new LinkedList<>();
    }
    public static HashSet<BlockPos> getNeighbors(World world, BlockPos pos){
        HashSet<BlockPos> ret = new HashSet<>();

        // 四个基本方向（东西南北）
        BlockPos[] directions = {
                pos.east(), pos.west(), pos.north(), pos.south()
        };

        // 四个对角线方向（东北、西北、东南、西南）
        BlockPos[] diagonals = {
                pos.east().north(), pos.east().south(),
                pos.west().north(), pos.west().south()
        };

        BlockPos[] allDirections = {
                pos.east(), pos.west(), pos.north(), pos.south(),
                pos.east().north(), pos.east().south(),
                pos.west().north(), pos.west().south()
        };
        for (BlockPos neighbor : directions) {
            if(isWalkable(world, neighbor.up())&&isCollisionBlock(world, pos.down())&&isStandable(world, pos.up())){
                ret.add(pos.up());
                break;
            }
        }
        if(isStandable(world, pos.down())){
            ret.add(pos.down());
        }

        // 处理基本方向
        for (BlockPos neighbor : directions) {
            // 检查同一高度是否可行走（需要有支撑）
            if (isWalkable(world, neighbor)) {
                ret.add(neighbor);
            }
        }

        // 处理对角线方向
        for (BlockPos diagonal : diagonals) {
            // 对角线移动需要检查两个相邻的基本方向是否都可行走
            BlockPos adj1 = null;
            BlockPos adj2 = null;

            // 确定对角线的两个相邻基本方向
            if (diagonal.equals(pos.east().north())) {
                adj1 = pos.east();
                adj2 = pos.north();
            } else if (diagonal.equals(pos.east().south())) {
                adj1 = pos.east();
                adj2 = pos.south();
            } else if (diagonal.equals(pos.west().north())) {
                adj1 = pos.west();
                adj2 = pos.north();
            } else if (diagonal.equals(pos.west().south())) {
                adj1 = pos.west();
                adj2 = pos.south();
            }

            // 只有当两个相邻方向都可行走时，才允许对角线移动
            if (adj1 != null && adj2 != null &&
                    isWalkable(world, adj1) && isWalkable(world, adj2) &&
                    isWalkable(world, diagonal)) {
                ret.add(diagonal);
            }
        }

        return ret;
    }
    public static boolean isCollisionBlock(World world ,BlockPos pos){
        if(world.getBlockState(pos).getBlock()==Blocks.air
        ||world.getBlockState(pos).getBlock().getCollisionBoundingBox(world, pos, world.getBlockState(pos))==null){
            return false;
        }else{
            return true;
        }
    }
    public static boolean isStandableBlock(World world, BlockPos pos){
        return !isCollisionBlock(world, pos);
    }
    /**
     * 检查位置是否可以站立（需要有2格高空间）
     */
    public static boolean isStandable(World world, BlockPos pos){
        // 检查当前位置是否为空气
        if(!isStandableBlock(world, pos)){
            return false;
        }

        // 检查上方是否为空气（玩家需要2格高的空间）
        return isStandableBlock(world, pos.up());
    }

    /**
     * 检查位置是否可行走（isStandable + 下方有支撑方块）
     */
    public static boolean isWalkable(World world, BlockPos pos){
        // 首先检查是否有足够的站立空间
        if (!isStandable(world, pos)) {
            return false;
        }

        // 检查下方是否有实体方块支撑
        BlockPos below = pos.down();
        return isCollisionBlock(world, below);
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
    /**
     * 移动代价：计算从一个方块移动到另一个方块的代价
     */
    public static double getMovementCost(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dz = Math.abs(from.getZ() - to.getZ());

        double baseCost;
        // 对角线移动（dx=1 且 dz=1）的代价是 √2 ≈ 1.414
        if (dx == 1 && dz == 1) {
            baseCost = 1.414;
        } else {
            // 直线移动代价为 1.0
            baseCost = 1.0;
        }

        // 垂直移动额外增加代价
        if (from.getY() != to.getY()) {
            baseCost += 0.5;
        }

        return baseCost;
    }
    /**
     * 重建路径：从终点回溯到起点
     */
    public static LinkedList<BlockPos> reconstructPath(PathNode endNode) {
        LinkedList<BlockPos> path = new LinkedList<>();
        PathNode current = endNode;

        while (current != null) {
            path.addFirst(current.pos);
            current = current.parent;
        }

        return path;
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
}
