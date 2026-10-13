package com.fredplenter.item;

import com.fredplenter.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;

public class ArcaneCodexItem extends Item {

    public ArcaneCodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
            Level level, Player player, net.minecraft.world.InteractionHand hand) {
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!(clickedState.getBlock() instanceof CraftingTableBlock)) {
            return InteractionResult.PASS;
        }

        BlockPos partnerPos = findAdjacentCraftingTable(level, clickedPos);
        if (partnerPos == null) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.fredplenter.need_second_table"),
                        true);
            }
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        if (!level.isClientSide && (player == null || !player.mayBuild()
                || !player.mayInteract(level, clickedPos) || !player.mayInteract(level, partnerPos))) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        return formArcaneTable(level, clickedPos, partnerPos, player)
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    private static BlockPos findAdjacentCraftingTable(Level level, BlockPos origin) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = origin.relative(direction);
            if (level.getBlockState(neighbour).getBlock() instanceof CraftingTableBlock) {
                return neighbour;
            }
        }
        return null;
    }

    private static boolean formArcaneTable(Level level, BlockPos mainPos, BlockPos extensionPos, Player player) {
        BlockSnapshot mainSnapshot = BlockSnapshot.create(level.dimension(), level, mainPos);
        BlockSnapshot extensionSnapshot = BlockSnapshot.create(level.dimension(), level, extensionPos);
        BlockState mainAgainst = level.getBlockState(mainPos);
        Direction facing = Direction.getNearest(
                extensionPos.getX() - mainPos.getX(), 0, extensionPos.getZ() - mainPos.getZ());
        BlockState mainState = ModBlocks.ARCANE_TABLE.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing);
        BlockState extensionState = ModBlocks.ARCANE_TABLE_EXTENSION.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing);

        level.setBlockAndUpdate(mainPos, mainState);
        level.setBlockAndUpdate(extensionPos, extensionState);
        BlockEvent.EntityPlaceEvent mainPlaceEvent =
                new BlockEvent.EntityPlaceEvent(mainSnapshot, mainAgainst, player);
        BlockEvent.EntityPlaceEvent extensionPlaceEvent =
                new BlockEvent.EntityPlaceEvent(extensionSnapshot, mainAgainst, player);
        if (NeoForge.EVENT_BUS.post(mainPlaceEvent).isCanceled()
                || NeoForge.EVENT_BUS.post(extensionPlaceEvent).isCanceled()) {
            extensionSnapshot.restore(3);
            mainSnapshot.restore(3);
            return false;
        }
        if (level.getBlockEntity(mainPos) instanceof com.fredplenter.block.entity.ArcaneTableBlockEntity table) {
            table.setExtensionPos(extensionPos);
        }
        level.playSound(null, mainPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
}
