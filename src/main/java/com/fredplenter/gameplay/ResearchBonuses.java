package com.fredplenter.gameplay;

import com.fredplenter.Fredplenter;
import com.fredplenter.FredplenterConfig;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.network.NetworkHandler;
import com.fredplenter.research.ArcaneProgress;
import com.fredplenter.research.ClientArcaneProgress;
import com.fredplenter.research.ResearchRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Iterator;

@EventBusSubscriber(modid = Fredplenter.MODID)
public final class ResearchBonuses {
    private static final ResourceLocation MINER_HASTE = id("miner_haste");
    private static final ResourceLocation WARRIOR_XP = id("warrior_instinct");
    private static final ResourceLocation WARRIOR_DAMAGE = id("warrior_fury");
    private static final ResourceLocation WARRIOR_DEFENSE = id("warrior_guard");
    private static final ResourceLocation CREATE_SCHEMATICS = id("create_schematics");
    private static final ResourceLocation CREATE_PRECISION = id("create_precision");
    private static final ResourceLocation CREATE_CACHE = id("create_cache");
    private static final ResourceLocation CREATE_ALLOY = ResourceLocation.fromNamespaceAndPath("create", "andesite_alloy");
    private static final ResourceLocation CREATE_GOGGLES = ResourceLocation.fromNamespaceAndPath("create", "goggles");
    private static final int[] LUCK_STEPS = {40, 35, 30, 25, 20, 15, 10};
    private static final ResourceLocation[] LUCK_IDS = {
            id("miner_luck_40"), id("miner_luck_35"), id("miner_luck_30"),
            id("miner_luck_25"), id("miner_luck_20"), id("miner_luck_15"), id("miner_luck_10")};

