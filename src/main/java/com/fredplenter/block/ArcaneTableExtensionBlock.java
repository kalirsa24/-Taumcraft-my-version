package com.fredplenter.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The non-functional "other half" of a formed Arcane Table. Right-clicking
 * it forwards the interaction to the main block (one tile over, opposite
 * the facing direction), so the player can open the research menu from
 * either half.
 */
public class ArcaneTableExtensionBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public ArcaneTableExtensionBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // The main block sits on the opposite side of the facing direction.
        BlockPos mainPos = pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        if (level.getBlockEntity(mainPos) instanceof MenuProvider menuProvider && !level.isClientSide) {
            player.openMenu(menuProvider, buf -> buf.writeBlockPos(mainPos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockPos mainPos = pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
            if (level.getBlockState(mainPos).getBlock() instanceof ArcaneTableBlock) {
                level.removeBlock(mainPos, false);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
