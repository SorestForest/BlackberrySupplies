package ru.SorestForest;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.Set;

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
            faction = faction.toUpperCase();
        }
        for (Faction f : Faction.values()) {
            if (f.name().equals(faction)) {
                return true;
            }
        }
        return false;
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
                case MM -> "Mexican Mafia";
                case RM -> "Russian Mafia";
                case AM -> "Armenian Mafia";
                case LCN -> "La Cosa Nostra";
                case YAK -> "Yakudza";
                case FAM -> "The Families";
                case LSV -> "Los Santos Vagos";
                case ESB -> "East Side Ballas";
                case BSG -> "Bloods Street Gang";
                case MG13 -> "Marabunta Grande";
                case FIB -> "FIB";
                case LSPD -> "LSPD";
                case LSSD -> "LSSD";
                case GOV -> "GOV";
                case NG -> "NG";
                case SASPA -> "SASPA";
                case EMS -> "EMS";
            };
        }

        public boolean isMafia(){
            return switch (this) {
                case AM, RM, YAK, LCN, MM -> true;
                default -> false;
            };
        }
    }


}
