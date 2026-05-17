package com.example.examplemod.AutoWalk;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedList;

public class AutoWalker {
    public static AutoWalker autoWalker=null;
    boolean isWalking=false;
    boolean needsJump=false;
    LinkedList<BlockPos> currentPath=null;
    public BlockPos target=null;

    // 追随玩家相关字段
    boolean isFollowing=false;
    EntityPlayer targetPlayer=null;
    private int followUpdateCounter=0;
    private static final int FOLLOW_UPDATE_INTERVAL=0; // 每10tick更新一次路径

    public AutoWalker(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void startWalking(BlockPos targetIn){
        // 如果正在追随，先停止追随
        if(isFollowing){
            stopFollowing();
        }
        isWalking=true;
        target=targetIn;
        Minecraft.getMinecraft().thePlayer.movementInput=new ModMovementInput(this);
        currentPath=PathFinder.findPath(Minecraft.getMinecraft().theWorld, Minecraft.getMinecraft().thePlayer.getPosition().down(), target);
    }

    public void stopWalking(){
        isWalking=false;
        target=null;
        Minecraft.getMinecraft().thePlayer.movementInput=new MovementInputFromOptions(Minecraft.getMinecraft().gameSettings);
        currentPath=null;
    }

    // 开始追随指定玩家
    public void startFollowing(EntityPlayer player){
        if(player==null){
            return;
        }
        // 如果正在行走，先停止行走
        if(isWalking){
            stopWalking();
        }
        isFollowing=true;
        targetPlayer=player;
        Minecraft.getMinecraft().thePlayer.movementInput=new ModMovementInput(this);
        followUpdateCounter=0;
    }

    // 停止追随
    public void stopFollowing(){
        isFollowing=false;
        targetPlayer=null;
        Minecraft.getMinecraft().thePlayer.movementInput=new MovementInputFromOptions(Minecraft.getMinecraft().gameSettings);
        currentPath=null;
    }
    public static BlockPos getPlayerBlockPos(EntityPlayer player){
        BlockPos blockpos = new BlockPos(player.posX, player.getEntityBoundingBox().minY, player.posZ);
        return blockpos;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e){
        if(e.phase!= TickEvent.Phase.START){
            return;
        }
        Minecraft mc=Minecraft.getMinecraft();
        EntityPlayerSP player=mc.thePlayer;
        if(player==null||mc.theWorld==null){
            return;
        }

        if(player.isRiding()){
            stopAll();
        }

        // 处理追随玩家逻辑
        if(isFollowing && targetPlayer!=null){
            handleFollowing(mc, player);
            return;
        }

        // 原有的寻路逻辑
        if(!isWalking||target==null){
            return;
        }

        BlockPos blockpos = new BlockPos(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().getEntityBoundingBox().minY, mc.getRenderViewEntity().posZ);

        // 如果到达目标，停止行走
        if (blockpos.equals(target)) {
            stopWalking();
            return;
        }

        // 如果当前路径不包含当前位置或路径为空，重新计算路径
        if (currentPath == null || currentPath.isEmpty() || !currentPath.contains(blockpos)) {
            currentPath = PathFinder.findPath(mc.theWorld, blockpos, target);
            if (currentPath == null || currentPath.isEmpty()) {
                // 无法找到路径，停止行走
                stopWalking();
                return;
            }
        }

        // 获取下一个目标方块
        int currentIndex = currentPath.indexOf(blockpos);
        if (currentIndex >= 0 && currentIndex < currentPath.size() - 1) {
            BlockPos next = currentPath.get(currentIndex + 1);
            // 转向下一个方块
            player.rotationYaw = smoothRotation(player.rotationYaw, getYawRotToBLockPos(player, next));

            // 判断是否需要跳跃（下一个方块比当前高）
            needsJump = next.getY() > blockpos.getY();
        }
    }
    @SubscribeEvent
    public void onWorld(WorldEvent.Load e){
        stopAll();
    }
    private void stopAll(){
        if(isWalking){
            stopWalking();
        }
        if(isFollowing){
            stopFollowing();
        }
    }

    // 处理追随玩家的逻辑
    private void handleFollowing(Minecraft mc, EntityPlayerSP player){
        // 检查目标玩家是否还存在
        if(targetPlayer.isDead || targetPlayer.getEntityWorld()!=mc.theWorld){
            stopFollowing();
            return;
        }

        BlockPos currentPlayerPos = getPlayerBlockPos(player);
        BlockPos targetPos = getPlayerBlockPos(targetPlayer);

        /*// 计算与目标玩家的距离
        double distance = player.getDistanceSqToEntity(targetPlayer);

        // 如果距离足够近（2格以内），停止移动
        if(distance <= 4.0){
            currentPath=null;
            needsJump=false;
            return;
        }*/

        // 每隔一定间隔更新路径
        followUpdateCounter++;
        if(followUpdateCounter>=FOLLOW_UPDATE_INTERVAL || currentPath==null || currentPath.isEmpty()){
            followUpdateCounter=0;
            currentPath = PathFinder.findPath(mc.theWorld, currentPlayerPos, targetPos);

            // 如果找不到路径，尝试直接朝目标玩家方向移动
            if(currentPath==null || currentPath.isEmpty()){
                player.rotationYaw = smoothRotation(player.rotationYaw, getYawRotToBLockPos(player, targetPos));
                needsJump = targetPos.getY() > currentPlayerPos.getY();
                return;
            }
        }

        // 沿着路径移动
        if(currentPath!=null && !currentPath.isEmpty()){
            BlockPos blockpos = new BlockPos(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().getEntityBoundingBox().minY, mc.getRenderViewEntity().posZ);

            int currentIndex = currentPath.indexOf(blockpos);
            if (currentIndex >= 0 && currentIndex < currentPath.size() - 1) {
                BlockPos next = currentPath.get(currentIndex + 1);
                // 转向下一个方块
                player.rotationYaw = smoothRotation(player.rotationYaw, getYawRotToBLockPos(player, next));

                // 判断是否需要跳跃
                needsJump = next.getY() > blockpos.getY();
            } else {
                // 如果当前位置不在路径上，重新计算路径
                currentPath = PathFinder.findPath(mc.theWorld, currentPlayerPos, targetPos);
            }
        }
    }

    //返回实体看向方块所需的横向角度,这个函数是ai写的
    public float getYawRotToBLockPos(Entity entity, BlockPos blockPos){
        if (entity == null || blockPos == null) {
            return 0.0f;
        }

        double dx = blockPos.getX() + 0.5 - entity.posX;
        double dz = blockPos.getZ() + 0.5 - entity.posZ;

        double yaw = Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0;

        return net.minecraft.util.MathHelper.wrapAngleTo180_float((float) yaw);
    }
    // 平滑转头相关字段
    private static final float MAX_YAW_CHANGE_PER_TICK = 10.0f; // 每tick最大转向角度

    /**
     * 平滑旋转：将当前角度逐步转向目标角度
     * @param currentYaw 当前角度
     * @param targetYaw 目标角度
     * @return 插值后的角度
     */
    private float smoothRotation(float currentYaw, float targetYaw) {
        // 计算角度差（考虑-180到180的环绕）
        float angleDiff = net.minecraft.util.MathHelper.wrapAngleTo180_float(targetYaw - currentYaw);

        // 限制每次转动的最大角度
        if (angleDiff > MAX_YAW_CHANGE_PER_TICK) {
            angleDiff = MAX_YAW_CHANGE_PER_TICK;
        } else if (angleDiff < -MAX_YAW_CHANGE_PER_TICK) {
            angleDiff = -MAX_YAW_CHANGE_PER_TICK;
        }

        // 返回新的角度
        return net.minecraft.util.MathHelper.wrapAngleTo180_float(currentYaw + angleDiff);
    }
}
