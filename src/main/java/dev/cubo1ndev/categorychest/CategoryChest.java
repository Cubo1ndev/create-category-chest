package dev.cubo1ndev.categorychest;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CategoryChest.MODID)
public class CategoryChest {
    public static final String MODID = "create_category_chest";
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<CategoryChestMenu>> MENU =
        MENUS.register("category_chest", () -> new MenuType<>(CategoryChestMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public CategoryChest(IEventBus modBus) {
        MENUS.register(modBus);
        modBus.addListener((RegisterPayloadHandlersEvent e) ->
            e.registrar("1").playToServer(OpenPayload.TYPE, OpenPayload.STREAM_CODEC, OpenPayload::handle));
        NeoForge.EVENT_BUS.addListener(CategoryChest::onRightClick);
        NeoForge.EVENT_BUS.addListener(CategoryChestMenu::onOpen);
    }

    /** Sneak + right-click a stock ticker with both hands empty; every other click is Create's. */
    private static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        var p = e.getEntity();
        if (e.getHand() != InteractionHand.MAIN_HAND || !p.isShiftKeyDown()
            || !p.getMainHandItem().isEmpty() || !p.getOffhandItem().isEmpty()) return;
        if (!(e.getLevel().getBlockEntity(e.getPos()) instanceof StockTickerBlockEntity be)) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (p instanceof ServerPlayer sp) CategoryChestMenu.open(sp, be);
    }
}
