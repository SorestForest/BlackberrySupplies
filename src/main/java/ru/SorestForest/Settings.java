package ru.SorestForest;

import java.time.LocalTime;
import java.time.ZoneId;

public class Settings {


    public static final String SELF_ID = "1401646665676620018";

    public static final String REPORT_CHANNEL_ID = "1401884769343504485";
    public static final String NEWS_CHANNEL_ID = "1401646018243592277";

    public static final String SUPPLY_ROLE_ID = "1401884822686793828";
    public static final String MODERATOR_ROLE_ID = "1401884851040419981";
    public static final String DEPLEADER_ROLE_ID = "1401990525346648064";
    public static final String CRIME_ROLE_ID = "1401999540084543631";
    public static final String STATE_ROLE_ID = "1401999584061952020";
    public static final String DEVELOPER_ROLE_ID = "1401884851040419981";

    public static String LSV_ID = "1401888085473755247";
    public static String ESB_ID = "1401888108915593297";
    public static String FAM_ID = "1401888070076207206";
    public static String MG13_ID = "1401888122203275395";
    public static String BSG_ID = "1401888139156656129";
    public static String MM_ID = "1401887603409551471";
    public static String AM_ID = "1401887647299014689";
    public static String LCN_ID = "1401887707105460286";
    public static String YAK_ID = "1401887678303309844";
    public static String RM_ID = "1401887627673866270";
    public static String LSSD_ID = "1401887661236682887";
    public static String LSPD_ID = "1401887731004608653";
    public static String FIB_ID = "1401887893051539477";
    public static String GOV_ID = "1401887911321796729";
    public static String NG_ID = "1401887979043291247";
    public static String EMS_ID = "1401887953013178398";
    public static String SASPA_ID = "1401887938408742982";

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
