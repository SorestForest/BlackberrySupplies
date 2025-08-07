package ru.SorestForest;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static ru.SorestForest.Settings.*;


public class MemberUtils {

    public static Role SUPPLY_ROLE;
    public static Role MODERATOR_ROLE;
    public static Role DEPLEADER_ROLE;
    public static Role DEVELOPER_ROLE;

    public static Role LSV;
    public static Role ESB;
    public static Role FAM;
    public static Role MG13;
    public static Role BSG;
    public static Role MM;
    public static Role AM;
    public static Role LCN;
    public static Role YAK;
    public static Role RM;
    public static Role LSSD;
    public static Role LSPD;
    public static Role FIB;
    public static Role GOV;
    public static Role NG;
    public static Role EMS;
    public static Role SASPA;

    public static void setup() {
        SUPPLY_ROLE = BotStarter.API.getRoleById(SUPPLY_ROLE_ID);
        MODERATOR_ROLE = BotStarter.API.getRoleById(MODERATOR_ROLE_ID);
        DEPLEADER_ROLE = BotStarter.API.getRoleById(DEPLEADER_ROLE_ID);
        DEVELOPER_ROLE = BotStarter.API.getRoleById(DEVELOPER_ROLE_ID);

        LSV = BotStarter.API.getRoleById(LSV_ID);
        ESB = BotStarter.API.getRoleById(ESB_ID);
        FAM = BotStarter.API.getRoleById(FAM_ID);
        MG13 = BotStarter.API.getRoleById(MG13_ID);
        BSG = BotStarter.API.getRoleById(BSG_ID);
        MM = BotStarter.API.getRoleById(MM_ID);
        AM = BotStarter.API.getRoleById(AM_ID);
        LCN = BotStarter.API.getRoleById(LCN_ID);
        YAK = BotStarter.API.getRoleById(YAK_ID);
        RM = BotStarter.API.getRoleById(RM_ID);
        LSSD = BotStarter.API.getRoleById(LSSD_ID);
        LSPD = BotStarter.API.getRoleById(LSPD_ID);
        FIB = BotStarter.API.getRoleById(FIB_ID);
        GOV = BotStarter.API.getRoleById(GOV_ID);
        NG = BotStarter.API.getRoleById(NG_ID);
        EMS = BotStarter.API.getRoleById(EMS_ID);
        SASPA = BotStarter.API.getRoleById(SASPA_ID);
    }


    public static boolean isModerator(Member member) {
        Set<Role> roles = member.getUnsortedRoles();
        return roles.contains(MODERATOR_ROLE) || roles.contains(DEVELOPER_ROLE);
    }

    public static boolean isSupplier(Member member) {
        return member.getUnsortedRoles().contains(SUPPLY_ROLE);
    }

    public static boolean isInFaction(Member member, Faction faction) {
        return member.getUnsortedRoles().contains(faction.asRole()) || isModerator(member);
    }

    public static boolean isFaction(String faction, boolean ignoreCase) {
        if (ignoreCase) {
            faction = faction.toLowerCase();
        }
        for (Faction f : Faction.values()) {
            if (f.aliases().contains(faction)) {
                return true;
            }
        }
        return false;
    }

    public static Faction toFaction(String string) {
        string = string.toLowerCase();
        for (Faction value : Faction.values()) {
            if (value.aliases().contains(string)){
                return value;
            }
        }
        return null;
    }

