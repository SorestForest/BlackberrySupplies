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
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.net.Proxy;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static ru.SorestForest.Settings.*;

public class SlashCommandHandler extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ng" -> handleStandardSupply(event, SupplyManager.SupplyType.NG);
            case "ems" -> handleStandardSupply(event, SupplyManager.SupplyType.EMS);
            case "spank" -> handleSpankSupply(event);
            case "rollresult" -> handleRollResult(event);
            case "result" -> handleResult(event);
            case "update" -> handleUpdate(event);
            case "stats" -> handleStats(event);
            case "save" -> handleSave(event);
            case "clearmembers" -> handleCleanMembers(event);
            case "cancel" -> handleCancel(event);
            default -> System.err.println("ERROR COMMAND");
        }
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

    private void handleStandardSupply(SlashCommandInteractionEvent event, SupplyManager.SupplyType type) {
        event.deferReply(false).queue();
        if (validatePermissions(event)) return;
        if (!MemberUtils.isInFaction(event.getMember(), type == SupplyManager.SupplyType.NG ? MemberUtils.Faction.NG : MemberUtils.Faction.EMS)) {
            event.getHook().sendMessage("Отпись поставок NG и EMS невозможно без соответствующей роли").queue();
            return;
        }
        String faction = event.getOption("faction", OptionMapping::getAsString);
        String time = event.getOption("time", OptionMapping::getAsString);
        int amount = event.getOption("amount", OptionMapping::getAsInt);
        assert time != null;
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        if (Settings.isBetween(hours, minutes, 18, 30, 19, 30)) {
            event.getHook().sendMessage("В данное время нельзя заказать поставку.").queue();
            return;
        }
        SupplyManager.Supply supply = SupplyManager.newSupply(type, time, amount, faction);
        boolean afkStatus = Boolean.TRUE.equals(event.getOption("afk", OptionMapping::getAsBoolean));
        if (afkStatus) {
            supply.afk = true;
            supply.result = "";
        }
        int check = validateTime(event, time, supply);
        if (check == 0 && !MemberUtils.isModerator(Objects.requireNonNull(event.getMember()))) return;
        event.getHook().sendMessage(buildMessage(supply, check == 1)).queue(sentMessage -> {
            SupplyManager.registerSupply(sentMessage.getId(), supply);
            assert faction != null;
            sentMessage.createThreadChannel(type.name().toLowerCase() + "-" + faction.toLowerCase())
                    .queue(thread -> thread.sendMessage("Ветка создана для обсуждения поставки " + type.name() + " для " + faction + ".").queue());
        });
    }

    private void handleSpankSupply(SlashCommandInteractionEvent event) {
        event.deferReply(false).queue();
        if (validatePermissions(event)) return;
        boolean hasMafiaRole = false;
        for (MemberUtils.Faction f : MemberUtils.Faction.values()) {
            if (f.isMafia() && MemberUtils.isInFaction(Objects.requireNonNull(event.getMember()), f)) {
                hasMafiaRole = true;
                break;
            }
        }
        if (!hasMafiaRole) {
            event.getHook().sendMessage("Отпись поставок SPANK невозможно без соответствующей роли любой мафии.").queue();
            return;
        }
        String faction = event.getOption("faction", OptionMapping::getAsString);
        String time = event.getOption("time", OptionMapping::getAsString);
        int amount = event.getOption("amount", OptionMapping::getAsInt);
        String dest = event.getOption("destination", OptionMapping::getAsString);
        assert faction != null;
        SupplyManager.SupplyType type = resolve(faction);
        if (type == null) {
            event.getHook().sendMessage("Неверно указана фракция заказчика.").queue();
            return;
        }
        if (!MemberUtils.isFaction(dest, true)) {
            event.getHook().sendMessage("Неверно указана фракция назначения").queue();
            return;
        }

        SupplyManager.Supply supply = SupplyManager.newSupply(type, time, amount, dest);
        boolean afkStatus = Boolean.TRUE.equals(event.getOption("afk", OptionMapping::getAsBoolean));
        if (afkStatus) {
            supply.afk = true;
            supply.result = "";
        }
        int check = validateTime(event, time, supply);
        if (check == 0 && !MemberUtils.isModerator(Objects.requireNonNull(event.getMember()))) return;
        event.getHook().sendMessage(buildMessage(supply, check == 1)).queue(sentMessage -> {
            SupplyManager.registerSupply(sentMessage.getId(), supply);
            sentMessage.createThreadChannel("spank-" + faction.toLowerCase() + "(" + dest + ")")
                    .queue(thread -> thread.sendMessage("Ветка создана для обсуждения поставки SPANK для " + dest + " от " + faction + ".").queue());
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
            supply.attack = attack;
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

            supply.winner = winner;
            supply.result = result;
            supply.ended = true;
            updateEmbed(parentMessage.getId(), supply);
            event.getHook().sendMessage("Данные о поставке обновлены, GG, WP!").queue();
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
                case "faction" -> supply.faction = value;
                case "time" -> supply.time = value;
                case "amount" -> {
                    try {
                        assert value != null;
                        supply.amount = Integer.parseInt(value);
                    } catch (Exception e) {
                        event.getHook().sendMessage("Должно быть числом для параметра amount!").queue();
                        return;
                    }
                }
                case "map" -> supply.map = value;
                case "attack" -> supply.attack = value;
                case "result" -> {
                    supply.result = value;
                    supply.afk = value != null && value.toLowerCase().contains("afk");
                }
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
    private final HashMap<String, Pair<String,String>> cleanMembersConfirmations = new HashMap<>();
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
        cleanMembersConfirmations.put(event.getUser().getId(), Pair.of(faction.name(),userID));
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
            String factionName = cleanMembersConfirmations.get(userId).getLeft();
            String leaderID = cleanMembersConfirmations.get(userId).getRight();
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
        String statsMessage = SupplyStats.calculateStats(faction);
        event.reply(statsMessage).queue();
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
        if (supply.type == SupplyManager.SupplyType.NG) builder.setColor(new Color(30, 105, 46));
        else if (supply.type == SupplyManager.SupplyType.EMS) builder.setColor(new Color(201, 2, 25));
        else builder.setColor(new Color(0, 183, 141));
        builder.setTitle("Поставка " + supply.type.displayName() + " // Blackberry");
        builder.setDescription(
                "Фракция: " + supply.faction + "\n" +
                        "Время поставки: " + supply.time + "\n" +
                        "Количество в заказе: " + supply.amount + "\n" +
                        "Карта: " + supply.map + "\n" +
                        "Нападают: " + supply.attack + "\n" +
                        "Итог: " + supply.result
        );
        if (supply.afk) {
            builder.appendDescription("\nАФК Поставка");
        }
        return builder.build();
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
