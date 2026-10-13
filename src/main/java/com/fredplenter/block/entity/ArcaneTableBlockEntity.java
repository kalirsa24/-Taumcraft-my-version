package com.fredplenter.block.entity;

import com.fredplenter.menu.ArcaneTableMenu;
import com.fredplenter.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.NonNullList;
import com.fredplenter.registry.ModItems;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Stores the table's research-note slot and its formed-table position link. */
public class ArcaneTableBlockEntity extends BlockEntity implements MenuProvider, Container {

    @Nullable
    private BlockPos extensionPos;
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public ArcaneTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_TABLE.get(), pos, state);
    }

    public void setExtensionPos(@Nullable BlockPos pos) {
        this.extensionPos = pos;
        setChanged();
    }

    @Nullable
    public BlockPos getExtensionPos() {
        return extensionPos;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (extensionPos != null) {
            tag.putLong("ExtensionPos", extensionPos.asLong());
        }
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        extensionPos = tag.contains("ExtensionPos") ? BlockPos.of(tag.getLong("ExtensionPos")) : null;
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fredplenter.arcane_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ArcaneTableMenu(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.getFirst().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? items.getFirst() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot == 0 ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.is(ModItems.RESEARCH_NOTE.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }
}
