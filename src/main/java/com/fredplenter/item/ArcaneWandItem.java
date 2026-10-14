package com.fredplenter.item;

import com.fredplenter.FredplenterConfig;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.block.entity.AuraNodeBlockEntity;
import com.fredplenter.research.ArcaneProgress;
import com.fredplenter.research.ClientArcaneProgress;
import com.fredplenter.spell.Spell;
import com.fredplenter.spell.SpellEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The Resonance Wand stores primal vis from aura nodes and spends it on spells.
 * <ul>
 *     <li>Right-click a node: absorb vis.</li>
 *     <li>Right-click elsewhere: cast the selected spell.</li>
 *     <li>Shift + right-click: select the next spell you have researched.</li>
 * </ul>
 */
public class ArcaneWandItem extends Item {

    public static final int CAPACITY_PER_ASPECT = 100;
    private static final int USE_COOLDOWN_TICKS = 5;
    private static final String SPELL_KEY = "Spell";
    private static final ResourceLocation WAND_ATTUNEMENT =
            ResourceLocation.fromNamespaceAndPath("fredplenter", "wand_attunement");
    private static final ResourceLocation WAND_CAPACITY =
            ResourceLocation.fromNamespaceAndPath("fredplenter", "wand_capacity");
    private static final ResourceLocation DEEP_RESONANCE =
            ResourceLocation.fromNamespaceAndPath("fredplenter", "deep_resonance");