    private ResearchBonuses() {
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, name);
    }

    private static boolean has(ServerPlayer player, ResourceLocation researchId) {
        return ArcaneProgress.get(player).hasResearch(player.getUUID(), researchId);
    }

    public static int minerLuckPercent(ServerPlayer player) {
        if (!FredplenterConfig.bonusBranchesEnabled()) {
            return 0;
        }
        ArcaneProgress progress = ArcaneProgress.get(player);
        for (int index = 0; index < LUCK_STEPS.length; index++) {
            if (progress.hasResearch(player.getUUID(), LUCK_IDS[index])) {
                return Math.min(LUCK_STEPS[index], FredplenterConfig.minerBonusCap());
            }
        }
        return 0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!FredplenterConfig.bonusBranchesEnabled()) {
            return;
        }
        Player player = event.getEntity();
        boolean learned = player instanceof ServerPlayer serverPlayer
                ? has(serverPlayer, MINER_HASTE)
                : player.level().isClientSide && ClientArcaneProgress.hasResearch(MINER_HASTE);
        if (learned) {
            event.setNewSpeed(event.getNewSpeed() * 1.25F);
        }
    }

    @SubscribeEvent
    public static void onOreDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || player.getAbilities().instabuild
                || !event.getState().is(Tags.Blocks.ORES)) {
            return;
        }
        int chance = minerLuckPercent(player);
        if (chance == 0) {
            return;
        }
        var enchantments = event.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        if (event.getTool().getEnchantmentLevel(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH)) > 0) {
            return;
        }
        if (player.getRandom().nextInt(100) >= chance) {
            return;
        }
        Item blockItem = Item.byBlock(event.getState().getBlock());
        ItemEntity resource = null;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty() || stack.is(blockItem)
                    || !(stack.is(Tags.Items.RAW_MATERIALS) || stack.is(Tags.Items.GEMS)
                    || stack.is(Tags.Items.DUSTS) || stack.is(Items.COAL)
                    || stack.is(Items.GOLD_NUGGET) || stack.is(Items.NETHERITE_SCRAP))) {
                continue;
            }
            resource = drop;
            break;
        }
        if (resource != null) {
            ItemEntity bonus = new ItemEntity(event.getLevel(), resource.getX(), resource.getY(), resource.getZ(),
                    resource.getItem().copyWithCount(1));
            bonus.setDefaultPickUpDelay();
            event.getDrops().add(bonus);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!FredplenterConfig.bonusBranchesEnabled()
                || event.getEntity().level().isClientSide || event.getAmount() <= 0) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && has(player, WARRIOR_DEFENSE)
                && (FredplenterConfig.bonusesAffectPvp()
                || !(event.getSource().getEntity() instanceof Player))) {
            event.setAmount(event.getAmount() * 0.80F);
        } else if (event.getEntity() instanceof Mob
                && !isMajorBoss(event.getEntity())
                && event.getSource().getEntity() instanceof ServerPlayer player && has(player, WARRIOR_DAMAGE)) {
            event.setAmount(event.getAmount() * 1.30F);
        }

    }

    private static boolean isMajorBoss(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getType() == EntityType.ENDER_DRAGON
                || entity.getType() == EntityType.WITHER
                || entity.getType() == EntityType.WARDEN;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExperience(LivingExperienceDropEvent event) {
        if (!FredplenterConfig.bonusBranchesEnabled()
                || !(event.getAttackingPlayer() instanceof ServerPlayer player) || !(event.getEntity() instanceof Mob)
                || !has(player, WARRIOR_XP) || event.getDroppedExperience() <= 0) {
            return;
        }
        int base = event.getDroppedExperience();
        long extra = base / 4L + (player.getRandom().nextInt(4) < base % 4 ? 1 : 0);
        event.setDroppedExperience((int) Math.min(Integer.MAX_VALUE, base + extra));
    }

    @SubscribeEvent
    public static void onCreateCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!FredplenterConfig.bonusBranchesEnabled()
                || !(event.getEntity() instanceof ServerPlayer player) || player.getAbilities().instabuild
                || !ResearchRegistry.isCreateLoaded() || event.getCrafting().isEmpty()
                || !"create".equals(BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem()).getNamespace())) {
            return;
        }
        ArcaneProgress progress = ArcaneProgress.get(player);
        ResourceLocation craftedItem = BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem());
        if (progress.hasResearch(player.getUUID(), CREATE_SCHEMATICS)
                && progress.getAspectPoints(player.getUUID(), Aspect.MOTUS) < FredplenterConfig.knowledgeCap()
                && progress.markCreateItemCrafted(player.getUUID(), craftedItem)) {
            progress.addAspectPoints(player.getUUID(), Aspect.MOTUS, 1);
            NetworkHandler.sendProgress(player);
        }
        if (progress.hasResearch(player.getUUID(), CREATE_PRECISION)
                && usesCreateAlloy(event.getInventory()) && player.getRandom().nextInt(10) == 0) {
            ItemStack alloy = new ItemStack(BuiltInRegistries.ITEM.get(CREATE_ALLOY));
            if (!player.getInventory().add(alloy)) {
                player.drop(alloy, false);
            }
        }
    }

    private static boolean usesCreateAlloy(net.minecraft.world.Container craftingGrid) {
        for (int slot = 0; slot < craftingGrid.getContainerSize(); slot++) {
            if (CREATE_ALLOY.equals(BuiltInRegistries.ITEM.getKey(craftingGrid.getItem(slot).getItem()))) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCreateDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || player.getAbilities().instabuild
                || !ResearchRegistry.isCreateLoaded() || !has(player, CREATE_CACHE)
                || !"create".equals(BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).getNamespace())
                || !CREATE_GOGGLES.equals(BuiltInRegistries.ITEM.getKey(
                player.getItemBySlot(EquipmentSlot.HEAD).getItem()))) {
            return;
        }
        Iterator<ItemEntity> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemEntity item = drops.next();
            ItemStack remainder = item.getItem().copy();
            player.getInventory().add(remainder);
            if (remainder.isEmpty()) {
                drops.remove();
            } else {
                item.setItem(remainder);
            }
        }
    }
}
