package com.fredplenter;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FredplenterConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue KNOWLEDGE_CAP;
    public static final ModConfigSpec.IntValue PASSIVE_KNOWLEDGE_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue NODE_ATTUNEMENT_RADIUS;
    public static final ModConfigSpec.IntValue AURA_NODE_VIS_CAP;
    public static final ModConfigSpec.IntValue WAND_VIS_BASE_CAP;
    public static final ModConfigSpec.BooleanValue BONUS_BRANCHES_ENABLED;
    public static final ModConfigSpec.BooleanValue SPELLS_AFFECT_PLAYERS;
    public static final ModConfigSpec.BooleanValue BONUSES_AFFECT_PVP;
    public static final ModConfigSpec.IntValue MINER_BONUS_CAP;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("progression");
        KNOWLEDGE_CAP = BUILDER.comment("Maximum stored research knowledge for each aspect.")
                .defineInRange("knowledgeCap", 100, 1, 100_000);
        PASSIVE_KNOWLEDGE_INTERVAL_TICKS = BUILDER
                .comment("Ticks between renewable knowledge points near an aura node; 1200 ticks is one minute.")
                .defineInRange("passiveKnowledgeIntervalTicks", 1200, 20, 1_000_000);
        NODE_ATTUNEMENT_RADIUS = BUILDER
                .comment("Maximum node search radius unlocked by Node Attunement.")
                .defineInRange("nodeAttunementRadius", 10, 1, 64);
        AURA_NODE_VIS_CAP = BUILDER
                .comment("Maximum regenerating vis stored per primal aspect in an Aura Node.")
                .defineInRange("auraNodeVisCap", 24, 1, 100_000);
        WAND_VIS_BASE_CAP = BUILDER
                .comment("Base vis capacity per primal aspect; Wand Capacity research doubles this value.")
                .defineInRange("wandVisBaseCap", 100, 1, 100_000);
        BUILDER.pop();

        BUILDER.push("gameplay");
        BONUS_BRANCHES_ENABLED = BUILDER
                .comment("Enable the optional miner, warrior, fisher, and Create bonus branches.")
                .define("bonusBranchesEnabled", true);
        SPELLS_AFFECT_PLAYERS = BUILDER
                .comment("Allow wand spells that target entities to affect other players.")
                .define("spellsAffectPlayers", false);
        BONUSES_AFFECT_PVP = BUILDER
                .comment("Apply Warrior Guard damage reduction to player-versus-player damage.")
                .define("bonusesAffectPvP", false);
        MINER_BONUS_CAP = BUILDER
                .comment("Maximum additional ore-drop chance in percentage points.")
                .defineInRange("minerBonusCap", 20, 0, 100);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private FredplenterConfig() {
    }

    public static int knowledgeCap() {
        return valueOrDefault(KNOWLEDGE_CAP, 100);
    }

    public static int nodeVisCap() {
        return valueOrDefault(AURA_NODE_VIS_CAP, 24);
    }

    public static int wandVisBaseCap() {
        return valueOrDefault(WAND_VIS_BASE_CAP, 100);
    }

    public static int passiveKnowledgeIntervalTicks() {
        return valueOrDefault(PASSIVE_KNOWLEDGE_INTERVAL_TICKS, 1200);
    }

    public static int nodeAttunementRadius() {
        return valueOrDefault(NODE_ATTUNEMENT_RADIUS, 10);
    }

    public static int minerBonusCap() {
        return valueOrDefault(MINER_BONUS_CAP, 20);
    }

    public static boolean bonusBranchesEnabled() {
        return booleanOrDefault(BONUS_BRANCHES_ENABLED, true);
    }

    public static boolean spellsAffectPlayers() {
        return booleanOrDefault(SPELLS_AFFECT_PLAYERS, false);
    }

    public static boolean bonusesAffectPvp() {
        return booleanOrDefault(BONUSES_AFFECT_PVP, false);
    }

    private static int valueOrDefault(ModConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (IllegalStateException exception) {
            return fallback;
        }
    }

    private static boolean booleanOrDefault(ModConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (IllegalStateException exception) {
            return fallback;
        }
    }
}
