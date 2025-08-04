package ru.SorestForest;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import static ru.SorestForest.Settings.*;


public class MemberUtils {

    public static Role SUPPLY_ROLE;
    public static Role MODERATOR_ROLE;
    public static Role DEPLEADER_ROLE;

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
        SUPPLY_ROLE = Main.API.getRoleById(SUPPLY_ROLE_ID);
        MODERATOR_ROLE = Main.API.getRoleById(MODERATOR_ROLE_ID);
        DEPLEADER_ROLE = Main.API.getRoleById(DEPLEADER_ROLE_ID);

        LSV = Main.API.getRoleById(LSV_ID);
        ESB = Main.API.getRoleById(ESB_ID);
        FAM = Main.API.getRoleById(FAM_ID);
        MG13 = Main.API.getRoleById(MG13_ID);
        BSG = Main.API.getRoleById(BSG_ID);
        MM = Main.API.getRoleById(MM_ID);
        AM = Main.API.getRoleById(AM_ID);
        LCN = Main.API.getRoleById(LCN_ID);
        YAK = Main.API.getRoleById(YAK_ID);
        RM = Main.API.getRoleById(RM_ID);
        LSSD = Main.API.getRoleById(LSSD_ID);
        LSPD = Main.API.getRoleById(LSPD_ID);
        FIB = Main.API.getRoleById(FIB_ID);
        GOV = Main.API.getRoleById(GOV_ID);
        NG = Main.API.getRoleById(NG_ID);
        EMS = Main.API.getRoleById(EMS_ID);
        SASPA = Main.API.getRoleById(SASPA_ID);
    }


    public static boolean isModerator(Member member) {
        return member.getUnsortedRoles().contains(MODERATOR_ROLE);
    }

    public static boolean isSupplier(Member member) {
        return member.getUnsortedRoles().contains(SUPPLY_ROLE);
    }

    public static boolean isSelf(Member member) {
        return member.getId().equals(SELF_ID);
    }

    public static boolean isInFaction(Member member, Faction faction) {
        return member.getUnsortedRoles().contains(faction.asRole()) || isModerator(member);
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


    }


}
