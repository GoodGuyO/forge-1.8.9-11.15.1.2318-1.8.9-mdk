package com.example.examplemod;

import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import java.util.*;
import java.util.stream.Collectors;

public class SidebarHelper {

    /**
     * 获取主世界侧边栏显示的所有行字符串。
     * 返回的列表顺序与游戏内从上到下的显示顺序一致。
     * 每个字符串已经应用了队伍颜色前缀和后缀。
     */
    public static List<String> getSidebarLines() {
        // 1. 获取服务端计分板
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        if (world == null) return Collections.emptyList();
        Scoreboard sb = world.getScoreboard();

        // 2. 获取侧边栏显示的计分项 (槽位 1)
        ScoreObjective sidebarObjective = sb.getObjectiveInDisplaySlot(1);
        if (sidebarObjective == null) return Collections.emptyList();

        // 3. 获取该计分项的所有分数
        Collection<Score> scores = sb.getScores();
        if (scores.isEmpty()) return Collections.emptyList();

        // 4. 按分数降序排序（游戏中侧边栏从上到下是分数从高到低）
        List<Score> sortedScores = scores.stream()
                .sorted(Comparator.comparingInt(Score::getScorePoints).reversed())
                .collect(Collectors.toList());

        // 5. 构建显示字符串列表
        List<String> lines = new ArrayList<>();
        for (Score score : sortedScores) {
            String entry = score.getPlayerName();          // 玩家名或虚拟条目
            String displayString = formatEntry(sb, entry); // 应用队伍格式
            lines.add(displayString);
        }
        return lines;
    }

    /**
     * 为计分板条目应用队伍颜色、前缀、后缀，返回最终显示的字符串。
     */
    private static String formatEntry(Scoreboard sb, String entry) {
        for (ScorePlayerTeam team : sb.getTeams()) {
            if (team.getMembershipCollection().contains(entry)) {
                // 拼接：前缀 + 条目名 + 后缀（颜色会渗透，这就是游戏内的效果）
                return team.getColorPrefix() + entry + team.getColorSuffix();
            }
        }
        // 不属于任何队伍，直接用原名
        return entry;
    }

    /**
     * 示例：同时获取侧边栏的标题（即计分项的显示名称）
     */
    public static String getSidebarTitle() {
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        if (world == null) return "";
        ScoreObjective obj = world.getScoreboard().getObjectiveInDisplaySlot(1);
        return obj != null ? obj.getDisplayName() : "";
    }
}