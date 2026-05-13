package com.example.examplemod.AutoWalk;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.stream.Collectors;

public class PathFinder {
    public PathFinder(){
        MinecraftForge.EVENT_BUS.register(this);
    }
    public static LinkedList<BlockPos> findPath(World world, BlockPos start, BlockPos end) {
        if (world == null || start == null || end == null) {
            return new LinkedList<>();
        }

        if (start.equals(end)) {
            return new LinkedList<>();
        }
        // Open Set: 待探索的节点，使用优先队列按 fScore 排序
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));

        // Closed Set: 已探索的节点
        Set<BlockPos> closedSet = new HashSet<>();

        // 记录每个节点的 gScore（从起点到该节点的实际代价）
        Map<BlockPos, Double> gScore = new HashMap<>();

        // 记录每个节点的父节点，用于重建路径
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();

        // 初始化起点
        AStarNode startNode = new AStarNode(start, 0, heuristic(start, end));
        openSet.add(startNode);
        gScore.put(start, 0.0);

        int maxIterations = 10000;
        int iterations = 0;

        while (!openSet.isEmpty() && iterations++ < maxIterations) {
            AStarNode current = openSet.poll();
            BlockPos currentPos = current.pos;

            // 如果到达终点，重建路径
            if (currentPos.equals(end)) {
                return reconstructPath(cameFrom, currentPos);
            }

            // 将当前节点加入已探索集合
            closedSet.add(currentPos);

            // 获取所有可达的邻居节点
            LinkedList<BlockPos> neighbors = getCanReachNearBlocks(world, currentPos);

            for (BlockPos neighbor : neighbors) {
                // 跳过已探索的节点
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                // 检查是否是可站立的方块
                if (!isStandableBlock(world, neighbor)) {
                    continue;
                }

                // 计算 tentative gScore
                double tentativeGScore = gScore.getOrDefault(currentPos, Double.MAX_VALUE) + getNearCost(currentPos, neighbor);

                // 如果找到更好的路径，更新
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, currentPos);
                    gScore.put(neighbor, tentativeGScore);
                    double fScore = tentativeGScore + heuristic(neighbor, end);
                    openSet.add(new AStarNode(neighbor, tentativeGScore, fScore));
                }
            }
        }

        // 找不到路径，返回空列表
        return new LinkedList<>();




        /*LinkedList<Map.Entry<BlockPos, Float>> path = new LinkedList<>();
        Set<BlockPos> errorBlockPos= new HashSet<>();
        Set<BlockPos> visitedBlocks = new HashSet<>();
        BlockPos current=start;
        visitedBlocks.add(start);

        int maxIterations = 10000;
        int iterations = 0;

        while (!current.equals(end)){
            if (++iterations > maxIterations) {
                System.err.println("[PathFinder] 达到最大迭代次数，路径搜索终止");
                break;
            }

            //寻找临近的所有方块坐标
            LinkedList<BlockPos> neighborBlocks=getCanReachNearBlocks(world, current);
            neighborBlocks= neighborBlocks.stream()
                    .filter(neighborBlock -> isStandableBlock(world, neighborBlock))
                    .filter(neighborBlock -> !errorBlockPos.contains(neighborBlock))
                    .filter(neighborBlock -> !visitedBlocks.contains(neighborBlock))
                    .collect(Collectors.toCollection(LinkedList::new));
            //获取最小路径代价的坐标
            BlockPos finalCurrent = current;
            Optional<BlockPos> nextOptional= neighborBlocks.stream().min((o1, o2) -> Float.compare(estimateCost(path, finalCurrent, o1, end), estimateCost(path, finalCurrent, o2, end)));
            //如果找不到下一路径坐标
            if(!nextOptional.isPresent()){
                if(path.isEmpty()){
                    break;
                }
                //往不可用路径列表添加当前坐标，并且从path删除当前坐标，然后继续下一循环
                errorBlockPos.add(current);
                path.removeLast();
                current=path.getLast().getKey();
                continue;
            }
            BlockPos next=nextOptional.get();
            path.add(new AbstractMap.SimpleEntry<>(next, getNearCost(current, next)));
            visitedBlocks.add(next);
            current=next;
        }
        LinkedList<BlockPos> ret=new LinkedList<>();
        for (Map.Entry<BlockPos, Float> m : path){
            ret.add(m.getKey());
        }
        return ret;*/
    }
    public static LinkedList<BlockPos> getYawNeighborBlockPos(BlockPos current){
        LinkedList<BlockPos> neighborBlocks=new LinkedList<>();
        neighborBlocks.add(current.east());
        neighborBlocks.add(current.north());
        neighborBlocks.add(current.west());
        neighborBlocks.add(current.south());
        neighborBlocks.add(current.east().north());
        neighborBlocks.add(current.east().south());
        neighborBlocks.add(current.west().north());
        neighborBlocks.add(current.west().south());
        return neighborBlocks;
    }
    public static float estimateCost(LinkedList<Map.Entry<BlockPos, Float>> currentPath, BlockPos currentPos, BlockPos blockPos, BlockPos end){
        float cost=0;
        for (Map.Entry<BlockPos, Float> m:currentPath){
            cost+=m.getValue();
        }
        cost+= (float) blockPos.distanceSq(end);
        cost+= getNearCost(currentPos, blockPos);
        return cost;
    }
    public static LinkedList<BlockPos> getCanReachNearBlocks(World world, BlockPos current){
        LinkedList<BlockPos> neighborBlocks=new LinkedList<>();
        neighborBlocks.addAll(getYawNeighborBlockPos(current));
        if(world.getBlockState(current.up(3)).getBlock()==Blocks.air){
            neighborBlocks.addAll(getYawNeighborBlockPos(current.up()));
        }
        BlockPos up2=current.up().up();
        BlockPos eastUp2=up2.east();
        if(world.getBlockState(eastUp2).getBlock()==Blocks.air){
            neighborBlocks.add(current.east().down());
            neighborBlocks.add(current.east().down().down());
        }
        BlockPos westUp2=up2.west();
        if(world.getBlockState(westUp2).getBlock()==Blocks.air){
            neighborBlocks.add(current.west().down());
            neighborBlocks.add(current.west().down().down());
        }
        BlockPos northUp2=up2.north();
        if(world.getBlockState(northUp2).getBlock()==Blocks.air){
            neighborBlocks.add(current.north().down());
            neighborBlocks.add(current.north().down().down());
        }
        BlockPos southUp2=up2.south();
        if(world.getBlockState(southUp2).getBlock()==Blocks.air){
            neighborBlocks.add(current.south().down());
            neighborBlocks.add(current.south().down().down());
        }
        if(world.getBlockState(westUp2).getBlock()==Blocks.air||world.getBlockState(northUp2).getBlock()==Blocks.air){
            neighborBlocks.add(current.north().west().down());
            neighborBlocks.add(current.north().west().down().down());
        }
        if(world.getBlockState(westUp2).getBlock()==Blocks.air||world.getBlockState(southUp2).getBlock()==Blocks.air){
            neighborBlocks.add(current.south().west().down());
            neighborBlocks.add(current.south().west().down().down());
        }
        if(world.getBlockState(eastUp2).getBlock()==Blocks.air||world.getBlockState(southUp2).getBlock()==Blocks.air){
            neighborBlocks.add(current.south().east().down());
            neighborBlocks.add(current.south().east().down().down());
        }
        if(world.getBlockState(eastUp2).getBlock()==Blocks.air||world.getBlockState(northUp2).getBlock()==Blocks.air){
            neighborBlocks.add(current.north().east().down());
            neighborBlocks.add(current.north().east().down().down());
        }
        return neighborBlocks;
    }
    public static boolean isStandableBlock(World world, BlockPos blockPos){
        Block block=world.getBlockState(blockPos).getBlock();
        if (block==Blocks.water
                ||block==Blocks.cactus
                ||block==Blocks.tallgrass
                ||block==Blocks.flowing_water
                ||block==Blocks.air
                ||block==Blocks.lava
                ||block==Blocks.flowing_lava
                ||block==Blocks.web
                ||block==Blocks.red_flower
                ||block==Blocks.yellow_flower) {
            return false;
        }
        return world.getBlockState(blockPos.up()).getBlock() == Blocks.air
                && world.getBlockState(blockPos.up().up()).getBlock() == Blocks.air;
    }
    public static float getNearCost(BlockPos start, BlockPos near){
        //如果不是包括对角线这种相邻方块就返回最大损失
        if(Math.abs(start.getX()-near.getX())>1||Math.abs(start.getZ()-near.getZ())>1){
            return Float.MAX_VALUE;
        }
        float ret=0;
        //计算高度损失
        float yDiff=near.getY()-start.getY();
        if(yDiff>1){
            return Float.MAX_VALUE;
        }else if(yDiff==1){
            ++ret;
        }else{
            ret-=yDiff;
        }
        //计算横向损失
        if(Math.abs(start.getX()-near.getX())>1&&Math.abs(start.getZ()-near.getZ())>1){
            ret+=1.414213562373095f;
        }else{
            ret+=1;
        }
        return ret;
    }
    /**
     * A* 算法的节点类
     */
    private static class AStarNode {
        BlockPos pos;
        double gScore;  // 从起点到该节点的实际代价
        double fScore;  // fScore = gScore + hScore（启发式估计）

        AStarNode(BlockPos pos, double gScore, double fScore) {
            this.pos = pos;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    /**
     * 启发式函数：使用欧几里得距离的平方
     */
    private static double heuristic(BlockPos from, BlockPos to) {
        return from.distanceSq(to);
    }

    /**
     * 根据 cameFrom 映射重建路径
     */
    private static LinkedList<BlockPos> reconstructPath(Map<BlockPos, BlockPos> cameFrom, BlockPos current) {
        LinkedList<BlockPos> path = new LinkedList<>();
        path.addFirst(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.addFirst(current);
        }

        return path;
    }
}
