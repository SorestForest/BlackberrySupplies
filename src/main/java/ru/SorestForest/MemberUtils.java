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
        return member.getUnsortedRoles().contains(SUPPLY_ROLE) || isModerator(member);
    }

    public static boolean isSelf(Member member) {
        return member.getId().equals(SELF_ID);
    }

    public static boolean isInFaction(Member member, Faction faction) {
        return member.getUnsortedRoles().contains(faction.asRole()) || isModerator(member);
    }

    public static boolean isFaction(String faction, boolean ignoreCase) {
        if (ignoreCase) {
            faction = faction.toUpperCase().trim();
        }
        for (Faction f : Faction.values()) {
            if (f.name().equalsIgnoreCase(faction)) {
                return true;
            }
        }
        return false;
    }

    public static Faction toFaction(String string) {
        for (Faction value : Faction.values()) {
            if (value.name().equalsIgnoreCase(string)){
                return value;
            }
        }
        return null;
    }

    public static ForestPair<Boolean, List<Faction>> parseFactions(String input) {
        if (input == null || input.isBlank()) return ForestPair.of(false, List.of());
        AtomicBoolean shouldBreak = new AtomicBoolean(false);
        try {
            List<Faction> factions = Arrays.stream(input.split(","))
                    .map(String::trim)
                    .map(s -> {
                        for (MemberUtils.Faction f : MemberUtils.Faction.values()) {
                            if (f.name().equalsIgnoreCase(s)) return f;
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
            return switch(this) {
                case MM -> EmojiUtils.MM_EMOJI + " Mexican Mafia";
                case RM -> EmojiUtils.RM_EMOJI + " Russian Mafia";
                case AM -> EmojiUtils.AM_EMOJI + " Armenian Mafia";
                case LCN -> EmojiUtils.LCN_EMOJI + " La Cosa Nostra";
                case YAK -> EmojiUtils.YAK_EMOJI + " Yakudza";
                case FAM -> EmojiUtils.FAM_EMOJI + " The Families";
                case LSV -> EmojiUtils.LSV_EMOJI + " Los Santos Vagos";
                case ESB -> EmojiUtils.ESB_EMOJI + " East Side Ballas";
                case BSG -> EmojiUtils.BSG_EMOJI + " Bloods Street Gang";
                case MG13 -> EmojiUtils.MG13_EMOJI + " Marabunta Grande";
                case FIB -> EmojiUtils.FIB_EMOJI + " FIB";
                case LSPD -> EmojiUtils.LSPD_EMOJI + " LSPD";
                case LSSD -> EmojiUtils.LSSD_EMOJI + " LSSD";
                case GOV -> EmojiUtils.GOV_EMOJI + " GOV";
                case NG -> EmojiUtils.NG_EMOJI + " NG";
                case SASPA -> EmojiUtils.SASPA_EMOJI + " SASPA";
                case EMS -> EmojiUtils.EMS_EMOJI + " EMS";
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
                // Мафии — тёмные насыщенные цвета
                case MM -> new Color(0x8B0000);      // тёмно-красный (кровавый)
                case RM -> new Color(0x2F4F4F);      // dark slate gray
                case AM -> new Color(0x4B0082);      // индиго
                case LCN -> new Color(0x191970);     // midnight blue
                case YAK -> new Color(0x800000);     // бордовый

                // Уличные банды — яркие узнаваемые цвета
                case FAM -> new Color(0x00FF00);     // ярко-зелёный
                case LSV -> new Color(0xFFFF00);     // ярко-жёлтый
                case ESB -> new Color(0x800080);     // фиолетовый (Ballas стиль)
                case BSG -> new Color(0xB22222);     // firebrick красный
                case MG13 -> new Color(0x1E90FF);    // dodger blue

                // Гос. структуры — официальные, строгие оттенки
                case FIB -> new Color(0x00008B);     // тёмно-синий
                case LSPD -> new Color(0x4169E1);    // royal blue
                case LSSD -> new Color(0x228B22);    // forest green
                case GOV -> new Color(0x708090);     // slate gray
                case NG -> new Color(0x556B2F);      // dark olive green
                case SASPA -> new Color(0x2E8B57);   // sea green
                case EMS -> new Color(0xDC143C);     // crimson
            };
        }
    }


}
