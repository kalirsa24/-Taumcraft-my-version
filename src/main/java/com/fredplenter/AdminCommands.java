package com.fredplenter;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.network.NetworkHandler;
import com.fredplenter.research.ArcaneProgress;
import com.fredplenter.research.ResearchRegistry;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AdminCommands {

    private AdminCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fredplenter")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("knowledge")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("aspect", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(
                                                0, FredplenterConfig.knowledgeCap()))
                                                .executes(context -> setKnowledge(
                                                        EntityArgument.getPlayer(context, "player"),
                                                        com.mojang.brigadier.arguments.StringArgumentType
                                                                .getString(context, "aspect"),
                                                        IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("research")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("research", ResourceLocationArgument.id())
                                        .executes(context -> setResearch(
                                                EntityArgument.getPlayer(context, "player"),
                                                ResourceLocationArgument.getId(context, "research"), true))
                                        .then(Commands.literal("revoke")
                                                .executes(context -> setResearch(
                                                        EntityArgument.getPlayer(context, "player"),
                                                        ResourceLocationArgument.getId(context, "research"), false))))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> reset(EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("recover-note")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("research", ResourceLocationArgument.id())
                                        .executes(context -> recoverNote(
                                                EntityArgument.getPlayer(context, "player"),
                                                ResourceLocationArgument.getId(context, "research")))))));
    }

    private static int setKnowledge(ServerPlayer player, String aspectId, int amount) {
        Aspect aspect = Aspect.byId(aspectId);
        if (aspect == null) {
            player.sendSystemMessage(Component.translatable("commands.fredplenter.unknown_aspect", aspectId));
            return 0;
        }
        ArcaneProgress.get(player).setAspectPoints(player.getUUID(), aspect, amount);
        NetworkHandler.sendFullProgress(player);
        player.sendSystemMessage(Component.translatable(
                "commands.fredplenter.knowledge_set", aspectId,
                ArcaneProgress.get(player).getAspectPoints(player.getUUID(), aspect)));
        return 1;
    }

    private static int setResearch(ServerPlayer player, ResourceLocation researchId, boolean learned) {
        if (ResearchRegistry.get(researchId) == null
                || !ArcaneProgress.get(player).setResearch(player.getUUID(), researchId, learned)) {
            player.sendSystemMessage(Component.translatable("commands.fredplenter.research_unchanged"));
            return 0;
        }
        NetworkHandler.sendFullProgress(player);
        player.sendSystemMessage(Component.translatable(
                learned ? "commands.fredplenter.research_granted" : "commands.fredplenter.research_revoked",
                researchId));
        return 1;
    }

    private static int reset(ServerPlayer player) {
        ArcaneProgress.get(player).resetPlayer(player.getUUID());
        NetworkHandler.sendFullProgress(player);
        player.sendSystemMessage(Component.translatable("commands.fredplenter.reset", player.getGameProfile().getName()));
        return 1;
    }

    private static int recoverNote(ServerPlayer player, ResourceLocation researchId) {
        if (ResearchRegistry.get(researchId) == null
                || !ArcaneProgress.get(player).recoverResearchReservation(player.getUUID(), researchId)) {
            player.sendSystemMessage(Component.translatable("commands.fredplenter.note_not_reserved"));
            return 0;
        }
        NetworkHandler.sendFullProgress(player);
        player.sendSystemMessage(Component.translatable("commands.fredplenter.note_recovered", researchId));
        return 1;
    }
}
