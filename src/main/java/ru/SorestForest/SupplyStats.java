package ru.SorestForest;

import java.util.Locale;

public class SupplyStats {

    public static String calculateStats(String factionStr) {
        if (factionStr == null || factionStr.isEmpty()) {
            return "Фракция не указана.";
        }

        String faction = factionStr.toUpperCase(Locale.ROOT).trim();
        if (!MemberUtils.isFaction(faction,true)) {
            return "Неверно указана фракция.";
        }

        int totalOrganized = 0;
        int wonOrganized = 0;
        int lostOrganized = 0;
        int afkOrganized = 0;

        int totalParticipated = 0;
        int wonParticipated = 0;
        int lostParticipated = 0;
        for (var entry : SupplyManager.data.entrySet()) {
            SupplyManager.Supply supply = entry.getValue();

            if (!supply.ended) continue; // учитываем только завершённые поставки

            boolean isOrganizer = supply.faction.equalsIgnoreCase(faction);
            boolean participated = isOrganizer || (supply.attack != null && supply.attack.toUpperCase(Locale.ROOT).contains(faction));

            if (!participated) continue;

            if (isOrganizer) {
                totalOrganized++;
                if (supply.afk) afkOrganized++;
                else if (Boolean.TRUE.equals(supply.winner)) wonOrganized++;
                else lostOrganized++;
            } else {
                totalParticipated++;
                if (Boolean.FALSE.equals(supply.winner)) wonParticipated++;
                else lostParticipated++;
            }
        }

        return String.format(
                "**Статистика по фракции %s (за последние 7 дней):**\n\n" +
                        "__Организованные поставки:__\n" +
                        "Всего: %d\n" +
                        "Выиграно: %d\n" +
                        "Проиграно: %d\n" +
                        "AFK: %d\n\n" +
                        "__Участие в других поставках:__\n" +
                        "Всего: %d\n" +
                        "Выиграно: %d\n" +
                        "Проиграно: %d\n",
                faction,
                totalOrganized, wonOrganized, lostOrganized, afkOrganized,
                totalParticipated, wonParticipated, lostParticipated
        );
    }
}
