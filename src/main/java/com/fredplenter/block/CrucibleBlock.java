package com.fredplenter.block;

import com.fredplenter.Fredplenter;
import com.fredplenter.alchemy.CrucibleRecipe;
import com.fredplenter.alchemy.CrucibleRecipes;
import com.fredplenter.block.entity.CrucibleBlockEntity;
import com.fredplenter.registry.ModItems;
import com.fredplenter.research.ArcaneProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.tags.FluidTags;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Alchemical Crucible. The whole loop happens here:
 * <ol>
 *     <li>Heat it from below (fire, soul fire, a lit campfire, magma or lava).</li>
 *     <li>Right-click with an item to dissolve one copy of it into aspects.</li>
 *     <li>Right-click with a Stirring Rod to brew whatever mixture the contents match.</li>
 * </ol>
 * Brewing needs the matching research; a mixture that matches only locked recipes
 * says so instead of wasting the aspects.
 */
public class CrucibleBlock extends Block implements EntityBlock {

    /** Learning this research unlocks the crucible for a player. */
    public static final ResourceLocation RESEARCH = ResourceLocation.fromNamespaceAndPath(Fredplenter.MODID, "crucible_alchemy");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(0, 0, 0, 16, 14, 1),
            Block.box(0, 0, 15, 16, 14, 16),
            Block.box(0, 0, 1, 1, 14, 15),
            Block.box(15, 0, 1, 16, 14, 15));

    public CrucibleBlock(Properties properties) {
        super(properties);
    }

    /** Fire, soul fire, magma, lava and lit campfires heat the crucible from below. */
    public static boolean isHeated(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState state = level.getBlockState(below);
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK)) {
            return true;
        }
        if ((state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                && state.getValue(BlockStateProperties.LIT)) {
            return true;
        }
        return level.getFluidState(below).is(FluidTags.LAVA);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    /** Client-only: steam while heated. Cosmetic, so the server never depends on it. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!isHeated(level, pos) || random.nextInt(4) != 0) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
        level.addParticle(ParticleTypes.BUBBLE, x, pos.getY() + 1.0, z, 0.0, 0.05, 0.0);
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.1, z, 0.0, 0.02, 0.0);
    }

    /** Held item: dissolve it, or brew when it is a Stirring Rod. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        interact(player, level, pos, held, hand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return ItemInteractionResult.SUCCESS;
    }

    /** Empty hand: show the current contents. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        interact(player, level, pos, ItemStack.EMPTY, EquipmentSlot.MAINHAND);
        return InteractionResult.SUCCESS;
    }

    private static void interact(Player player, Level level, BlockPos pos, ItemStack held, EquipmentSlot slot) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) {
            return;
        }

        ArcaneProgress progress = ArcaneProgress.get(serverPlayer);
        if (!progress.hasResearch(serverPlayer.getUUID(), RESEARCH)) {
            serverPlayer.displayClientMessage(Component.translatable("message.fredplenter.crucible_locked"), true);
            return;
        }

        if (held.is(ModItems.STIRRING_ROD.get())) {
            Predicate<ResourceLocation> learned = id -> progress.hasResearch(serverPlayer.getUUID(), id);
            stir(serverPlayer, (ServerLevel) level, pos, crucible, held, slot, learned);
        } else if (held.isEmpty()) {
            serverPlayer.displayClientMessage(crucible.describe(), true);
        } else {
            dissolve(serverPlayer, (ServerLevel) level, pos, crucible, held);
        }
    }

    private static void dissolve(ServerPlayer player, ServerLevel level, BlockPos pos,
                                 CrucibleBlockEntity crucible, ItemStack held) {
        if (!isHeated(level, pos)) {
            player.displayClientMessage(Component.translatable("message.fredplenter.crucible_needs_heat"), true);
            return;
        }
        Component name = held.getHoverName();
        if (!crucible.dissolve(held)) {
            player.displayClientMessage(Component.translatable("message.fredplenter.crucible_no_aspects"), true);
            return;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.6F, 1.2F);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                6, 0.2, 0.1, 0.2, 0.01);
        player.displayClientMessage(Component.translatable("message.fredplenter.crucible_dissolved", name), true);
    }

    private static void stir(ServerPlayer player, ServerLevel level, BlockPos pos,
                             CrucibleBlockEntity crucible, ItemStack rod, EquipmentSlot slot,
                             Predicate<ResourceLocation> learned) {
        if (!isHeated(level, pos)) {
            player.displayClientMessage(Component.translatable("message.fredplenter.crucible_needs_heat"), true);
            return;
        }
        if (crucible.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.fredplenter.crucible_empty"), true);
            return;
        }
        level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.4F, 0.8F);
        rod.hurtAndBreak(1, player, slot);

        CrucibleRecipe recipe = CrucibleRecipes.match(crucible.getContents(), learned);
        if (recipe == null) {
            boolean locked = CrucibleRecipes.anyMatch(crucible.getContents());
            player.displayClientMessage(Component.translatable(locked
                    ? "message.fredplenter.crucible_recipe_locked"
                    : "message.fredplenter.crucible_unstable"), true);
            return;
        }

        crucible.consume(recipe.requires());
        ItemStack result = recipe.result().copy();
        Block.popResource(level, pos.above(), result);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.0F);
        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                20, 0.3, 0.2, 0.3, 0.05);
        player.displayClientMessage(Component.translatable("message.fredplenter.crucible_brewed",
                result.getHoverName()), true);
    }
}
