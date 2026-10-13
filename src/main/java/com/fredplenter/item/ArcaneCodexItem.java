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

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        formArcaneTable(level, clickedPos, partnerPos);
        return InteractionResult.CONSUME;
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

    private static void formArcaneTable(Level level, BlockPos mainPos, BlockPos extensionPos) {
        Direction facing = Direction.getNearest(
                extensionPos.getX() - mainPos.getX(), 0, extensionPos.getZ() - mainPos.getZ());
        BlockState mainState = ModBlocks.ARCANE_TABLE.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing);
        BlockState extensionState = ModBlocks.ARCANE_TABLE_EXTENSION.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing);

        level.setBlockAndUpdate(mainPos, mainState);
        level.setBlockAndUpdate(extensionPos, extensionState);
        if (level.getBlockEntity(mainPos) instanceof com.fredplenter.block.entity.ArcaneTableBlockEntity table) {
            table.setExtensionPos(extensionPos);
        }
        level.playSound(null, mainPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
