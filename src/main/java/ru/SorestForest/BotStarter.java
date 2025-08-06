package ru.SorestForest;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import static ru.SorestForest.Settings.STATS_COMMAND;


public class BotStarter {

    public static JDA API;

    public static void startBot() throws InterruptedException {
        System.out.println("Starting API...");
        API = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(new SlashCommandHandler())
                .build();
        CommandListUpdateAction commnads = API.updateCommands();
        commnads.addCommands(Commands.slash(Settings.NG_COMMAND, "Отпись правительственных поставок материалов")
                                .addOptions(factionGov()) // destinatnion
                                .addOption(OptionType.STRING,"defenders","Фракции стороны защиты", true)
                                .addOption(OptionType.STRING, "time", "Время отписи поставки", true)
                                .addOption(OptionType.INTEGER, "amount", "Количество материалов для заказа (стандарт - 20.000)", true)
                        .addOption(OptionType.BOOLEAN,"afk","[Дополнительно] Заказать АФК поставку (выбрать true)")
                                .setContexts(InteractionContextType.GUILD),

                        Commands.slash(Settings.EMS_COMMAND, "Отпись правительственных поставок аптек")
                                .addOptions(factionGov()) // destination
                                .addOption(OptionType.STRING,"defenders","Фракции стороны защиты", true)
                                .addOption(OptionType.STRING, "time", "Время отписи поставки", true)
                                .addOption(OptionType.INTEGER, "amount", "Количество аптек для заказа (стандарт - 1500)", true)
                                .addOption(OptionType.BOOLEAN,"afk","[Дополнительно] Заказать АФК поставку (выбрать true)")
                                .setContexts(InteractionContextType.GUILD),

                        Commands.slash(Settings.SPANK_COMMAND, "Отпись крайм поставок анальгетиков")
                                .addOptions(factionCrime()) // fraction
                                .addOption(OptionType.STRING, "destination", "Фракция, куда РАЗГРУЖАЕТСЯ поставка", true)
                                .addOption(OptionType.STRING,"defenders","Фракции стороны защиты", true)
                                .addOption(OptionType.STRING, "time", "Время отписи поставки", true)
                                .addOption(OptionType.INTEGER, "amount", "Количество спанка для заказа (стандарт - 1000)", true)
                                .addOption(OptionType.BOOLEAN,"afk","[Дополнительно] Заказать АФК поставку (выбрать true)")
                                .setContexts(InteractionContextType.GUILD))
                .addCommands(Commands.slash(Settings.ROLL_COMMAND,"Устанавливает карту и фракцию нападения")
                        .addOption(OptionType.STRING,"map","Карта розыгрыша поставки", true)
                        .addOption(OptionType.STRING, "attack","Фракция нападения", true)
                        .setContexts(InteractionContextType.GUILD))
                .addCommands(Commands.slash(Settings.RESULT_COMMAND,"Установить результат и победителя на данной поставке")
                        .addOption(OptionType.BOOLEAN, "winner", "Победила ли сторона защиты? (true - защита выиграла, false - атака)", true)
                        .addOption(OptionType.STRING, "result", "Описание результата поставки.", true)
                        .setContexts(InteractionContextType.GUILD))
                .addCommands(Commands.slash("update", "[Модератор] Обновить принудительно данные о любой поставке")
                        .addOption(OptionType.STRING,"key","Значение для обновления: faction/time/amount/map/attack/result")
                        .addOption(OptionType.STRING,"value","Новое значение параметра поставки")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                        )
                .addCommands(Commands.slash(STATS_COMMAND, "Статистика по фракции")
                        .addOption(OptionType.STRING,"faction","Фракция для просмотра статистики"))
                .addCommands(Commands.slash("save","[Модератор] Сохранет статистику поставок").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)))
                .addCommands(Commands.slash("clearmembers","[Модератор] Снимает лидера и все роли его состава")
                        .addOption(OptionType.STRING,"faction","Фракция для снятия")
                        .addOption(OptionType.USER,"leader","Лидер для снятия и уведомления")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)))
                .addCommands(Commands.slash("cancel","[Модератор] Отменяет данную поставку, удаляет данные из базы.")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)))
                .addCommands(Commands.slash("помощь","Показать информацию об использовании бота").setContexts(InteractionContextType.GUILD))
                .addCommands(Commands.slash("dump-data", "[Модератор] Выписать много разной информции о поставке."))
                .queue();
        API.awaitReady();
        MemberUtils.setup();
        SupplyManager.SUPPLY_CHANNEL = API.getTextChannelById(Settings.REPORT_CHANNEL_ID);
        SupplyManager.NEWS_CHANNEL = API.getTextChannelById(Settings.NEWS_CHANNEL_ID);
        SupplyManager.loadData();
        Runtime.getRuntime().addShutdownHook(new Thread(SupplyManager::saveData));
    }



    public static OptionData factionGov() {
        OptionData option = new OptionData(OptionType.STRING, "destination", "Фракция, в которую направляется поставка", true);
        for (MemberUtils.Faction faction : MemberUtils.Faction.values()) {
            if (isGovFaction(faction)) {
                option.addChoice(faction.name(), faction.name());
            }
        }
        return option;
    }

    public static OptionData factionCrime() {
        OptionData option = new OptionData(OptionType.STRING, "faction", "Фракция, которая ЗАКАЗЫВАЕТ (прокает) поставку", true);
        for (MemberUtils.Faction faction : MemberUtils.Faction.values()) {
            if (faction.isMafia()) {
                option.addChoice(faction.name(), faction.name());
            }
        }
        return option;
    }

    private static boolean isGovFaction(MemberUtils.Faction faction) {
        return switch(faction) {
            case NG, EMS, GOV, LSPD, FIB, LSSD, SASPA -> true;
            default -> false;
        };
    }

    private static boolean isCrimeFaction(MemberUtils.Faction faction) {
        return switch(faction) {
            case MM, RM, AM, LCN, YAK, FAM, LSV, ESB, BSG, MG13 -> true;
            default -> false;
        };
    }
}