    public static ForestPair<Boolean, List<Faction>> parseFactions(String input) {
        if (input == null || input.isBlank()) return ForestPair.of(false, List.of());
        AtomicBoolean shouldBreak = new AtomicBoolean(false);
        input = input.replace(",","");
        input = input.replace("+"," ");
        try {
            List<Faction> factions = Arrays.stream(input.split(" "))
                    .map(String::trim)
                    .map(s -> {
                        s = s.toLowerCase();
                        for (Faction f : Faction.values()) {
                            if (f.aliases().contains(s)) return f;
                        }
                        shouldBreak.set(true);
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            if(shouldBreak.get()) {
                return ForestPair.of(false, factions);
            }
            return ForestPair.of(true, factions);
        } catch (Exception e){
            return ForestPair.of(false, List.of());
        }
    }


    public enum Faction {
        MM, RM, AM, LCN, YAK, FAM, LSV, ESB, BSG, MG13, FIB, LSPD, LSSD, GOV, NG, SASPA, EMS;
        // Display Names
        public static final String MM_NAME = EmojiUtils.MM_EMOJI + " Mexican Mafia";
        public static final String RM_NAME = EmojiUtils.RM_EMOJI + " Russian Mafia";
        public static final String AM_NAME = EmojiUtils.AM_EMOJI + " Armenian Mafia";
        public static final String LCN_NAME = EmojiUtils.LCN_EMOJI + " La Cosa Nostra";
        public static final String YAK_NAME = EmojiUtils.YAK_EMOJI + " Yakudza";
        public static final String FAM_NAME = EmojiUtils.FAM_EMOJI + " The Families";
        public static final String LSV_NAME = EmojiUtils.LSV_EMOJI + " Los Santos Vagos";
        public static final String ESB_NAME = EmojiUtils.ESB_EMOJI + " East Side Ballas";
        public static final String BSG_NAME = EmojiUtils.BSG_EMOJI + " Bloods Street Gang";
        public static final String MG13_NAME = EmojiUtils.MG13_EMOJI + " Marabunta Grande";
        public static final String FIB_NAME = EmojiUtils.FIB_EMOJI + " FIB";
        public static final String LSPD_NAME = EmojiUtils.LSPD_EMOJI + " LSPD";
        public static final String LSSD_NAME = EmojiUtils.LSSD_EMOJI + " LSSD";
        public static final String GOV_NAME = EmojiUtils.GOV_EMOJI + " GOV";
        public static final String NG_NAME = EmojiUtils.NG_EMOJI + " NG";
        public static final String SASPA_NAME = EmojiUtils.SASPA_EMOJI + " SASPA";
        public static final String EMS_NAME = EmojiUtils.EMS_EMOJI + " EMS";

        // Colors
        public static final Color MM_COLOR = new Color(0x006C00);
        public static final Color RM_COLOR = new Color(0x7E8989);
        public static final Color AM_COLOR = new Color(0x430606);
        public static final Color LCN_COLOR = new Color(0xD1B604);
        public static final Color YAK_COLOR = new Color(0x800000);

        public static final Color FAM_COLOR = new Color(0x00FF00);
        public static final Color LSV_COLOR = new Color(0xFFFF00);
        public static final Color ESB_COLOR = new Color(0x800080);
        public static final Color BSG_COLOR = new Color(0xB22222);
        public static final Color MG13_COLOR = new Color(0x1E90FF);

        public static final Color FIB_COLOR = new Color(0x000000);
        public static final Color LSPD_COLOR = new Color(0x4169E1);
        public static final Color LSSD_COLOR = new Color(0x804900);
        public static final Color GOV_COLOR = new Color(0x708090);
        public static final Color NG_COLOR = new Color(0x556B2F);
        public static final Color SASPA_COLOR = new Color(0x18036A);
        public static final Color EMS_COLOR = new Color(0xDC143C);

        // Aliases
        public static final Set<String> MM_ALIASES = Set.of("mm", "mex");
        public static final Set<String> RM_ALIASES = Set.of("rm", "rus","russianmafia");
        public static final Set<String> AM_ALIASES = Set.of("am","arm");
        public static final Set<String> LCN_ALIASES = Set.of("lcn", "cosa", "lacosa", "la-cosa-nostra");
        public static final Set<String> YAK_ALIASES = Set.of("yak", "yakuza", "yakudza");

        public static final Set<String> FAM_ALIASES = Set.of("fam", "families", "thefamilies", "the-families");
        public static final Set<String> LSV_ALIASES = Set.of("lsv", "vagos", "lossantosvagos", "los-santos-vagos");
        public static final Set<String> ESB_ALIASES = Set.of("esb", "ballas", "eastsideballas", "east-side-ballas");
        public static final Set<String> BSG_ALIASES = Set.of("bsg", "bloods", "blds", "blood");
        public static final Set<String> MG13_ALIASES = Set.of("mg13", "mg-13", "mara", "marabunta", "marabuntagrande", "marabunta-grande");

        public static final Set<String> FIB_ALIASES = Set.of("fib", "federal", "fed");
        public static final Set<String> LSPD_ALIASES = Set.of("lspd", "pd", "police");
        public static final Set<String> LSSD_ALIASES = Set.of("lssd", "sd", "sheriff");
        public static final Set<String> GOV_ALIASES = Set.of("gov", "government", "usss", "uss","ussss");
        public static final Set<String> NG_ALIASES = Set.of("ng", "nationalguard", "national-guard", "sang", "army");
        public static final Set<String> SASPA_ALIASES = Set.of("saspa", "ft");
        public static final Set<String> EMS_ALIASES = Set.of("ems","medic");

        public Role asRole() {
            return switch(this) {
                case MM -> MemberUtils.MM;
                case RM -> MemberUtils.RM;
                case AM -> MemberUtils.AM;
                case LCN -> MemberUtils.LCN;
                case YAK -> MemberUtils.YAK;
                case FAM -> MemberUtils.FAM;
                case LSV -> MemberUtils.LSV;
                case ESB -> MemberUtils.ESB;
                case BSG -> MemberUtils.BSG;
                case MG13 -> MemberUtils.MG13;
                case FIB -> MemberUtils.FIB;
                case LSPD -> MemberUtils.LSPD;
                case LSSD -> MemberUtils.LSSD;
                case GOV -> MemberUtils.GOV ;
                case NG -> MemberUtils.NG;
                case SASPA -> MemberUtils.SASPA;
                case EMS -> MemberUtils.EMS;
            };
        }

        public String displayName() {
            return switch (this) {
                case MM -> MM_NAME;
                case RM -> RM_NAME;
                case AM -> AM_NAME;
                case LCN -> LCN_NAME;
                case YAK -> YAK_NAME;
                case FAM -> FAM_NAME;
                case LSV -> LSV_NAME;
                case ESB -> ESB_NAME;
                case BSG -> BSG_NAME;
                case MG13 -> MG13_NAME;
                case FIB -> FIB_NAME;
                case LSPD -> LSPD_NAME;
                case LSSD -> LSSD_NAME;
                case GOV -> GOV_NAME;
                case NG -> NG_NAME;
                case SASPA -> SASPA_NAME;
                case EMS -> EMS_NAME;
            };
        }

        public boolean isMafia(){
            return switch (this) {
                case AM, RM, YAK, LCN, MM -> true;
                default -> false;
            };
        }

        public Color color() {
            return switch (this) {
                case MM -> MM_COLOR;
                case RM -> RM_COLOR;
                case AM -> AM_COLOR;
                case LCN -> LCN_COLOR;
                case YAK -> YAK_COLOR;

                case FAM -> FAM_COLOR;
                case LSV -> LSV_COLOR;
                case ESB -> ESB_COLOR;
                case BSG -> BSG_COLOR;
                case MG13 -> MG13_COLOR;

                case FIB -> FIB_COLOR;
                case LSPD -> LSPD_COLOR;
                case LSSD -> LSSD_COLOR;
                case GOV -> GOV_COLOR;
                case NG -> NG_COLOR;
                case SASPA -> SASPA_COLOR;
                case EMS -> EMS_COLOR;
            };
        }

        public Set<String> aliases() {
            return switch (this) {
                case MM -> MM_ALIASES;
                case RM -> RM_ALIASES;
                case AM -> AM_ALIASES;
                case LCN -> LCN_ALIASES;
                case YAK -> YAK_ALIASES;

                case FAM -> FAM_ALIASES;
                case LSV -> LSV_ALIASES;
                case ESB -> ESB_ALIASES;
                case BSG -> BSG_ALIASES;
                case MG13 -> MG13_ALIASES;

                case FIB -> FIB_ALIASES;
                case LSPD -> LSPD_ALIASES;
                case LSSD -> LSSD_ALIASES;
                case GOV -> GOV_ALIASES;
                case NG -> NG_ALIASES;
                case SASPA -> SASPA_ALIASES;
                case EMS -> EMS_ALIASES;
            };
        }
    }


}
