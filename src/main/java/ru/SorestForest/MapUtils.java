package ru.SorestForest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class MapUtils {
    private static final Set<String> allMaps = new HashSet<>();
    private static final File FILE = new File("saved" + File.separator + "maps.json");
    private static final Gson gson = new Gson();


    // Добавить карту в общий список (если новой карты раньше не было)
    public static void addMap(String map) {
        allMaps.add(map.trim());
        save();
    }

    public static void removeMap(String map) {
        allMaps.remove(map);
        save();
    }

    public static void handleMapCommand(SlashCommandInteractionEvent event) {
        event.deferReply(false).queue();
        if (Objects.equals(event.getSubcommandName(), "управление")) {
            if (!MemberUtils.isModerator(Objects.requireNonNull(event.getMember()))) {
                event.getHook().sendMessage("Только модераторам доступно управление картами").queue();
                return;
            }
            String map = event.getOption("map", OptionMapping::getAsString);
            assert map != null;
            if (allMaps.contains(map.toLowerCase())) {
                removeMap(map.toLowerCase());
                event.getHook().sendMessage("Удалена карта "+map).queue();
            } else {
                addMap(map.toLowerCase());
                event.getHook().sendMessage("Добавлена карта "+map).queue();
            }
        } else {
            event.getHook().sendMessage("Список карт: " + String.join(", ", allMaps)).queue();
        }
    }

    public static boolean areAllMapsValid(List<String> maps) {
        for (String map : maps) {
            if (!allMaps.contains(map.trim().toLowerCase())) {
                return false;
            }
        }
        return true;
    }


    static void load() {
        if (!FILE.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                FILE.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (FileReader reader = new FileReader(FILE)) {
            Type type = new TypeToken<Map<String, Set<String>>>(){}.getType();
            Map<String, Set<String>> data = gson.fromJson(reader, type);
            if (data == null) {
                System.out.println("Карт не найдено!");
                return;
            }
            allMaps.clear();
            allMaps.addAll(data.getOrDefault("all", new HashSet<>()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        Map<String, Set<String>> data = new HashMap<>();
        data.put("all", allMaps);

        try (FileWriter writer = new FileWriter(FILE)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
