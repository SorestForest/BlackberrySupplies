package ru.SorestForest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class SupplyManager {

    public static final HashMap<String, Supply> data = new HashMap<>();

    public static TextChannel SUPPLY_CHANNEL;
    public static TextChannel NEWS_CHANNEL;

    public static void registerSupply(String messageID, Supply supply) {
        data.put(messageID, supply);
    }

    public static Supply getByMessageID(String id) {
        return data.getOrDefault(id, null);
    }

    public static void saveData() {
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        // 1. Создаём папку ./saved, если её нет
        File dir = new File("saved");
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                System.err.println("Не удалось создать папку для сохранения.");
                return;
            }
        }

        // 2. Формируем имя файла по текущей дате и времени
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "saved/supply_" + timestamp + ".json";

        // 3. Сохраняем JSON
        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(data, writer);
            System.out.println("Поставки успешно сохранены в файл: " + fileName);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении данных: " + e.getMessage());
        }
    }

    public static void loadData() {
        File dir = new File("saved");
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Папка saved не найдена.");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdir();
            return;
        }

        deleteOldFiles(dir);

        File[] files = dir.listFiles((d, name) -> name.startsWith("supply_") && name.endsWith(".json"));
        if (files == null || files.length == 0) {
            System.out.println("Файлы сохранения не найдены (после очистки).");
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::lastModified).reversed());
        File latestFile = files[0];

        try (FileReader reader = new FileReader(latestFile)) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();

            Type type = new TypeToken<HashMap<String, Supply>>() {}.getType();
            HashMap<String, Supply> loadedData = gson.fromJson(reader, type);

            if (loadedData != null) {
                data.clear();
                data.putAll(loadedData);
                System.out.println("Загружено " + data.size() + " поставок за сегодня из файла: " + latestFile.getName());
            } else {
                System.err.println("Файл пуст или повреждён: " + latestFile.getName());
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void deleteOldFiles(File dir) {
        Instant now = Instant.now();
        File[] files = dir.listFiles((d, name) -> name.startsWith("supply_") && name.endsWith(".json"));
        if (files == null) return;

        int deletedCount = 0;
        for (File file : files) {
            try {
                Instant fileTime = Files.getLastModifiedTime(file.toPath()).toInstant();
                if (ChronoUnit.DAYS.between(fileTime, now) > 31) {
                    if (file.delete()) {
                        deletedCount++;
                        System.out.println("Удалён устаревший файл: " + file.getName());
                    }
                }
            } catch (IOException e) {
                System.err.println("Ошибка при удалении файла: " + file.getName());
                e.printStackTrace();
            }
        }

        if (deletedCount > 0) {
            System.out.println("Удалено старых файлов: " + deletedCount);
        }
    }


    public static class Supply {
        public SupplyType type;
        public String time;
        public int amount;
        public MemberUtils.Faction destination;
        public List<MemberUtils.Faction> defenders; // Изменено здесь
        public String map;
        public String result;
        public List<MemberUtils.Faction> attackers;
        public boolean ended;
        public boolean afk;
        public Boolean defenderWin;
        public LocalDateTime timeEnded;

        public Supply(SupplyType type, String time, int amount, List<MemberUtils.Faction> defenders, MemberUtils.Faction destination) {
            this.type = type;
            this.time = time;
            this.destination = destination;
            this.amount = amount;
            this.defenders = defenders;
            this.map = "Не выбрана";
            this.result = "В процессе..";
            this.attackers = new ArrayList<>();
            this.ended = false;
            this.afk = false;
            this.defenderWin = null;
        }

        public String getDefendersDisplay(boolean newLine) {
            return displayFactions(defenders, newLine);
        }
        public String getAttackersDisplay(boolean newLine) {
            return displayFactions(attackers, newLine);
        }

        private static String displayFactions(List<MemberUtils.Faction> factions, boolean newLine) {
            return factions.stream()
                    .map(MemberUtils.Faction::displayName)
                    .reduce((a, b) -> a + (newLine ? "\n" : " ") + b)
                    .orElse("Не указано");
        }

    }



    public enum SupplyType {
        EMS, NG, SPANK_MM, SPANK_LCN, SPANK_RM, SPANK_YAK, SPANK_AM, SPANK_BLANK;

        @NotNull
        public MemberUtils.Faction faction() {
            return switch(this) {
                case NG -> MemberUtils.Faction.NG;
                case EMS -> MemberUtils.Faction.EMS;
                case SPANK_MM -> MemberUtils.Faction.MM;
                case SPANK_LCN -> MemberUtils.Faction.LCN;
                case SPANK_RM -> MemberUtils.Faction.RM;
                case SPANK_YAK -> MemberUtils.Faction.YAK;
                case SPANK_AM -> MemberUtils.Faction.AM;
                case SPANK_BLANK -> MemberUtils.Faction.LSPD;
            };
        }

        public String displayName(){
            return toString().replace("_"," ");
        }

    }

}
