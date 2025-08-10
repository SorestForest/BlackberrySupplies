package ru.SorestForest;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.*;

public class SupplyStats {

    public static MessageEmbed calculateStats(String factionStr, int period) {
        if (factionStr == null || factionStr.isEmpty()) {
            return errorEmbed("Фракция не указана.");
        }

        String faction = factionStr.toUpperCase(Locale.ROOT).trim();
        if (!MemberUtils.isFaction(faction, true)) {
            return errorEmbed("Неверно указана фракция.");
        }

        MemberUtils.Faction f = MemberUtils.toFaction(faction);

        int totalOrganized = 0;
        int afkDelivered = 0;

        int defended = 0;
        int defendedWon = 0;
        int defendedLost = 0;

        int attacked = 0;
        int attackedWon = 0;
        int attackedLost = 0;

        for (var supply : getSuppliesForPeriod(period)) {
            assert f != null;
            boolean isDestination = f.equals(supply.destination);
            boolean isDefender = supply.defenders != null && supply.defenders.contains(f);
            boolean isAttacker = supply.attackers != null && supply.attackers.contains(f);

            if (isDestination) {
                totalOrganized++;
                if (supply.afk) afkDelivered++;
            }

            if (isDefender) {
                defended++;
                if (Boolean.TRUE.equals(supply.defenderWin)) defendedWon++;
                else defendedLost++;
            }

            if (isAttacker) {
                attacked++;
                if (Boolean.FALSE.equals(supply.defenderWin)) attackedWon++;
                else attackedLost++;
            }
        }

        assert f != null;
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📦 Статистика по фракции " + f.displayName())
                .setColor(f.color())
                .setDescription("Период: **последние " + period + " дней**")
                .setTimestamp(Instant.now());

        embed.addField("__Организованные поставки__", String.format(
                "Всего: **%d**\nAFK: **%d**",
                totalOrganized, afkDelivered
        ), false);

        embed.addField("__Участие в защите__", String.format(
                """
                        Всего: **%d**
                        Успешно %s: **%d**
                        Провалено %s: **%d**""",
                defended, EmojiUtils.YES_EMOJI, defendedWon,
                EmojiUtils.NO_EMOJI, defendedLost
        ), false);

        embed.addField("__Участие в атаке__", String.format(
                """
                        Всего: **%d**
                        Успешно %s: **%d**
                        Провалено %s: **%d**""",
                attacked, EmojiUtils.YES_EMOJI, attackedWon,
                EmojiUtils.NO_EMOJI, attackedLost
        ), false);

        return embed.build();
    }

    public static MessageEmbed calculateGeneralStats(int period) {
        int ngPlayed = 0, ngWinned = 0;
        int emsPlayed = 0, emsWinned = 0;
        int spankPlayed = 0, spankWinned = 0;

        for (var supply : getSuppliesForPeriod(period)) {
            if (supply.type == SupplyManager.SupplyType.NG) {
                ngPlayed++;
                ngWinned += Boolean.TRUE.equals(supply.defenderWin) ? 1 : 0;
            } else if (supply.type == SupplyManager.SupplyType.EMS) {
                emsPlayed++;
                emsWinned += Boolean.TRUE.equals(supply.defenderWin) ? 1 : 0;
            } else if (supply.type.name().contains("SPANK")) {
                spankPlayed++;
                spankWinned += Boolean.TRUE.equals(supply.defenderWin) ? 1 : 0;
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 Общая статистика по NG / EMS / SPANK")
                .setColor(Color.ORANGE)
                .setDescription("Период: **последние " + period + " дней**")
                .setTimestamp(Instant.now());

        embed.addField("NG", formatStats(ngPlayed, ngWinned), true);
        embed.addField("EMS", formatStats(emsPlayed, emsWinned), true);
        embed.addField("SPANK", formatStats(spankPlayed, spankWinned), true);

        return embed.build();
    }

    public static MessageEmbed calculateMapStats(int period) {
        Map<String, Integer> stats = new HashMap<>();
        List<SupplyManager.Supply> supplies = getSuppliesForPeriod(period);
        int totalPlayed = 0;
        for (var supply : supplies) {
            if (supply.map == null || supply.map.isEmpty() || supply.map.equalsIgnoreCase("не выбрана")) continue;
            stats.put(supply.map.toLowerCase(), stats.getOrDefault(supply.map.toLowerCase(), 0) + 1);
            totalPlayed++;
        }
        List<Map.Entry<String, Integer>> sortedStats = new ArrayList<>(stats.entrySet());
        sortedStats.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        StringBuilder sb = new StringBuilder();
        for (var entry : sortedStats) {
            double percent = totalPlayed > 0
                    ? (entry.getValue() * 100.0 / totalPlayed)
                    : 0;
            sb.append("**").append(Settings.capitalizeFirst(entry.getKey())).append("** — ")
                    .append(entry.getValue()).append(" раз(а) ")
                    .append(String.format("(%.1f%%)", percent))
                    .append("\n");
        }

        if (sb.isEmpty()) {
            sb.append("Нет сыгранных поставок за указанный период.");
        }

        return new EmbedBuilder()
                .setTitle("🗺 Статистика по картам")
                .setDescription("Период: **последние " + period + " дней**")
                .addField("Карты", sb.toString(), false)
                .setColor(Color.CYAN)
                .setTimestamp(Instant.now())
                .build();
    }



    private static String formatStats(int played, int won) {
        if (played == 0) {
            return "Сыграно: **0**\nПобед: **0**\nПобед%: **0%**";
        }
        double winRate = (won * 100.0) / played;
        return String.format(
                "Сыграно: **%d**\nПобед: **%d**\nПобед%%: **%.1f%%**",
                played, won, winRate
        );
    }



    private static List<SupplyManager.Supply> getSuppliesForPeriod(int period) {
        LocalDateTime cutoffDate = LocalDateTime.now()
                .atZone(ZoneId.of("Europe/Moscow"))
                .minusDays(period)
                .toLocalDateTime();

        List<SupplyManager.Supply> supplies = new ArrayList<>();
        for (var supply : SupplyManager.data.values()) {
            if (!supply.ended) continue;
            if (supply.timeEnded == null || supply.timeEnded.isBefore(cutoffDate)) continue;
            supplies.add(supply);
        }
        return supplies;
    }



    private static MessageEmbed errorEmbed(String message) {
        return new EmbedBuilder()
                .setColor(Color.RED)
                .setTitle("Ошибка")
                .setDescription(message)
                .setTimestamp(Instant.now())
                .build();
    }
}
