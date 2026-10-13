package com.fredplenter.menu;

import com.fredplenter.block.entity.ArcaneTableBlockEntity;
import com.fredplenter.registry.ModMenuTypes;
import com.fredplenter.research.ClientArcaneProgress;
import com.fredplenter.research.Research;
import com.fredplenter.research.ResearchRegistry;
import com.fredplenter.aspect.Aspect;
import com.fredplenter.research.ArcaneProgress;
import com.fredplenter.research.HexPosition;
import com.fredplenter.item.ResearchNoteItem;
import com.fredplenter.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/** The table session validates research on the server; player knowledge lives in SavedData. */
public class ArcaneTableMenu extends AbstractContainerMenu {

    public static final int NOTE_SLOT_X = 41;
    public static final int NOTE_SLOT_Y = 146;

    private final ArcaneTableBlockEntity blockEntity;
    @Nullable
    private ResourceLocation activeResearch;
    /** Creates the server-side session attached to the table block entity. */
    public ArcaneTableMenu(int containerId, Inventory playerInventory, ArcaneTableBlockEntity blockEntity) {
        super(ModMenuTypes.ARCANE_TABLE.get(), containerId);
        this.blockEntity = blockEntity;
        addTableSlot(blockEntity);
        addPlayerInventorySlots(playerInventory);
    }

    /** Creates the client-side session for the table position sent by the server. */
    public ArcaneTableMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.ARCANE_TABLE.get(), containerId);
        Level level = playerInventory.player.level();
        if (level.getBlockEntity(pos) instanceof ArcaneTableBlockEntity be) {
            this.blockEntity = be;
        } else {
            this.blockEntity = null;
        }
        addTableSlot(this.blockEntity);
        addPlayerInventorySlots(playerInventory);
    }

    private void addTableSlot(ArcaneTableBlockEntity blockEntity) {
        if (blockEntity != null) {
            addSlot(new Slot(blockEntity, 0, NOTE_SLOT_X, NOTE_SLOT_Y) {
                @Override
                public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                    return blockEntity.canPlaceItem(0, stack);
                }
            });
        } else {
            addSlot(new Slot(new net.minecraft.world.SimpleContainer(1), 0, NOTE_SLOT_X, NOTE_SLOT_Y) {
                @Override
                public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                    return stack.is(ModItems.RESEARCH_NOTE.get());
                }
            });
        }
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        int inventoryLeft = 8;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        inventoryLeft + column * 18, 184 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, inventoryLeft + column * 18, 242));
        }
    }

    public Set<ResourceLocation> getCompletedResearch() {
        return ClientArcaneProgress.getCompletedResearch();
    }

    public boolean isAvailable(Research research) {
        return ResearchRegistry.isAvailable(research, getCompletedResearch());
    }

    public int getAspectPoints(Aspect aspect) {
        return ClientArcaneProgress.getAspectPoints(aspect);
    }

    @Nullable
    public ResourceLocation getResearchNoteId() {
        return ResearchNoteItem.researchId(getNoteStack());
    }

    public boolean canStartResearch(Research research) {
        if (research.method() != Research.Method.PAPER || !isAvailable(research)) {
            return false;
        }
        return isNoteFor(research) && !ResearchNoteItem.isComplete(getNoteStack());
    }

    public boolean beginResearch(ServerPlayer player, ResourceLocation researchId) {
        Research research = ResearchRegistry.get(researchId);
        if (activeResearch != null || research == null || research.method() != Research.Method.PAPER
                || !isNoteFor(research) || ResearchNoteItem.isComplete(getNoteStack())
                || !ResearchRegistry.isAvailable(research, ArcaneProgress.get(player).getCompletedResearch(player.getUUID()))) {
            return false;
        }
        activeResearch = researchId;
        return true;
    }

    public boolean completeResearch(ServerPlayer player, ResourceLocation researchId,
                                    java.util.List<Aspect> solution, java.util.List<HexPosition> path) {
        if (!researchId.equals(activeResearch)
                || blockEntity == null
                || !isNoteFor(ResearchRegistry.get(researchId))
                || ResearchNoteItem.isComplete(getNoteStack())
                || !ArcaneProgress.get(player).completeResearch(player, researchId, solution, path)) {
            return false;
        }
        ResearchNoteItem.markComplete(getNoteStack());
        blockEntity.setChanged();
        activeResearch = null;
        return true;
    }

    private boolean isNoteFor(Research research) {
        return research != null && blockEntity != null
                && ModItems.RESEARCH_NOTE.get() == getNoteStack().getItem()
                && research.id().equals(ResearchNoteItem.researchId(getNoteStack()));
    }

    private net.minecraft.world.item.ItemStack getNoteStack() {
        return blockEntity == null ? net.minecraft.world.item.ItemStack.EMPTY : blockEntity.getItem(0);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && blockEntity.getLevel() != null
                && player.distanceToSqr(
                        blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        net.minecraft.world.item.ItemStack original = slot.getItem();
        net.minecraft.world.item.ItemStack moved = original.copy();
        if (index == 0) {
            if (!moveItemStackTo(original, 1, slots.size(), true)) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        } else if (original.is(ModItems.RESEARCH_NOTE.get())) {
            if (!moveItemStackTo(original, 0, 1, false)) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        } else {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(net.minecraft.world.item.ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (original.getCount() == moved.getCount()) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        slot.onTake(player, original);
        return moved;
    }
}
