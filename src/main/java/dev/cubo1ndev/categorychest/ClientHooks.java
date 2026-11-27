package dev.cubo1ndev.categorychest;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CategoryChest.MODID, value = Dist.CLIENT)
public final class ClientHooks {
    private ClientHooks() {}

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent e) {
        e.register(CategoryChest.MENU.get(), ContainerScreen::new);
    }

    /** Adds the "Category Chest" button beside Create's category screen; Create's screen is otherwise untouched. */
    @SubscribeEvent
    static void init(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof StockKeeperCategoryScreen s)) return;
        var be = s.getMenu().contentHolder;
        Button b = Button.builder(Component.translatable("create_category_chest.button"),
                btn -> { // close Create's screen first: it sends its list (unsaved edits too) before the chest opens on it
                    s.getMinecraft().player.closeContainer();
                    PacketDistributor.sendToServer(new OpenPayload(be.getBlockPos()));
                })
            .bounds(s.getGuiLeft() + s.getXSize() + 4, s.getGuiTop() + 4, 90, 20).build();
        if (CategoryChestMenu.categories(be).size() > CategoryChestMenu.SLOTS)
            b.setTooltip(Tooltip.create(Component.translatable("create_category_chest.overflow")));
        e.addListener(b);
    }
}
