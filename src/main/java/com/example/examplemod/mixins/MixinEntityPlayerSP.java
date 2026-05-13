package com.example.examplemod.mixins;

import com.example.examplemod.AutoWalk.AutoWalker;
import com.example.examplemod.AutoWalk.PathFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    @Redirect(
            method = "onLivingUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z"
            )
    )
    private boolean alwaysSprint(net.minecraft.client.settings.KeyBinding instance) {
        return true;
    }
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void onMessage(String message, CallbackInfo ci){
        if(message.contains("goto")){
            AutoWalker.autoWalker.startWalking(parseToBlockPos(message));
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(PathFinder.findPath(Minecraft.getMinecraft().theWorld, Minecraft.getMinecraft().thePlayer.getPosition().down(), Minecraft.getMinecraft().objectMouseOver.getBlockPos()).toString()));
            ci.cancel();
        }else if(message.contains("stop")){
            AutoWalker.autoWalker.stopWalking();
            ci.cancel();
        }
    }
    //字符串转换是ai写的
    private final Pattern COORDINATE_PATTERN = Pattern.compile(
            "(-?\\d+\\.?\\d*)\\s+(-?\\d+\\.?\\d*)\\s+(-?\\d+\\.?\\d*)"
    );

    /**
     * 从字符串中提取坐标信息并转换为BlockPos
     * 支持格式:
     * - "<player>:goto 123 456 789"
     * - "/goto 123 456 789"
     * - "123 456 789"
     * - 其他包含三个连续数字的字符串
     *
     * @param input 输入字符串
     * @return 解析出的BlockPos对象，如果解析失败返回null
     */
    public BlockPos parseToBlockPos(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        // 查找匹配坐标的部分
        Matcher matcher = COORDINATE_PATTERN.matcher(input);

        if (matcher.find()) {
            try {
                double x = Double.parseDouble(matcher.group(1));
                double y = Double.parseDouble(matcher.group(2));
                double z = Double.parseDouble(matcher.group(3));

                // 将double转换为int（BlockPos使用整数坐标）
                return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            } catch (NumberFormatException e) {
                System.err.println("解析坐标时出错: " + e.getMessage());
                return null;
            }
        }

        System.err.println("未在字符串中找到有效的坐标: " + input);
        return null;
    }
}