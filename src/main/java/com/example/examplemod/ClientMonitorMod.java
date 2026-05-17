package com.example.examplemod;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minecraft 1.8.9 Forge 客户端监控
 * 简化版本 - 静态工具类形式
 */
public class ClientMonitorMod {

    private static final String API_URL = "http://localhost:8080/api/rooms";
    private static final String PROGRAM_NAME = "MinecraftClient-1.8.9";

    private static final Gson gson = new Gson();
    private static int tickCounter = 0;
    private static final int SYNC_INTERVAL_TICKS = 100; // 100 ticks = 5秒
    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * 初始化方法 - 在模组加载时调用
     */
    public static void initialize() {
        System.out.println("===========================================");
        System.out.println("🎮 Client Monitor (1.8.9 Forge) 已启动");
        System.out.println("📡 监控后端: " + API_URL);
        System.out.println("⏱️  同步间隔: 5秒");
        System.out.println("===========================================");

        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(new ClientMonitorMod());
    }

    /**
     * 客户端Tick事件处理
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // 只在END阶段执行，避免重复
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;

        // 每5秒发送一次数据
        if (tickCounter >= SYNC_INTERVAL_TICKS) {
            tickCounter = 0;

            if (mc.theWorld != null && mc.thePlayer != null) {
                sendPlayerData();
            }
        }
    }

    /**
     * 发送玩家数据
     */
    private void sendPlayerData() {
        try {
            EntityPlayerSP localPlayer = mc.thePlayer;
            if (localPlayer == null || mc.theWorld == null) return;

            // 获取所有在线玩家列表
            List<EntityPlayer> allPlayers = mc.theWorld.playerEntities;

            // 构建房间数据
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("programName", PROGRAM_NAME);

            // 生成房间ID
            String serverIP = mc.getCurrentServerData() != null ?
                    mc.getCurrentServerData().serverIP : "singleplayer";
            String roomId = "server-" + serverIP.replace(":", "-").replace(".", "-");
            roomData.put("roomId", roomId);

            roomData.put("roomName", getRoomName());
            roomData.put("currentPlayerCount", allPlayers.size());
            roomData.put("maxPlayerCount", 100); // 默认最大100人，可根据需要修改
            roomData.put("status", allPlayers.size() >= 100 ? "full" : "open");
            roomData.put("gameMode", getPlayerGameMode(localPlayer));

            // 添加所有玩家信息
            JsonArray playerArray = new JsonArray();
            for (EntityPlayer player : allPlayers) {
                if (player == null) continue;

                JsonObject playerInfo = new JsonObject();

                // 基本信息
                playerInfo.addProperty("playerName", player.getName());

                // 获取玩家UUID（前端用于生成皮肤URL）
                String playerId = player.getUniqueID().toString();
                playerInfo.addProperty("playerId", playerId);

                playerInfo.addProperty("level", player.experienceLevel);
                playerInfo.addProperty("gameMode", getPlayerGameMode(player));
                playerInfo.addProperty("isOp", false); // 客户端无法判断OP

                // 血量和饥饿值
                playerInfo.addProperty("health", player.getHealth());
                playerInfo.addProperty("maxHealth", player.getMaxHealth());
                playerInfo.addProperty("foodLevel", player.getFoodStats().getFoodLevel());

                // 坐标位置
                JsonObject position = new JsonObject();
                position.addProperty("x", player.posX);
                position.addProperty("y", player.posY);
                position.addProperty("z", player.posZ);
                position.addProperty("dimension", getDimensionName(player.dimension));
                playerInfo.add("position", position);

                // 其他信息
                playerInfo.addProperty("isFlying", player.capabilities.isFlying);
                playerInfo.addProperty("allowFlying", player.capabilities.allowFlying);
                playerInfo.addProperty("isSprinting", player.isSprinting());
                playerInfo.addProperty("isSneaking", player.isSneaking());

                // 标记是否是本地玩家
                playerInfo.addProperty("isLocalPlayer", player == localPlayer);

                playerArray.add(playerInfo);
            }

            roomData.put("players", playerArray);

            // 异步发送HTTP请求（避免阻塞游戏）
            final String jsonData = gson.toJson(roomData);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendToApi(jsonData);
                }
            }).start();

        } catch (Exception e) {
            System.err.println("❌ 发送玩家数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取房间名称
     */
    private String getRoomName() {
        if (mc.isSingleplayer()) {
            return "单人游戏 - " + mc.thePlayer.getName();
        } else {
            String serverIP = mc.getCurrentServerData() != null ?
                    mc.getCurrentServerData().serverIP : "未知服务器";
            return "多人服务器 - " + serverIP;
        }
    }

    /**
     * 获取玩家游戏模式
     */
    private String getPlayerGameMode(EntityPlayer player) {
        if (player.capabilities.isCreativeMode) {
            return "creative";
        } else if (player.capabilities.allowEdit) {
            return "survival";
        } else {
            return "adventure";
        }
    }

    /**
     * 获取维度名称
     */
    private String getDimensionName(int dimensionId) {
        switch (dimensionId) {
            case -1:
                return "nether";
            case 0:
                return "overworld";
            case 1:
                return "end";
            default:
                return "overworld";
        }
    }

    /**
     * 发送数据到API
     */
    private void sendToApi(String jsonData) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // 发送请求
            OutputStream os = connection.getOutputStream();
            byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
            os.flush();
            os.close();

            // 检查响应
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                System.out.println("✅ [ClientMonitor] 同步成功");
            } else {
                System.err.println("❌ [ClientMonitor] 同步失败 | HTTP " + responseCode);
            }

        } catch (Exception e) {
            System.err.println("❌ [ClientMonitor] 发送数据失败: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
