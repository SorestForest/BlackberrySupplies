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
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static ru.SorestForest.Settings.*;

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
            default -> System.err.println("ERROR COMMAND");
        }
    }

    private void handleHelpCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder help = new EmbedBuilder();
        help.setTitle("📘 Помощь по командам бота");
        help.setDescription("Ниже приведены все доступные команды и их назначение.");
        help.setColor(Color.CYAN);

        help.addField("/поставка нг", """
        📦 Заказ материалов **армии (NG)** для гос. структур. Используется только армией.
        **Параметры:**
        • `faction` — фракция назначения (гос)
        • `time` — время поставки (в формате HH:MM)
        • `amount` — количество материалов
        • `afk` — [необязательно] заказать как AFK-поставку, надо указать True
        """, false);

        help.addField("/поставка емс", """
        💉 Заказ аптечек **медслужбы (EMS)** для гос. структур. Используется только EMS
        **Параметры:**
        • `faction` — фракция назначения (гос)
        • `time` — время поставки
        • `amount` — количество аптек
        • `afk` — [необязательно] заказать как AFK-поставку, надо указать True
        """, false);

        help.addField("/поставка спанк", """
        💼 Заказ спанка (анальгетиков) для **крайм-фракций**.
        **Параметры:**
        • `faction` — кто заказывает спанк (прокает его)
        • `destination` — куда разгружается спанк (играющая фракция, которая будет его дефать)
        • `time` — время поставки
        • `amount` — количество спанка
        • `afk` — [необязательно] заказать как AFK-поставку, надо указать True
        """, false);

        help.addField("/ролл", """
        🎲 Установить **карту** и **фракцию нападения** на активную поставку.
        **Параметры:**
        • `map` — название карты
        • `attack` — фракция, совершающая атаку. Можно указывать союзы!
        """, false);

        help.addField("/результат", """
        🏁 Установить **результат поставки**.
        **Параметры:**
        • `winner` — победила ли защита. Если защита выиграла, указываете true, иначе - false.
        • `result` — описание результата
        """, false);

        help.addField("/статистика", """
        📊 Посмотреть статистику поставок **по одной фракции** за последние 7 дней.
        **Параметры:**
        • `faction` — название фракции (напр. LSSD, MM или любая другая)
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
                    .anyMatch(f -> f.isMafia() && MemberUtils.isInFaction(member, f));
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

        // Проверка ВЗХ
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        if (Settings.isBetween(hours, minutes, 18, 30, 19, 30) && !isSpank) {
            event.getHook().sendMessage("В данное время нельзя заказать поставку в связи с ВЗХ.").queue();
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

        SupplyManager.Supply supply = new SupplyManager.Supply(type, time, amount, defendersStr, MemberUtils.toFaction(destination));
        supply.defenders = defenderFactions.r;

        boolean afkStatus = Boolean.TRUE.equals(event.getOption("afk", OptionMapping::getAsBoolean));
        if (afkStatus) {
            supply.afk = true;
            supply.result = "";
        }

        int check = validateTime(event, time, supply);
        if (check == 0 && !MemberUtils.isModerator(Objects.requireNonNull(member))) return;


        SupplyManager.SupplyType finalType = type;
        event.getHook().sendMessage(buildMessage(supply, check == 1)).queue(sentMessage -> {
            SupplyManager.registerSupply(sentMessage.getId(), supply);
            String threadName = isSpank
                    ? "spank-" + defendersStr.toLowerCase() + "(" + destination + ")"
                    : finalType.name().toLowerCase() + "-" + destination.toLowerCase();
            sentMessage.createThreadChannel(threadName).queue(thread -> {
                thread.sendMessage("Ветка создана для обсуждения поставки " + finalType.displayName() + " для " + destination +
                        (isSpank ? " от " + defendersStr : ". Защищают: " + supply.getDefendersDisplay(false)) + ".").queue();

                if (supply.afk) {
                    String afkMention = isSpank ? defendersStr : destination;
                    thread.sendMessage("⚠️ Поставка была заказана как **AFK**. У фракции заказчика и **" + afkMention +
                            "** есть **5 минут** на указание причины AFK-поставки, иначе она будет считаться **заказанной не по правилам**. Ссылку можно указать с помощью команды /результат, winner указывайте как True.").queue();
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

            Boolean winner = event.getOption("winner", OptionMapping::getAsBoolean);
            String result = event.getOption("result", OptionMapping::getAsString);

            if (winner == null || result == null) {
                event.getHook().sendMessage("Параметры winner и result обязательны.").setEphemeral(true).queue();
                return;
            }

            supply.defenderWin = winner;
            supply.result = result;
            supply.ended = true;
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

            switch (Objects.requireNonNull(key)) {
                case "time" -> {
                    supply.time = value;
                }
                case "destination" -> {
                    supply.destination = MemberUtils.toFaction(value);
                }
                case "amount" -> {
                    assert value != null;
                    supply.amount = Integer.parseInt(value);
                }
                case "defenders" -> {
                    supply.defenders = MemberUtils.parseFactions(value).r;
                }
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
        String faction = event.getOption("faction", OptionMapping::getAsString);
        MessageEmbed statsEmbed = SupplyStats.calculateStats(faction);
        event.replyEmbeds(statsEmbed).queue();
    }

    private boolean validatePermissions(SlashCommandInteractionEvent event) {
        if (!MemberUtils.isSupplier(Objects.requireNonNull(event.getMember())) && !MemberUtils.isModerator(event.getMember())) {
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
        if (!MemberUtils.isInFaction(Objects.requireNonNull(event.getMember()), supply.type.faction()) && !MemberUtils.isModerator(event.getMember())) {
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
        builder.addField("Карта", supply.map, true);
        // Результат
        builder.addField("Итог", supply.result != null ? supply.result : "—", true);
        // AFK
        if (supply.afk) {
            builder.addField("AFK", "Поставка была заказана как AFK!", false);
        }
        return builder.build();
    }

    private String getSupplyIcon(SupplyManager.SupplyType type) {
        return switch (type) {
            case EMS -> "🚑"; // Скорая помощь
            case NG -> "\uD83D\uDE9B";  // Армия / военная поставка (шлем)

            case SPANK_MM, SPANK_LCN, SPANK_RM, SPANK_YAK, SPANK_AM -> "\uD83D\uDC8A"; // Спанк — чемоданчик/контрабанда

            default -> "🚚"; // Стандартный грузовик
        };
    }


    private Color getColorForType(SupplyManager.SupplyType type) {
        return switch (type) {
            case EMS -> new Color(201, 2, 25);        // Ярко-красный (медики)
            case NG -> new Color(30, 105, 46);        // Армейский зелёный

            case SPANK_MM -> new Color(0, 128, 0);    // Яркий зелёный (Mexican Mafia)
            case SPANK_LCN -> new Color(218, 165, 32); // Goldenrod (La Cosa Nostra)
            case SPANK_RM -> new Color(47, 79, 79);   // Dark slate gray
            case SPANK_YAK -> new Color(128, 0, 0);   // Тёмно-красный
            case SPANK_AM -> new Color(139, 69, 19);   // Индиго

            default -> new Color(0, 183, 141);        // Fallback — бирюзовый
        };
    }



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

    private SupplyManager.SupplyType resolve(String faction) {
        return switch (faction.toUpperCase()) {
            case "MM" -> SupplyManager.SupplyType.SPANK_MM;
            case "AM" -> SupplyManager.SupplyType.SPANK_AM;
            case "YAK" -> SupplyManager.SupplyType.SPANK_YAK;
            case "RM" -> SupplyManager.SupplyType.SPANK_RM;
            case "LCN" -> SupplyManager.SupplyType.SPANK_LCN;
            default -> null;
        };
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
