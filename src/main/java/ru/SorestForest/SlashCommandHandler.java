package ru.SorestForest;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
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
            case "clearmembers" -> handleCleanMembers(event);
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
            String chosenMap = Settings.capitalizeFirst(maps.get(new Random().nextInt(maps.size())));
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
            factionsStr = factionsStr.replace(",","");
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

            supply.attackers = factions.get(new Random().nextInt(factions.size()));



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
        • `defenders` — сторона защиты поставки. Пишется английскими буквами, союзы через запятую: lssd, fib
        • `time` — время поставки (в формате HH:MM)
        • `amount` — количество материалов для заказа
        • `afk` — [необязательно] если надо заказать как AFK-поставку, то надо указать True
        """, false);

        help.addField("/поставка-емс", """
        💉 Заказ аптечек **медслужбы (EMS)** для гос. структур. Используется только EMS
        **Параметры:**
        • `destination` — фракция назначения (гос) на выбор
        • `defenders` — сторона защиты поставки. Пишется английскими буквами, союзы через запятую: lssd, fib
        • `time` — время заказа поставки в формате HH:mm
        • `amount` — количество аптек для заказаа
        • `afk` — [необязательно] если надо заказать как AFK-поставку, то надо указать True
        """, false);

        help.addField("/поставка-спанк", """
        💼 Заказ спанка (анальгетиков) для **крайм-фракций**.
        **Параметры:**
        • `faction` — кто заказывает спанк (прокает его)
        • `destination` — куда разгружается поставка спанка
        • `defenders` — сторона защиты поставки. Пишется английскими буквами, союзы через запятую: am, lcn, yak
        • `time` — время заказа поставки в формате HH:mm
        • `amount` — количество спанка
        • `afk` — [необязательно] заказать как AFK-поставку, надо указать True
        """, false);

        help.addField("/ролл", """
        🎲 Установить **карту** и **фракцию нападения** на активную поставку.
        **Параметры:**
        • `map` — название карты
        • `attack` — фракция, совершающая атаку. Союзы указываются через запятую: am, lcn или fib, lssd
        """, false);

        help.addField("/результат", """
        🏁 Установить **результат поставки**.
        **Параметры:**
        • `winner` — победила ли защита. Если защита выиграла, указываете true, иначе - false.
        • `result` — описание результата - на ваше усмотрение
        """, false);

        help.addField("/статистика", """
        📊 Посмотреть статистику поставок **по одной фракции** за последние 7 дней или месяц.
        **Параметры:**
        • `faction` — название фракции (напр. LSSD, MM или любая другая)
        • `period` — период просмотра статистики
        """, false);
        help.addField("🔹 /авто-ролл карта", """
        Случайно выбрать карту из списка.
        **Параметры:**
        • `maps` — список карт через запятую.
        """, false);

        help.addField("🔹 /авто-ролл фракция", """
        Случайно выбрать атакующую фракцию.
        **Параметры:**
        • `factions` — список фракций через запятую;
        • союзы — указывать через `+`, пример: `bsg+esb`. Пример полной строки: `bsg+lcn, ems, fib'
        """, false);

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
        event.deferReply(false).queue();

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

        boolean afkStatus = Boolean.TRUE.equals(event.getOption("afk", OptionMapping::getAsBoolean));
        if (afkStatus) {
            supply.afk = true;
            supply.result = "";
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

        SupplyManager.SupplyType finalType = type;
        event.getHook().sendMessage(buildMessage(supply, check == 1)).queue(sentMessage -> {
            SupplyManager.registerSupply(sentMessage.getId(), supply);
            String threadName = isSpank
                    ? "spank-" + defendersStr.toLowerCase() + "(" + destination + ")"
                    : finalType.name().toLowerCase() + "-" + destination.toLowerCase();
            sentMessage.createThreadChannel(threadName).queue(thread -> {
                thread.sendMessage("Ветка создана для обсуждения поставки " + finalType.displayName() + " для " + destFaction.displayName()
                        + ". Защищают: "+supply.getDefendersDisplay(false)).queue();

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
            supply.map = map;
            ForestPair<Boolean,List<MemberUtils.Faction>> factions = MemberUtils.parseFactions(attack);
            if (factions.r.isEmpty() || !factions.l) {
                event.getHook().sendMessage("Не распознано ни одной фракции. Фракции перечисляются через запятую: AM, LCN или am, lcn").queue();
                return;
            }
            supply.attackers = factions.r;
            updateEmbed(parentMessage.getId(), supply);
            event.getHook().sendMessage("Данные о поставке обновлены! Удачной игры!").queue();
        });
    }

    private void handleResult(SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("Команда подразумевает использование в ветке поставки.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
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

    // Добавь поле для временного хранения выбора фракции для подтверждения
    private final HashMap<String, ForestPair<String,String>> cleanMembersConfirmations = new HashMap<>();
    // Ключ — ID пользователя (модератора), значение — фракция для очистки

    private void handleCleanMembers(SlashCommandInteractionEvent event) {
        if (!MemberUtils.isModerator(Objects.requireNonNull(event.getMember()))) {
            event.reply("Команда доступна только модераторам.").setEphemeral(true).queue();
            return;
        }

        String factionName = event.getOption("faction", OptionMapping::getAsString);
        if (factionName == null) {
            event.reply("Укажите фракцию.").setEphemeral(true).queue();
            return;
        }

        MemberUtils.Faction faction;
        try {
            faction = MemberUtils.Faction.valueOf(factionName.toUpperCase());
        } catch (IllegalArgumentException e) {
            event.reply("Неизвестная фракция: " + factionName).setEphemeral(true).queue();
            return;
        }
        String userID = event.getOption("leader",OptionMapping::getAsString);
        cleanMembersConfirmations.put(event.getUser().getId(), ForestPair.of(faction.name(),userID));
        event.reply("Вы уверены, что хотите снять роли фракции **" + faction.name() + "** у всех участников? Это действие нельзя отменить.")
                .addComponents(
                        ActionRow.of(Button.danger("cleanmembers_confirm", "Подтвердить"),
                                Button.secondary("cleanmembers_cancel", "Отмена"))
                )
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("cleanmembers_confirm")) {
            String userId = event.getUser().getId();
            if (!cleanMembersConfirmations.containsKey(userId)) {
                event.reply("Подтверждение не найдено или устарело. Повторите команду.").setEphemeral(true).queue();
                return;
            }
            String factionName = cleanMembersConfirmations.get(userId).l;
            String leaderID = cleanMembersConfirmations.get(userId).r;
            cleanMembersConfirmations.remove(userId);

            MemberUtils.Faction faction = MemberUtils.Faction.valueOf(factionName);
            Role factionRole = faction.asRole();
            if (factionRole == null) {
                event.reply("Роль фракции не найдена!").setEphemeral(true).queue();
                return;
            }

            Objects.requireNonNull(event.getGuild()).loadMembers().onSuccess(members -> {
                List<Member> filtered = members.stream().filter(m -> m.getUnsortedRoles().contains(factionRole)).toList();
                for (Member member : filtered) {
                    event.getGuild().removeRoleFromMember(member, factionRole).queue();
                    event.getGuild().removeRoleFromMember(member, MemberUtils.SUPPLY_ROLE).queue();
                    event.getGuild().removeRoleFromMember(member, MemberUtils.DEPLEADER_ROLE).queue();
                }

                event.reply("Роли фракции " + faction.name() + " успешно сняты у "+ filtered.size() + " игроков")
                        .setEphemeral(true).queue();
                sendLeaderRemovalMessage(SupplyManager.NEWS_CHANNEL,faction.displayName(),leaderID, factionRole.getId(),factionRole.getId(),MemberUtils.DEPLEADER_ROLE.getId(),MemberUtils.SUPPLY_ROLE.getId());
            });



        } else if (event.getComponentId().equals("cleanmembers_cancel")) {
            cleanMembersConfirmations.remove(event.getUser().getId());
            event.reply("Очистка ролей отменена.").setEphemeral(true).queue();
        }
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
        MessageEmbed statsEmbed = SupplyStats.calculateStats(faction, days);
        event.getHook().sendMessageEmbeds(statsEmbed).queue();
    }

    private boolean validatePermissions(SlashCommandInteractionEvent event) {
        if (!MemberUtils.isSupplier(Objects.requireNonNull(event.getMember()))) {
            event.getHook().sendMessage("Недостаточно прав для отписи поставки. Требуется роль отписи.").queue();
            return true;
        }
        if (!Settings.REPORT_CHANNEL_ID.equals(event.getChannelId())) {
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
        if (!MemberUtils.isInFaction(Objects.requireNonNull(event.getMember()), supply.type.faction())) {
            event.getHook().sendMessage("Данная поставка не ваша, обратитесь к " + supply.type.faction() + " для редакции информации.").queue();
            return null;
        }
        return supply;
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
    private MessageCreateData buildMessage(SupplyManager.Supply supply, boolean pingModerators) {
        MessageEmbed embed = buildEmbed(supply);
        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setEmbeds(embed);
        String message = String.format("<@&%s> <@&%s>", CRIME_ROLE_ID, STATE_ROLE_ID);
        if (pingModerators){
            message += " ";
            message += String.format("<@&%s>", MODERATOR_ROLE_ID);
        }
        builder.setContent(message);
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

    public static void sendLeaderRemovalMessage(TextChannel textChannel,
                                                String factionName,
                                                String userId,
                                                String factionRoleId,
                                                String crimeRoleId,
                                                String deputyRoleId,
                                                String supplyRoleId) {

        String message = String.format("""
                @everyone

                🎤 **Лидер %s** <@%s> успешно завершил свой путь на посту главы организации. Было много ярких моментов, активности и сильных решений, за что выражаем благодарность!

                💀 Роль <@&%s> была снята со всех участников.

                🧹 Также роли <@&%s>, <@&%s> и <@&%s> были удалены у всех, кто входил в состав **%s**.
                """,
                factionName,
                userId,
                factionRoleId,
                crimeRoleId,
                deputyRoleId,
                supplyRoleId,
                factionName
        );

        textChannel.sendMessage(message).queue();
    }
}
