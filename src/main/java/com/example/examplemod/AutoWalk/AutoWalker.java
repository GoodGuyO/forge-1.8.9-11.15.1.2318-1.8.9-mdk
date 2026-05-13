package com.example.examplemod.AutoWalk;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedList;

public class AutoWalker {
    public static AutoWalker autoWalker=null;
    boolean isWalking=false;
    boolean needsJump=false;
    public BlockPos target=null;
    public AutoWalker(){
        MinecraftForge.EVENT_BUS.register(this);
    }
    public void startWalking(BlockPos targetIn){
        isWalking=true;
        target=targetIn;
        Minecraft.getMinecraft().thePlayer.movementInput=new ModMovementInput(this);
    }
    public void stopWalking(){
        isWalking=false;
        target=null;
        Minecraft.getMinecraft().thePlayer.movementInput=new MovementInputFromOptions(Minecraft.getMinecraft().gameSettings);
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
        if(!isWalking||target==null){
            return;
        }

        BlockPos blockpos = new BlockPos(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().getEntityBoundingBox().minY, mc.getRenderViewEntity().posZ);
        LinkedList<BlockPos> path=PathFinder.findPath(mc.theWorld, blockpos.down(), target);
        if(!path.isEmpty()){
            //转向下一个方块
            player.rotationYaw=getYawRotToBLockPos(player, path.getFirst());

            if(path.getFirst().getY()>blockpos.down().getY()){
                needsJump=true;
            }else{
                needsJump=false;
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
}
