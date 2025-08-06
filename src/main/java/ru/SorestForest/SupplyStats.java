package ru.SorestForest;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.Instant;
import java.util.Locale;

public class SupplyStats {

    public static MessageEmbed calculateStats(String factionStr) {
        if (factionStr == null || factionStr.isEmpty()) {
            return errorEmbed("Фракция не указана.");
        }

        String faction = factionStr.toUpperCase(Locale.ROOT).trim();
        if (!MemberUtils.isFaction(faction, true)) {
            return errorEmbed("Неверно указана фракция.");
        }

        MemberUtils.Faction f = MemberUtils.Faction.valueOf(faction);

        int totalOrganized = 0;
        int afkDelivered = 0;

        int defended = 0;
        int defendedWon = 0;
        int defendedLost = 0;

        int attacked = 0;
        int attackedWon = 0;
        int attackedLost = 0;

        for (var entry : SupplyManager.data.entrySet()) {
            SupplyManager.Supply supply = entry.getValue();
            if (!supply.ended) continue;

            boolean isDestination = f.equals(supply.destination);
            boolean isDefender = supply.defenders != null && supply.defenders.contains(f);
            boolean isAttacker = supply.attackers != null && supply.attackers.contains(f);

            // 1. Организованные поставки
            if (isDestination) {
                totalOrganized++;
                if (supply.afk) afkDelivered++;
            }

            // 2. Защита
            if (isDefender) {
                defended++;
                if (Boolean.TRUE.equals(supply.defenderWin)) defendedWon++;
                else defendedLost++;
            }

            // 3. Атака
            if (isAttacker) {
                attacked++;
                if (Boolean.FALSE.equals(supply.defenderWin)) attackedWon++;
                else attackedLost++;
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📦 Статистика по фракции " + f.displayName())
                .setColor(f.color())
                .setDescription("Период: **последние 7 дней**")
                .setTimestamp(Instant.now());

        embed.addField("__Организованные поставки__", String.format(
                "Всего: **%d**\nAFK: **%d**",
                totalOrganized, afkDelivered
        ), false);

        embed.addField("__Участие в защите__", String.format(
                "Всего: **%d**\n" +
                        "Успешно %s: **%d**\n" +
                        "Провалено %s: **%d**",
                defended, EmojiUtils.YES_EMOJI, defendedWon,
                EmojiUtils.NO_EMOJI, defendedLost
        ), false);

        embed.addField("__Участие в атаке__", String.format(
                "Всего: **%d**\n" +
                        "Успешно %s: **%d**\n" +
                        "Провалено %s: **%d**",
                attacked, EmojiUtils.YES_EMOJI, attackedWon,
                EmojiUtils.NO_EMOJI, attackedLost
        ), false);

        return embed.build();
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
