package com.fredplenter.block;

import com.fredplenter.block.entity.ArcaneTableBlockEntity;
import com.fredplenter.network.NetworkHandler;
import com.fredplenter.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The "main" half of a formed Arcane Table. Holds the {@link ArcaneTableBlockEntity}
 * with all of the research state; the other half is a purely decorative
 * {@link ArcaneTableExtensionBlock} that points back at this one.
 */
public class ArcaneTableBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public ArcaneTableBlock(Properties properties) {
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MenuProvider menuProvider && !level.isClientSide) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHandler.sendProgress(serverPlayer);
            }
            player.openMenu(menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ArcaneTableBlockEntity be) {
                if (!level.isClientSide) {
                    net.minecraft.world.Containers.dropContents(level, pos, be);
                }
                BlockPos extensionPos = be.getExtensionPos();
                if (extensionPos != null && level.getBlockState(extensionPos).getBlock() instanceof ArcaneTableExtensionBlock) {
                    level.removeBlock(extensionPos, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