    public ArcaneWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof AuraNodeBlockEntity node)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        if (!ArcaneProgress.get(player).hasResearch(player.getUUID(), WAND_ATTUNEMENT)) {
            player.displayClientMessage(Component.translatable("message.fredplenter.wand_attunement_locked"), true);
            return InteractionResult.CONSUME;
        }

        ItemStack wand = context.getItemInHand();
        CompoundTag stored = getStored(wand);
        int capacity = capacityPerAspect(learnedBy(player));
        int drained = 0;
        boolean hasCapacity = false;
        boolean nodeHasVis = false;
        for (Aspect aspect : Aspect.values()) {
            if (!aspect.isPrimal()) {
                continue;
            }
            int current = Math.max(0, Math.min(maximumCapacityPerAspect(), stored.getInt(aspect.getId())));
            hasCapacity |= current < capacity;
            nodeHasVis |= node.getVis(aspect) > 0;
            if (current >= capacity) {
                continue;
            }
            int amount = node.extract(aspect, 1);
            if (amount > 0) {
                stored.putInt(aspect.getId(), current + amount);
                drained += amount;
            }
        }

        if (drained > 0) {
            wand.set(DataComponents.CUSTOM_DATA, CustomData.of(stored));
            player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
            level.playSound(null, context.getClickedPos(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.7F, 1.2F);
            player.displayClientMessage(Component.translatable("message.fredplenter.wand_drained", drained), true);
            return InteractionResult.CONSUME;
        }
        player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        String message = !hasCapacity ? "message.fredplenter.wand_full"
                : nodeHasVis ? "message.fredplenter.wand_cannot_hold_node_aspects"
                : "message.fredplenter.node_empty";
        player.displayClientMessage(Component.translatable(message), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack wand = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(wand, level.isClientSide);
        }
        if (player.isShiftKeyDown()) {
            cycleSpell(serverPlayer, wand);
        } else {
            castSelectedSpell(serverPlayer, wand);
        }
        return InteractionResultHolder.success(wand);
    }

    private static void castSelectedSpell(ServerPlayer player, ItemStack wand) {
        Spell spell = selectedSpell(player, wand);
        if (spell == null) {
            player.displayClientMessage(Component.translatable("message.fredplenter.no_spells_learned"), true);
            return;
        }
        int cost = spellCost(spell, learnedBy(player));
        if (stored(wand, spell.getAspect()) < cost) {
            player.displayClientMessage(Component.translatable("message.fredplenter.wand_low_vis",
                    Component.translatable(spell.getAspect().getTranslationKey()), cost), true);
            return;
        }
        if (!SpellEffects.apply(spell, player)) {
            player.displayClientMessage(Component.translatable("message.fredplenter.spell_no_target"), true);
            return;
        }
        drain(wand, spell.getAspect(), cost);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.8F, 1.3F);
        player.displayClientMessage(Component.translatable("message.fredplenter.spell_cast",
                Component.translatable(spell.getTranslationKey())), true);
    }

    private static void cycleSpell(ServerPlayer player, ItemStack wand) {
        Spell next = Spell.nextUnlocked(learnedBy(player), selectedSpell(player, wand));
        if (next == null) {
            player.displayClientMessage(Component.translatable("message.fredplenter.no_spells_learned"), true);
            return;
        }
        CompoundTag tag = getStored(wand);
        tag.putString(SPELL_KEY, next.getId());
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.displayClientMessage(Component.translatable("message.fredplenter.spell_selected",
                Component.translatable(next.getTranslationKey())), true);
    }

    /** The spell the wand will cast: the stored choice if still learned, otherwise the first learned one. */
    public static Spell selectedSpell(ServerPlayer player, ItemStack wand) {
        Predicate<ResourceLocation> learned = learnedBy(player);
        Spell chosen = Spell.byId(getStored(wand).getString(SPELL_KEY));
        if (chosen != null && chosen.isUnlocked(learned)) {
            return chosen;
        }
        return Spell.firstUnlocked(learned);
    }

    private static Predicate<ResourceLocation> learnedBy(ServerPlayer player) {
        ArcaneProgress progress = ArcaneProgress.get(player);
        UUID playerId = player.getUUID();
        return researchId -> progress.hasResearch(playerId, researchId);
    }

    public static int capacityPerAspect(Predicate<ResourceLocation> learned) {
        int baseCapacity = FredplenterConfig.wandVisBaseCap();
        return learned.test(WAND_CAPACITY) ? maximumCapacityPerAspect() : baseCapacity;
    }

    private static int maximumCapacityPerAspect() {
        return (int) Math.min(Integer.MAX_VALUE, (long) FredplenterConfig.wandVisBaseCap() * 2);
    }

    public static int spellCost(Spell spell, Predicate<ResourceLocation> learned) {
        return learned.test(DEEP_RESONANCE) ? Math.max(1, spell.getCost() - 1) : spell.getCost();
    }

    public static int stored(ItemStack stack, Aspect aspect) {
        return Math.max(0, Math.min(maximumCapacityPerAspect(), getStored(stack).getInt(aspect.getId())));
    }

    /** Removes {@code amount} of {@code aspect} from the wand if it holds enough. */
    public static boolean drain(ItemStack stack, Aspect aspect, int amount) {
        if (amount <= 0) {
            return false;
        }
        CompoundTag stored = getStored(stack);
        int current = Math.max(0, Math.min(maximumCapacityPerAspect(), stored.getInt(aspect.getId())));
        if (current < amount) {
            return false;
        }
        stored.putInt(aspect.getId(), current - amount);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(stored));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int total = 0;
        int capacityPerAspect = capacityPerAspect(ClientArcaneProgress::hasResearch);
        for (Aspect aspect : Aspect.values()) {
            if (!aspect.isPrimal()) {
                continue;
            }
            int amount = Math.min(capacityPerAspect, stored(stack, aspect));
            total += amount;
        }
        int capacity = capacityPerAspect * 6;
        int fillPercent = total * 100 / capacity;
        tooltip.add(Component.translatable("tooltip.fredplenter.wand_capacity", total, capacity, fillPercent));
        Spell chosen = Spell.byId(getStored(stack).getString(SPELL_KEY));
        Spell shown = chosen != null && chosen.isUnlocked(ClientArcaneProgress::hasResearch)
                ? chosen : Spell.firstUnlocked(ClientArcaneProgress::hasResearch);
        if (shown == null) {
            tooltip.add(Component.translatable("tooltip.fredplenter.wand_no_spell"));
        } else {
            tooltip.add(Component.translatable("tooltip.fredplenter.wand_spell",
                    Component.translatable(shown.getTranslationKey()),
                    spellCost(shown, ClientArcaneProgress::hasResearch),
                    Component.translatable(shown.getAspect().getTranslationKey())));
        }
        tooltip.add(Component.translatable("tooltip.fredplenter.wand_cast_help"));
        tooltip.add(Component.translatable("tooltip.fredplenter.wand_shift_hint"));
        tooltip.add(Component.translatable("tooltip.fredplenter.wand_scan_help"));
    }

    private static CompoundTag getStored(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
}
