package dev.cubo1ndev.categorychest;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryMenu;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.foundation.utility.AdventureUtil;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A 6-row chest whose contents are a stock ticker's categories; every change is applied to the ticker at once. */
public class CategoryChestMenu extends ChestMenu {
    public static final int SLOTS = 54;
    private static final Field CATEGORIES = field("categories"), HIDDEN = field("hiddenCategoriesByPlayer");

    private final StockTickerBlockEntity be; // null on the client
    private List<ItemStack> lastWritten, overflow;

    /** Client side: the server fills the slots. */
    public CategoryChestMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(SLOTS), null);
    }

    private CategoryChestMenu(int id, Inventory inv, SimpleContainer box, StockTickerBlockEntity be) {
        super(CategoryChest.MENU.get(), id, inv, box, 6);
        this.be = be;
        for (int i = 0; i < SLOTS; i++) { // vanilla chest slots accept anything: swap in filter-only ones
            Slot old = slots.get(i);
            Slot s = new Slot(box, i, old.x, old.y) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof FilterItem && !stale(); }
                @Override public boolean mayPickup(Player p) { return !stale(); }
                @Override public int getMaxStackSize() { return 1; } // blank filters stack; one slot is one category
            };
            s.index = i;
            slots.set(i, s);
        }
        if (be == null) return;
        lastWritten = categories(be);
        overflow = Remap.overflow(lastWritten, SLOTS);
        for (int i = 0; i < SLOTS && i < lastWritten.size(); i++) box.setItem(i, lastWritten.get(i).copy());
        box.addListener(c -> apply(box));
    }

    /** Someone else replaced the categories (Create's own screen) or the ticker is gone: this chest is no longer them. */
    boolean stale() {
        if (be == null) return false;
        if (be.isRemoved()) return true;
        List<ItemStack> now = categories(be);
        if (now == lastWritten) return false;
        // Create's category screen sends its list back when it closes (the button path): same contents, not a change.
        if (!ItemStack.listMatches(now, lastWritten)) return true;
        lastWritten = now;
        return false;
    }

    private void apply(Container box) {
        if (stale()) return;
        List<ItemStack> chest = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) if (!box.getItem(i).isEmpty()) chest.add(box.getItem(i).copy());
        List<ItemStack> old = lastWritten;
        List<ItemStack> neu = new ArrayList<>(chest);
        for (ItemStack s : overflow) neu.add(s.copy());
        int[] map = Remap.remap(old, neu, ItemStack::matches);
        try {
            CATEGORIES.set(be, neu);
            @SuppressWarnings("unchecked") Map<UUID, List<Integer>> hidden = (Map<UUID, List<Integer>>) HIDDEN.get(be);
            Map<UUID, List<Integer>> moved = Remap.remapHidden(hidden, map);
            hidden.clear();
            hidden.putAll(moved);
        } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        lastWritten = neu;
        be.notifyUpdate();
    }

    @Override
    public boolean stillValid(Player p) {
        return be == null || !stale() && p.distanceToSqr(be.getBlockPos().getCenter()) <= 64;
    }

    /** Opens the chest for a stock ticker, if the player may use it and nobody else has it open. */
    public static void open(ServerPlayer p, StockTickerBlockEntity be) {
        if (p.isSpectator() || AdventureUtil.isAdventure(p)) return; // Create refuses their category edits too
        LogisticallyLinkedBehaviour link = BlockEntityBehaviour.get(be, LogisticallyLinkedBehaviour.TYPE);
        if (link != null && !link.mayInteractMessage(p)) return;
        // Create's own category screen keeps a client copy of the list and writes it back on close: never both at once.
        boolean busy = p.server.getPlayerList().getPlayers().stream().anyMatch(o -> o != p && editing(o, be));
        if (busy) {
            p.displayClientMessage(Component.translatable("create_category_chest.busy"), true);
            return;
        }
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new CategoryChestMenu(id, inv, new SimpleContainer(SLOTS), be),
            Component.translatable("create_category_chest.title")));
    }

    private static boolean editing(Player o, StockTickerBlockEntity be) {
        return o.containerMenu instanceof CategoryChestMenu m && m.be == be
            || o.containerMenu instanceof StockKeeperCategoryMenu k && k.contentHolder == be;
    }

    /** Someone opened Create's category screen on a ticker: close any chest on it (the chest is the ticker, nothing is lost). */
    static void onOpen(PlayerContainerEvent.Open e) {
        if (!(e.getContainer() instanceof StockKeeperCategoryMenu m) || !(e.getEntity() instanceof ServerPlayer p)) return;
        for (ServerPlayer o : p.server.getPlayerList().getPlayers())
            if (o.containerMenu instanceof CategoryChestMenu c && c.be == m.contentHolder) o.closeContainer();
    }

    @SuppressWarnings("unchecked")
    static List<ItemStack> categories(StockTickerBlockEntity be) {
        try { return (List<ItemStack>) CATEGORIES.get(be); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }

    private static Field field(String name) {
        try {
            Field f = StockTickerBlockEntity.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) { throw new IllegalStateException("Create changed StockTickerBlockEntity." + name, e); }
    }
}
