package ru.SorestForest;

import java.time.LocalTime;
import java.time.ZoneId;

public class Settings {


    public static final String SELF_ID = "1401646665676620018";

    public static final String SUPPLY_CHANNEL_ID = "1328810240862322743";
    public static final String NEWS_CHANNEL_ID = "1393012130390741002";

    public static final String SUPPLY_ROLE_ID = "1392990918394515497";
    public static final String MODERATOR_ROLE_ID = "1328820426767466557";
    public static final String DEPLEADER_ROLE_ID = "1328810239784652871";
    public static final String CRIME_ROLE_ID = "1328810239750836247";
    public static final String STATE_ROLE_ID = "1329419976506871888";
    public static final String DEVELOPER_ROLE_ID = "1403101800512491712";

    public static String LSV_ID = "1329421067910910043";
    public static String ESB_ID = "1329421377937080412";
    public static String FAM_ID = "1328810239750836250";
    public static String MG13_ID = "1328810239750836249";
    public static String BSG_ID = "1328810239750836251";
    public static String MM_ID = "1329421773934034995";
    public static String AM_ID = "1329422114616246282";
    public static String LCN_ID = "1329421596775157780";
    public static String YAK_ID = "1328810239763415126";
    public static String RM_ID = "1329421885200666644";
    public static String LSSD_ID = "1329420291469738046";
    public static String LSPD_ID = "1328810239763415123";
    public static String FIB_ID = "1329420488275132416";
    public static String GOV_ID = "1329420835458519101";
    public static String NG_ID = "1329419894818869369";
    public static String EMS_ID = "1328831030643392542";
    public static String SASPA_ID = "1328810239750836252";

    public static final String NG_COMMAND = "поставка-нг";
    public static final String EMS_COMMAND = "поставка-емс";
    public static final String SPANK_COMMAND = "поставка-спанк";
    public static final String ROLL_COMMAND = "ролл";
    public static final String RESULT_COMMAND = "результат";
    public static final String STATS_COMMAND = "статистика";

    public static LocalTime getMoscowTime() {
        return LocalTime.now(ZoneId.of("Europe/Moscow"));
    }

    public static boolean isBetween(int hours, int minutes, int lh, int lm, int rh, int rm) {
        LocalTime left = LocalTime.of(lh, lm);
        LocalTime right = LocalTime.of(rh, rm);
        LocalTime now = LocalTime.of(hours, minutes);
        return !now.isBefore(left) && !now.isAfter(right);
    }

    public static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    /*
    * Код ошибки 01 - Slash Command Handler
    * Код ошибки 02 - Slash Command Handler
    * */
}
