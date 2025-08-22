package ru.SorestForest;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static ru.SorestForest.Settings.*;

@SuppressWarnings("DataFlowIssue")
public class SlashCommandHandler extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case NG_COMMAND -> handleSupply(event, SupplyManager.SupplyType.NG);
            case EMS_COMMAND -> handleSupply(event, SupplyManager.SupplyType.EMS);
            case SPANK_COMMAND -> handleSupply(event, SupplyManager.SupplyType.SPANK_BLANK);
            case ROLL_COMMAND -> handleRollResult(event);
            case RESULT_COMMAND -> handleResult(event);
            case "update" -> handleUpdate(event);
            case STATS_COMMAND -> handleStats(event);
            case "save" -> handleSave(event);
            case "лидер" -> SupplyManager.handleLeaderCommand(event);
            case "cancel" -> handleCancel(event);
            case "помощь" -> handleHelpCommand(event);
            case "dump-data" -> handleDump(event);
            case "авто-ролл" -> {
                if (Objects.equals(event.getSubcommandName(), "карта")) {
                    handleRollMap(event);
                } else if (Objects.equals(event.getSubcommandName(), "фракция")) {
                    handleRollFactions(event);
                }
            }
            case "карта" -> MapUtils.handleMapCommand(event);
            case "whoami" -> {
                if (!MemberUtils.isModerator(event.getMember())) {
                    event.reply("idk who you are").setEphemeral(true).queue();
                    return;
                }
                StringBuilder msg = new StringBuilder("isModerator: " + MemberUtils.isModerator(event.getMember()) + '\n');
                msg.append("<#" + SUPPLY_CHANNEL_ID + ">\n");
                for (MemberUtils.Faction value : MemberUtils.Faction.values()) {
                    msg.append(value.displayName()).append("\n");
                }
                msg.append("<@&").append(Settings.SUPPLY_ROLE_ID).append(">\n");
                msg.append("<@&").append(Settings.MODERATOR_ROLE_ID).append(">\n");
                msg.append("<@&").append(Settings.DEPLEADER_ROLE_ID).append(">\n");
                msg.append("<@&").append(Settings.CRIME_ROLE_ID).append(">\n");
                msg.append("<@&").append(Settings.STATE_ROLE_ID).append(">\n");
                msg.append("<@&").append(Settings.DEVELOPER_ROLE_ID).append(">\n");
                msg.append("<@&").append(Settings.LSV_ID).append(">\n");
                msg.append("<@&").append(Settings.ESB_ID).append(">\n");
                msg.append("<@&").append(Settings.FAM_ID).append(">\n");
                msg.append("<@&").append(Settings.MG13_ID).append(">\n");
                msg.append("<@&").append(Settings.BSG_ID).append(">\n");
                msg.append("<@&").append(Settings.MM_ID).append(">\n");
                msg.append("<@&").append(Settings.AM_ID).append(">\n");
                msg.append("<@&").append(Settings.LCN_ID).append(">\n");
                msg.append("<@&").append(Settings.YAK_ID).append(">\n");
                msg.append("<@&").append(Settings.RM_ID).append(">\n");
                msg.append("<@&").append(Settings.LSSD_ID).append(">\n");
                msg.append("<@&").append(Settings.LSPD_ID).append(">\n");
                msg.append("<@&").append(Settings.FIB_ID).append(">\n");
                msg.append("<@&").append(Settings.GOV_ID).append(">\n");
                msg.append("<@&").append(Settings.NG_ID).append(">\n");
                msg.append("<@&").append(Settings.EMS_ID).append(">\n");
                msg.append("<@&").append(Settings.SASPA_ID).append(">\n");
                event.reply(msg.toString()).queue();
            }
            default -> System.err.println("ERROR COMMAND");
        }
    }

    private void handleRollMap(SlashCommandInteractionEvent event) {
        event.deferReply(false).queue();
        if (!event.getChannelType().isThread()) {
            event.getHook().sendMessage("Команда работает только в ветке поставки.").queue();
            return;
        }

        event.getChannel().asThreadChannel().retrieveParentMessage().queue(parentMessage -> {
            SupplyManager.Supply supply = getSupplyFromParent(event, parentMessage);
            if (supply == null) return;
            String mapsStr = event.getOption("maps", null, OptionMapping::getAsString);
            if (mapsStr == null || mapsStr.isBlank()) {
                event.getHook().sendMessage("Не указаны карты для розыгрыша.").queue();
                return;
            }
            List<String> maps = Arrays.stream(mapsStr.trim().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (!MapUtils.areAllMapsValid(maps)) {
                event.getHook().sendMessage("Не все карты были распознаны как возможные карты для игры.").queue();
                return;
            }
            String chosenMap = Settings.capitalizeFirst(maps.get(ThreadLocalRandom.current().nextInt(maps.size())));
            supply.map = chosenMap;
            updateEmbed(parentMessage.getId(), supply);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("🗺 Ролл карты")
                    .addField("Доступные карты", String.join(", ", maps), false)
                    .addField("✅ Выбранная карта", "**" + chosenMap + "**", false)
                    .setFooter("Выбор карты произведён " + event.getUser().getName(), event.getUser().getAvatarUrl());

            event.getHook().sendMessageEmbeds(eb.build()).queue();
        });
    }

    private static final SecureRandom secureRandom = new SecureRandom();

    private void handleRollFactions(SlashCommandInteractionEvent event) {
        event.deferReply(false).queue();
        if (!event.getChannelType().isThread()) {
            event.getHook().sendMessage("Команда работает только в ветке поставки.").queue();
            return;
        }
        event.getChannel().asThreadChannel().retrieveParentMessage().queue(parentMessage -> {
            SupplyManager.Supply supply = getSupplyFromParent(event, parentMessage);
            if (supply == null) return;
            String factionsStr = event.getOption("factions", null, OptionMapping::getAsString);
            if (factionsStr == null || factionsStr.isBlank()) {
                event.getHook().sendMessage("Не указаны фракции для розыгрыша.").queue();
                return;
            }
            factionsStr = factionsStr.replace(","," ");
            List<String> factionsRoll = Arrays.stream(factionsStr.trim().split(" "))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            List<List<MemberUtils.Faction>> factions = new ArrayList<>();

            for (String s : factionsRoll) {
                List<MemberUtils.Faction> group = new ArrayList<>();
                for (String f : s.split("\\+")) {
                    f = f.trim().toUpperCase();
                    if (MemberUtils.isFaction(f, true)) {
                        MemberUtils.Faction faction = MemberUtils.toFaction(f);
                        group.add(faction);
                    } else {
                        event.getHook().sendMessage("Одна из фракций не была распознана: " + f).queue();
                        return;
                    }
                }
                if (!group.isEmpty()) factions.add(group);
            }

            if (factions.isEmpty()) {
                event.getHook().sendMessage("Не удалось распознать ни одну фракцию.").queue();
                return;
            }

            supply.attackers = factions.get(secureRandom.nextInt(0, factions.size()));



            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("⚔️ Ролл фракций")
                    .addField("Участвующие фракции", formatFactionGroups(factions), false)
                    .addField("✅ Выбранная фракция(-и)", "**" + supply.getAttackersDisplay(false) + "**", false)
                    .setFooter("Выбор фракции произведён " + event.getUser().getName(), event.getUser().getAvatarUrl());

            updateEmbed(parentMessage.getId(), supply);
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        });
    }

    public static String formatFactionGroups(List<List<MemberUtils.Faction>> factions) {
        return factions.stream()
                .map(group -> group.stream()
                        .map(MemberUtils.Faction::displayName)
                        .collect(Collectors.joining(" + ")))
                .collect(Collectors.joining(", "));
    }

    private void handleHelpCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder help = new EmbedBuilder();
        help.setTitle("📘 Помощь по командам бота");
        help.setDescription("Ниже приведены все доступные команды и их назначение.");
        help.setColor(Color.CYAN);

        help.addField("/поставка-нг", """
    📦 Заказ материалов **армии (NG)** для гос. структур. Используется только армией.
    **Параметры:**
    • `destination` — фракция назначения (гос) на выбор
    • `defenders` — фракции стороны защиты (через запятую или пробел)
    • `time` — время поставки (HH:MM)
    • `amount` — количество материалов (по умолчанию 20000)
    • `afk` — [необязательно] AFK или боевая поставка
    """, false);

        help.addField("/поставка-емс", """
    💉 Заказ аптек **медслужбы (EMS)** для гос. структур. Используется только EMS.
    **Параметры:**
    • `destination` — фракция назначения (гос)
    • `defenders` — фракции стороны защиты
    • `time` — время (HH:MM)
    • `amount` — количество аптек (по умолчанию 1500)
    • `afk` — [необязательно] AFK или боевая
    """, false);

        help.addField("/поставка-спанк", """
    💼 Заказ спанка (анальгетиков) для **крайм-фракций**.
    **Параметры:**
    • `faction` — кто заказывает спанк
    • `destination` — куда разгружается поставка
    • `defenders` — фракции стороны защиты
    • `time` — время (HH:MM)
    • `amount` — количество (по умолчанию 1000)
    • `afk` — [необязательно] AFK или боевая
    """, false);

        help.addField("/управление", """
    🎲 Установить карту и/или фракцию нападения на активную поставку.
    **Параметры:**
    • `map` — карта розыгрыша
    • `attack` — фракция атаки (союзы через запятую)
    """, false);

        help.addField("/результат", """
    🏁 Установить результат и победителя поставки.
    **Параметры:**
    • `winner` — Защита или Атака
    • `result` — описание результата
    """, false);
        help.addField("/статистика", """
    📊 Посмотреть статистику по фракции.
    **Параметры:**
    • `faction` — фракция или тип статистики
    • `period` — месяц или неделя
    """, false);
        help.addField("/авто-ролл карта", """
    🎯 Случайно выбрать карту.
    **Параметры:**
    • `maps` — список карт через запятую
    """, false);

        help.addField("/авто-ролл фракция", """
    🎯 Случайно выбрать атакующую фракцию.
    **Параметры:**
    • `factions` — список фракций через запятую
    • союзы через '+', пример: bsg+esb
    """, false);

        help.addField("/карта список", "📃 Показать все карты.", false);
        help.setFooter("Бот создан Daniel Powell (sorestforest)");
        event.replyEmbeds(help.build()).setEphemeral(true).queue();
    }


    private void handleDump(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        if (!MemberUtils.isModerator(Objects.requireNonNull(event.getMember()))) {
            event.getHook().sendMessage("Команда доступна только модераторам.").queue();
            return;
        }
        if (!event.getChannelType().isThread()) {
            event.getHook().sendMessage("Команда подразумевает использование в ветке поставки.").queue();
            return;
        }

        event.getChannel().asThreadChannel().retrieveParentMessage().queue(parentMessage -> {
            SupplyManager.Supply supply = getSupplyFromParent(event, parentMessage);
            if (supply == null) return;
            String sb = "Type: " + supply.type + "\n" +
                    "Time: " + supply.time + "\n" +
                    "Destination: " + supply.destination + "\n" +
                    "Amount: " + supply.amount + "\n" +
                    "Defenders: " + supply.getDefendersDisplay(false) + "\n" +
                    "Attackers: " + supply.getAttackersDisplay(false) + "\n" +
                    "Map: " + supply.map + "\n" +
                    "Result: " + supply.result + "\n" +
                    "Ended: " + supply.ended + "\n" +
                    "AFK: " + supply.afk + "\n" +
                    "DefenderWin: " + supply.defenderWin + "\n";
            event.getHook().sendMessage(sb).queue();
        });
    }

    private void handleCancel(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        if (!MemberUtils.isModerator(Objects.requireNonNull(event.getMember()))) {
            event.getHook().sendMessage("Команда доступна только модераторам.").queue();
            return;
        }
        if (!event.getChannelType().isThread()) {
            event.getHook().sendMessage("Команда подразумевает использование в ветке поставки.").queue();
            return;
        }

        event.getChannel().asThreadChannel().retrieveParentMessage().queue(parentMessage -> {
            SupplyManager.Supply supply = getSupplyFromParent(event, parentMessage);
            if (supply == null) return;
            supply.ended = false;
            supply.result = "Отмена // by "+ event.getUser().getName();
            updateEmbed(parentMessage.getId(), supply);
            SupplyManager.data.remove(parentMessage.getId());
            event.getHook().sendMessage("Данные из базы удалены. Поставка отменена").queue();
        });
    }

    private void handleSave(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        if (!MemberUtils.isModerator(Objects.requireNonNull(event.getMember()))) {
            event.getHook().sendMessage("Команда доступна только модераторам.").queue();
            return;
        }
        try {
            SupplyManager.saveData();
            event.getHook().sendMessage("Данные успешно сохранены.").queue();
        } catch (Exception e) {
            event.getHook().sendMessage("Ошибка при сохранении данных: " + e.getMessage()).queue();
            e.printStackTrace();
        }
    }

    private void handleSupply(SlashCommandInteractionEvent event, SupplyManager.SupplyType type) {
        event.deferReply(false).setAllowedMentions(EnumSet.allOf(Message.MentionType.class)).queue();

        if (validatePermissions(event)) return;

        Member member = event.getMember();
        boolean isSpank = type == SupplyManager.SupplyType.SPANK_BLANK;

        // Проверка роли
        if (!isSpank) {
            assert member != null;
            if (!MemberUtils.isInFaction(member, type == SupplyManager.SupplyType.NG ? MemberUtils.Faction.NG : MemberUtils.Faction.EMS)) {
                event.getHook().sendMessage("Отпись поставок NG и EMS невозможно без соответствующей роли").queue();
                return;
            }
        } else {
            boolean hasMafiaRole = Arrays.stream(MemberUtils.Faction.values())
                    .anyMatch(f -> {
                        if (!f.isMafia()) return false;
                        assert member != null;
                        return MemberUtils.isInFaction(member, f);
                    });
            if (!hasMafiaRole) {
                event.getHook().sendMessage("Отпись поставок SPANK невозможно без соответствующей роли любой мафии.").queue();
                return;
            }
            String organizer = event.getOption("faction",OptionMapping::getAsString);
            type = SupplyManager.SupplyType.valueOf("SPANK_"+organizer);
        }

        String defendersStr = event.getOption("defenders", OptionMapping::getAsString);
        String destination = event.getOption("destination", OptionMapping::getAsString);
        String time = event.getOption("time", OptionMapping::getAsString);
        int amount = event.getOption("amount", OptionMapping::getAsInt);

        if (defendersStr == null || destination == null || time == null) {
            event.getHook().sendMessage("Не заполнены поля, которые обязательны для заполнения.").queue();
            return;
        }

        // Проверка фракций
        ForestPair<Boolean,List<MemberUtils.Faction>> defenderFactions = MemberUtils.parseFactions(defendersStr);
        if (defenderFactions.r.isEmpty() || !defenderFactions.l) {
            event.getHook().sendMessage("Неверно указаны фракции стороны защиты.").queue();
            return;
        }
        if (!MemberUtils.isFaction(destination, true)) {
            event.getHook().sendMessage("Неверно указана фракция назначения.").queue();
            return;
        }
        MemberUtils.Faction destFaction = MemberUtils.toFaction(destination);
        SupplyManager.Supply supply = new SupplyManager.Supply(type, time, amount, defenderFactions.r, destFaction);
        supply.defenders = defenderFactions.r;

        boolean afkStatus = Objects.equals(event.getOption("afk", OptionMapping::getAsString), "AFK");
        if (afkStatus) {
            supply.afk = true;
            supply.result = "Ожидается причина АФК поставки!";
        }

        int check = validateTime(event, time, supply);
        if (check == 0 && !MemberUtils.isModerator(Objects.requireNonNull(member))) return;
        // Проверка ВЗХ
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        if (Settings.isBetween(hours, minutes, 18, 30, 19, 30) && !isSpank) {
            event.getHook().sendMessage("В данное время нельзя заказать поставку в связи с ВЗХ.").queue();
            return;
        }

        String message = MemberUtils.CRIME_ROLE.getAsMention() + " " + MemberUtils.STATE_ROLE.getAsMention();
        if (check == 1){
            message += " ";
            message += String.format("<@&%s>", MODERATOR_ROLE_ID);
        }

        SupplyManager.SupplyType finalType = supply.type;
        /**/
        event.getHook().sendMessage(buildMessage(supply))
                .setContent(message)
                .queue(sentMessage -> {
                    SupplyManager.registerSupply(sentMessage.getId(), supply);
                    String threadName = isSpank
                            ? "spank-" + defendersStr.toLowerCase() + "(" + destination + ")"
                            : finalType.name().toLowerCase() + "-" + destination.toLowerCase();

                    sentMessage.createThreadChannel(threadName).queue(thread -> {

                        thread.sendMessage("Ветка создана для обсуждения поставки " + finalType.displayName() + " для " + destFaction.displayName()
                                + ". Защищают: "+supply.getDefendersDisplay(false) + "\n").queue();

                        if (supply.afk) {
                            String afkMention = isSpank ? supply.getDefendersDisplay(false) : destFaction.displayName();
                            thread.sendMessage("⚠️ Поставка была заказана как **AFK**. У фракции заказчика и **" + afkMention +
                                    "** есть **5 минут** на указание причины AFK-поставки, иначе она будет считаться **заказанной не по правилам**. Ссылку можно указать с помощью команды /результат, winner указывайте как Защита.").queue();
                        }
                    });
                });
    }

    private int validateTime(SlashCommandInteractionEvent event, String time, SupplyManager.Supply supply) {
        if (time == null || !time.matches("^\\d{2}:\\d{2}$")) {
            event.getHook().sendMessage("Ошибка: неверный формат времени. Ожидается HH:mm.").queue();
            return 0;
        }
        String[] parts = time.split(":");
        int hours, minutes;
        try {
            hours = Integer.parseInt(parts[0]);
            minutes = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            event.getHook().sendMessage("Ошибка: неверный числовой формат.").queue();
            return 0;
        }
        if (MemberUtils.isModerator(event.getMember())) {
            return 2;
        }
        if (hours < 0 || hours >= 23 || minutes < 0 || minutes >= 60) {
            event.getHook().sendMessage("Ошибка: часы или минуты вне допустимого диапазона.").queue();
            return 0;
        }
        LocalTime enteredTime = LocalTime.of(hours, minutes);
        LocalTime moscowTime = Settings.getMoscowTime();
        if (!enteredTime.isAfter(moscowTime)) {
            event.getHook().sendMessage("Ошибка: время должно быть позже текущего московского времени.").queue();
            return 0;
        }
        if (!(Settings.isBetween(hours, minutes, 13, 0, 15, 0) || Settings.isBetween(hours, minutes, 21, 0, 22, 0)) && supply.afk) {
            return 1;
        }
        return 2;
    }

    private void handleRollResult(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        if (!event.getChannelType().isThread()) {
            event.getHook().sendMessage("Команда подразумевает использование в ветке поставки.").queue();
            return;
        }
        event.getChannel().asThreadChannel().retrieveParentMessage().queue(parentMessage -> {
            SupplyManager.Supply supply = getSupplyFromParent(event, parentMessage);
            if (supply == null) return;

            String map = event.getOption("map", OptionMapping::getAsString);
            String attack = event.getOption("attack", OptionMapping::getAsString);
            if (map != null) {
                if (MapUtils.checkMap(map)) {
                    event.getHook().sendMessage("Указанная карта не найдена в разрешенном списке.").queue();
                    return;
                }
                supply.map = map;
            }
            if (attack != null) {
                ForestPair<Boolean,List<MemberUtils.Faction>> factions = MemberUtils.parseFactions(attack);
                if (factions.r.isEmpty() || !factions.l) {
                    event.getHook().sendMessage("Не распознано ни одной фракции. Фракции перечисляются через запяту или пробел: AM, LCN или am, lcn").queue();
                    return;
                }
                supply.attackers = factions.r;
            }
            updateEmbed(parentMessage.getId(), supply);
            event.getHook().sendMessage("Данные о поставке обновлены! Удачной игры!").queue();
        });
    }

    private void handleResult(SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("Команда подразумевает использование в ветке поставки.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(false).queue();
        event.getChannel().asThreadChannel().retrieveParentMessage().queue(parentMessage -> {
            SupplyManager.Supply supply = getSupplyFromParent(event, parentMessage);
            if (supply == null) return;

            String winner = event.getOption("winner", OptionMapping::getAsString);
            String result = event.getOption("result", OptionMapping::getAsString);

            if (winner == null || result == null) {
                event.getHook().sendMessage("Параметры winner и result обязательны.").setEphemeral(true).queue();
                return;
            }

            supply.defenderWin = winner.equals("Защита");
            supply.result = result;
            supply.ended = true;
            supply.timeEnded = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
            updateEmbed(parentMessage.getId(), supply);
            event.getHook().sendMessage("Данные о поставке обновлены, GG, WP! "+supply.defenderWin).queue();
        });
    }

    private void handleUpdate(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("Команда подразумевает использование в ветке поставки.").setEphemeral(true).queue();
            return;
        }

        String key = event.getOption("key", OptionMapping::getAsString);
        String value = event.getOption("value", OptionMapping::getAsString);
        event.deferReply(true).queue();

        event.getChannel().asThreadChannel().retrieveParentMessage().queue(parentMessage -> {
            SupplyManager.Supply supply = getSupplyFromParent(event, parentMessage);
            if (supply == null) {
                return;
            }

            if (!MemberUtils.isModerator(Objects.requireNonNull(event.getMember()))) {
                event.getHook().sendMessage("Команда предназначена для использования только с ролью модератора.").queue();
                return;
            }
            // time, destination, amount, defenders, map, result, attackers, afk, defenderWin, ended
            switch (Objects.requireNonNull(key)) {
                case "time" -> supply.time = value;
                case "destination" -> supply.destination = MemberUtils.toFaction(value);
                case "amount" -> {
                    assert value != null;
                    supply.amount = Integer.parseInt(value);
                }
                case "defenders" -> supply.defenders = MemberUtils.parseFactions(value).r;
                case "map" -> supply.map = value;
                case "result" -> supply.result = value;
                case "attackers" -> supply.attackers = MemberUtils.parseFactions(value).r;
                case "afk" -> supply.afk = Boolean.parseBoolean(value);
                case "defenderWin" -> supply.defenderWin = Boolean.parseBoolean(value);
                case "ended" -> supply.ended = Boolean.parseBoolean(value);
                default -> {
                    event.getHook().sendMessage("Не удалось распознать ключ.").queue();
                    return;
                }
            }
            updateEmbed(parentMessage.getId(), supply);
            event.getHook().sendMessage("Поставка обновлена.").queue();
        });
    }


    private void handleStats(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        String faction = event.getOption("faction", OptionMapping::getAsString);
        String period = event.getOption("period", OptionMapping::getAsString);
        int days;
        if (Objects.equals(period, "месяц")) {
            days = 31;
        } else {
            days = 7;
        }
        MessageEmbed statsEmbed;
        if ("General".equals(faction)) {
            statsEmbed = SupplyStats.calculateGeneralStats(days);
        } else if ("Map".equals(faction)) {
            statsEmbed = SupplyStats.calculateMapStats(days);
        } else {
            statsEmbed = SupplyStats.calculateStats(faction, days);
        }

        event.getHook().sendMessageEmbeds(statsEmbed).queue();
    }

    private boolean validatePermissions(SlashCommandInteractionEvent event) {
        if (!MemberUtils.isSupplier(Objects.requireNonNull(event.getMember()))) {
            event.getHook().sendMessage("Недостаточно прав для отписи поставки. Требуется роль отписи.").queue();
            return true;
        }
        if (!Settings.SUPPLY_CHANNEL_ID.equals(event.getChannelId())) {
            event.getHook().sendMessage("Невозможно отписать поставку не из канала отписей.").queue();
            return true;
        }
        return false;
    }

    private SupplyManager.Supply getSupplyFromParent(SlashCommandInteractionEvent event, Message parentMessage) {
        if (parentMessage == null) {
            event.getHook().sendMessage("Произошла ошибка! Код ошибки: 01.").setEphemeral(true).queue();
            return null;
        }
        SupplyManager.Supply supply = SupplyManager.getByMessageID(parentMessage.getId());
        if (supply == null) {
            event.getHook().sendMessage("Не удалось определить поставку! Код ошибки: 02.").setEphemeral(true).queue();
            return null;
        }
        if (!canManipulateSupply(supply, event.getMember())) {
            event.getHook()
                    .sendMessage("Данная поставка не ваша, обратитесь к " + supply.type.faction().displayName() + "/" +
                            supply.destination.displayName()+"/"+supply.getDefendersDisplay(false)+" для редакции информации.").queue();
            return null;
        }
        return supply;
    }

    private boolean canManipulateSupply(SupplyManager.Supply supply, Member member) {
        if (MemberUtils.isInFaction(member, supply.destination) || MemberUtils.isInFaction(member, supply.type.faction())) {
            return true;
        }
        if (supply.defenders != null) {
            for (MemberUtils.Faction defender : supply.defenders) {
                if (MemberUtils.isInFaction(member, defender)) {
                    return true;
                }
            }
        }
        if (supply.attackers != null) {
            for (MemberUtils.Faction attacker : supply.attackers) {
                if (MemberUtils.isInFaction(member, attacker)) {
                    return true;
                }
            }
        }
        return false;
    }

    private MessageEmbed buildEmbed(SupplyManager.Supply supply) {
        EmbedBuilder builder = new EmbedBuilder();
        // Цвет по типу поставки
        builder.setColor(getColorForType(supply.type));
        // Заголовок и иконка
        builder.setTitle(getSupplyIcon(supply.type) + " Поставка " + supply.type.displayName() + " для "+ supply.destination + " // Blackberry " + EmojiUtils.BLACKBERRY_EMOJI);
        // Фракция
        builder.addField("Защита", supply.getDefendersDisplay(true), true);
        // Время
        builder.addField("Время поставки", supply.time, true);
        // Кол-во
        builder.addField("Количество в заказе", String.valueOf(supply.amount), true);
        // Нападавшие
        builder.addField("Нападают", supply.attackers != null ? supply.getAttackersDisplay(true) : "—", true);
        // Карта
        builder.addField("Карта", Settings.capitalizeFirst(supply.map), true);
        // Результат
        builder.addField("Итог", supply.result != null ? supply.result : "—", true);
        // AFK
        if (supply.afk) {
            builder.addField("AFK", "Поставка была заказана как AFK!", false);
        }
        return builder.build();
    }

    public static final String EMS_ICON = "🚑";
    public static final String SPANK_ICON = "\uD83D\uDC8A";
    public static final String NG_ICON = "\uD83D\uDE9B";
    public static final String DEFAULT_ICON = "🚚";

    @NotNull
    @Contract(pure = true)
    private String getSupplyIcon(@NotNull SupplyManager.SupplyType type) {
        return switch (type) {
            case EMS -> EMS_ICON; // Скорая помощь
            case NG -> NG_ICON;  // Армия / военная поставка (шлем)

            case SPANK_MM, SPANK_LCN, SPANK_RM, SPANK_YAK, SPANK_AM -> SPANK_ICON ; // Спанк — чемоданчик/контрабанда

            default -> DEFAULT_ICON; // Стандартный грузовик
        };
    }


    @NotNull
    private Color getColorForType(@NotNull SupplyManager.SupplyType type) {
        return type.faction().color();
    }

    @NotNull
    private MessageCreateData buildMessage(SupplyManager.Supply supply) {
        MessageEmbed embed = buildEmbed(supply);
        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setEmbeds(embed);
        return builder.build();
    }

    private void updateEmbed(String messageID, SupplyManager.Supply supply) {
        SupplyManager.SUPPLY_CHANNEL.retrieveMessageById(messageID)
                .queue(message -> {
                    MessageEditBuilder builder = new MessageEditBuilder();
                    builder.setEmbeds(buildEmbed(supply));
                    String content = String.format("<@&%s> <@&%s>", CRIME_ROLE_ID, STATE_ROLE_ID);
                    builder.setContent(content);
                    message.editMessage(builder.build()).queue();
                });
    }
}
