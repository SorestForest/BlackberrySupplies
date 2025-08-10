package ru.SorestForest;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import static ru.SorestForest.Settings.STATS_COMMAND;


public class BotStarter {

    public static JDA API;

    public static void startBot() throws InterruptedException {
        System.out.println("Starting API...");
        API = JDABuilder.createDefault(System.getProperty("token"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(new SlashCommandHandler())
                .build();
        CommandListUpdateAction commnads = API.updateCommands();
        commnads.addCommands(Commands.slash(Settings.NG_COMMAND, "Отпись правительственных поставок материалов")
                                .addOptions(factionGov()) // destinatnion
                                .addOption(OptionType.STRING,"defenders","Фракции стороны защиты", true)
                                .addOption(OptionType.STRING, "time", "Время отписи поставки", true)
                                .addOption(OptionType.INTEGER, "amount", "Количество материалов для заказа (стандарт - 20.000)", true)
                        .addOptions(new OptionData(OptionType.STRING,"afk","[Дополнительно] Заказать АФК поставку")
                                .addChoice("AFK","AFK")
                                .addChoice("Боевая","Боевая"))
                                .setContexts(InteractionContextType.GUILD),

                        Commands.slash(Settings.EMS_COMMAND, "Отпись правительственных поставок аптек")
                                .addOptions(factionGov()) // destination
                                .addOption(OptionType.STRING,"defenders","Фракции стороны защиты", true)
                                .addOption(OptionType.STRING, "time", "Время отписи поставки", true)
                                .addOption(OptionType.INTEGER, "amount", "Количество аптек для заказа (стандарт - 1500)", true)
                                .addOptions(new OptionData(OptionType.STRING,"afk","[Дополнительно] Заказать АФК поставку")
                                        .addChoice("AFK","AFK")
                                        .addChoice("Боевая","Боевая"))
                                .setContexts(InteractionContextType.GUILD),

                        Commands.slash(Settings.SPANK_COMMAND, "Отпись крайм поставок анальгетиков")
                                .addOptions(factionCrime()) // fraction
                                .addOption(OptionType.STRING, "destination", "Фракция, куда РАЗГРУЖАЕТСЯ поставка", true)
                                .addOption(OptionType.STRING,"defenders","Фракции стороны защиты", true)
                                .addOption(OptionType.STRING, "time", "Время отписи поставки", true)
                                .addOption(OptionType.INTEGER, "amount", "Количество спанка для заказа (стандарт - 1000)", true)
                                .addOptions(new OptionData(OptionType.STRING,"afk","[Дополнительно] Заказать АФК поставку")
                                        .addChoice("AFK","AFK")
                                        .addChoice("Боевая","Боевая"))
                                .setContexts(InteractionContextType.GUILD))
                .addCommands(Commands.slash(Settings.ROLL_COMMAND,"Устанавливает карту или фракцию нападения")
                        .addOption(OptionType.STRING,"map","Карта розыгрыша поставки", false)
                        .addOption(OptionType.STRING, "attack","Фракция нападения", false)
                        .setContexts(InteractionContextType.GUILD))
                .addCommands(Commands.slash(Settings.RESULT_COMMAND,"Установить результат и победителя на данной поставке")
                        .addOptions(new OptionData(OptionType.STRING,"winner","Кто победил в поставке?",true)
                                .addChoice("Защита","Защита")
                                .addChoice("Атака","Атака"))
                        .addOption(OptionType.STRING, "result", "Описание результата поставки.", true)
                        .setContexts(InteractionContextType.GUILD))
                .addCommands(Commands.slash("update", "[Модератор] Обновить принудительно данные о любой поставке")
                        .addOption(OptionType.STRING,"key","time, destination, amount, defenders, map, result, attackers, afk, defenderWin, ended", true)
                        .addOption(OptionType.STRING,"value","Новое значение параметра поставки", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)))
                .addCommands(Commands.slash(STATS_COMMAND, "Статистика по фракции")
                        .addOptions(allFactionsOption())
                        .addOptions(dateOption()))
                .addCommands(Commands.slash("save","[Модератор] Сохранет статистику поставок"))
                .addCommands(Commands.slash("clearmembers","[Модератор] Снимает лидера и все роли его состава")
                        .addOption(OptionType.STRING,"faction","Фракция для снятия", true)
                        .addOption(OptionType.USER,"leader","Лидер для снятия и уведомления", true))
                .addCommands(Commands.slash("cancel","[Модератор] Отменяет данную поставку, удаляет данные из базы."))
                .addCommands(Commands.slash("помощь","Показать информацию об использовании бота").setContexts(InteractionContextType.GUILD))
                .addCommands(Commands.slash("dump-data", "[Модератор] Выписать много разной информции о поставке."))
                .addCommands(Commands.slash("авто-ролл","Команда для автоматического выбора карт и поставок случайным образои")
                        .addSubcommands(new SubcommandData("карта","Разыгрывает карту для поставки среди предоставленных")
                                        .addOption(OptionType.STRING,"maps","Карты для розыгрыша, писать через запятую.", true),
                                new SubcommandData("фракция","Выбирает случайным образом фракцию").addOption(OptionType.STRING,"factions",
                                        "Фракции для рола на поставку через запятую, союзы - слитно через плюс. Пример: am, lcn, bsg+esb", true)))
                .addCommands(Commands.slash("карта","Управление картами").addSubcommands(
                                new SubcommandData("управление","[Модератор] Добавить или удалить карту")
                                        .addOption(OptionType.STRING,"map","название карты",true),
                                new SubcommandData("список","Список всех текущих карт")
                        ))
                .addCommands(Commands.slash("whoami","who am i??"))
                .queue();
        API.awaitReady();
        MemberUtils.setup();
        SupplyManager.SUPPLY_CHANNEL = API.getTextChannelById(Settings.SUPPLY_CHANNEL_ID);
        SupplyManager.loadData();
        MapUtils.load();
        Runtime.getRuntime().addShutdownHook(new Thread(SupplyManager::saveData));
    }

    private static OptionData allFactionsOption() {
        OptionData data = new OptionData(OptionType.STRING,"faction","Тип статистики для просмотра.", true);
        data.addChoice("General","General");
        data.addChoice("Map","Map");
        for (MemberUtils.Faction value : MemberUtils.Faction.values()) {
            data.addChoice(value.name(), value.name());
        }
        return data;
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

    public static OptionData dateOption() {
        OptionData option = new OptionData(OptionType.STRING, "period", "Период статистики", true);
        option.addChoice("месяц","месяц");
        option.addChoice("неделя","неделя");
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

}