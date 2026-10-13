package com.fredplenter.registry;

import com.fredplenter.Fredplenter;
import com.fredplenter.menu.ArcaneTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, Fredplenter.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneTableMenu>> ARCANE_TABLE = MENUS.register(
            "arcane_table",
            () -> IMenuTypeExtension.create((containerId, inventory, buf) ->
                    new ArcaneTableMenu(containerId, inventory, buf.readBlockPos())));

    private ModMenuTypes() {
    }
}
