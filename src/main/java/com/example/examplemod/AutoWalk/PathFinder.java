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
        path.add(new PathNode(start, 0, 0, getCost(start, end)));
        BlockPos current=start;
        while(!current.equals(end)){
            visitedPos.add(current);
            HashSet<BlockPos> neighbors=getNeighbors(current);
            BlockPos finalCurrent = current;
            neighbors=neighbors.stream()
                    .filter(blockPos -> canReachBlock(world, finalCurrent, blockPos))
                    .filter(blockPos -> !visitedPos.contains(blockPos))
                    .collect(Collectors.toCollection(HashSet::new));
            if(neighbors.isEmpty()){
                if(path.isEmpty()){
                    break;
                }
                visitedPos.remove(path.getLast().pos);
                path.removeLast();
                current=path.getLast().pos;
                continue;
            }
            BlockPos next= neighbors.stream().min(Comparator.comparingDouble(pos -> pos.distanceSq(end))).get();
            path.add(new PathNode(next, getCost(start, next),getCost(current, next), getCost(next, end)));
            current=next;
        }

        return recConstructionPath(path);
    }
    public static boolean canReachBlock(World world, BlockPos start, BlockPos near){
        if(world.getBlockState(near).getBlock()!= Blocks.air
                ||world.getBlockState(near.up()).getBlock()!= Blocks.air){
            return false;
        }
        if(start.getY()==near.getY()){
            if(start.distanceSq(near)>1.2){
                //对角线方块
                int[] sub ={near.getX()-start.getX(), near.getZ()-start.getZ()};
                BlockPos xSub=new BlockPos(start.getX()+sub[0], start.getY(), start.getZ());
                BlockPos zSub=new BlockPos(start.getX(), start.getY(), start.getZ()+sub[1]);
                if(world.getBlockState(xSub).getBlock()==Blocks.air
                        && world.getBlockState(xSub.up()).getBlock()==Blocks.air){
                    return true;
                }
                return world.getBlockState(zSub).getBlock() == Blocks.air
                        && world.getBlockState(zSub.up()).getBlock() == Blocks.air;
            }else{
                return world.getBlockState(near).getBlock() == Blocks.air
                        && world.getBlockState(near.up()).getBlock() == Blocks.air;
            }
        }else{
            if(start.getY()<near.getY()){
                if(world.getBlockState(start.up()).getBlock()!=Blocks.air){
                    return false;
                }

                if(getHorizontalDistance(start, near)>1.2){
                    //对角线方块的上方一格
                    if(world.getBlockState(near).getBlock()!=Blocks.air
                    ||world.getBlockState(near.up()).getBlock()!=Blocks.air){
                        return false;
                    }
                    int[] sub ={near.getX()-start.getX(), near.getZ()-start.getZ()};
                    BlockPos xSub=new BlockPos(start.getX()+sub[0], start.getY()+1, start.getZ());
                    BlockPos zSub=new BlockPos(start.getX(), start.getY()+1, start.getZ()+sub[1]);
                    if(world.getBlockState(xSub).getBlock()==Blocks.air
                            && world.getBlockState(xSub.up()).getBlock()==Blocks.air){
                        return true;
                    }
                    if(world.getBlockState(zSub).getBlock()==Blocks.air
                            && world.getBlockState(zSub.up()).getBlock()==Blocks.air){
                        return true;
                    }
                    return world.getBlockState(near).getBlock() == Blocks.air
                            && world.getBlockState(near.up()).getBlock() == Blocks.air;
                }else{
                    return world.getBlockState(near).getBlock() == Blocks.air
                            && world.getBlockState(near.up()).getBlock() == Blocks.air;
                }
            }else{
                //低于两格以内的
                if(getHorizontalDistance(start, near)>1.2){
                    int[] sub ={near.getX()-start.getX(), near.getZ()-start.getZ()};
                    BlockPos xSub=new BlockPos(start.getX()+sub[0], start.getY(), start.getZ());
                    BlockPos zSub=new BlockPos(start.getX(), start.getY(), start.getZ()+sub[1]);
                    if(world.getBlockState(xSub).getBlock()==Blocks.air
                            && world.getBlockState(xSub.up()).getBlock()==Blocks.air){
                        return true;
                    }
                    if(world.getBlockState(zSub).getBlock()==Blocks.air
                            && world.getBlockState(zSub.up()).getBlock()==Blocks.air){
                        return true;
                    }
                    return world.getBlockState(near).getBlock() == Blocks.air
                            && world.getBlockState(near.up()).getBlock() == Blocks.air;
                }else{
                    int[] sub ={near.getX()-start.getX(), near.getZ()-start.getZ()};
                    BlockPos xSub=new BlockPos(start.getX()+sub[0], start.getY(), start.getZ());
                    BlockPos zSub=new BlockPos(start.getX(), start.getY(), start.getZ()+sub[1]);
                    if(world.getBlockState(xSub).getBlock()==Blocks.air
                            && world.getBlockState(xSub.up()).getBlock()==Blocks.air){
                        return true;
                    }
                    if(world.getBlockState(zSub).getBlock()==Blocks.air
                            && world.getBlockState(zSub.up()).getBlock()==Blocks.air){
                        return true;
                    }
                    return world.getBlockState(near).getBlock() == Blocks.air
                            && world.getBlockState(near.up()).getBlock() == Blocks.air;
                }
            }
        }
    }
    public static HashSet<BlockPos> getNeighbors(BlockPos pos){
        HashSet<BlockPos> ret=new HashSet<>();
        ret.addAll(getYawNeighbors(pos.up(1)));
        ret.addAll(getYawNeighbors(pos));
        ret.addAll(getYawNeighbors(pos.down(1)));
        ret.addAll(getYawNeighbors(pos.down(2)));
        return ret;
    }
    public static HashSet<BlockPos> getYawNeighbors(BlockPos pos){
        HashSet<BlockPos> ret=new HashSet<>();
        ret.add(pos.east());
        ret.add(pos.west());
        ret.add(pos.north());
        ret.add(pos.south());
        ret.add(pos.east().north());
        ret.add(pos.east().south());
        ret.add(pos.west().north());
        ret.add(pos.west().south());
        return ret;
    }
    public static float getCost(BlockPos current, BlockPos end){
        return (float) current.distanceSq(end);
    }
    public static LinkedList<BlockPos> recConstructionPath(LinkedList<PathNode> path){
        LinkedList<BlockPos> ret=new LinkedList<>();
        for (PathNode node:path){
            ret.add(node.pos);
        }
        return ret;
    }
    public static class PathNode{
        BlockPos pos;
        float fCost, cost, endCost;
        public PathNode(BlockPos pos,float fCost, float cost, float endCost){
            this.pos=pos;
            this.cost=cost;
            this.endCost=endCost;
            this.fCost=fCost;
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
