package dev.cubo1ndev.categorychest;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client asks to open the category chest of the stock ticker at pos (the button in Create's screen). */
public record OpenPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CategoryChest.MODID, "open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> STREAM_CODEC =
        BlockPos.STREAM_CODEC.map(OpenPayload::new, OpenPayload::pos).cast();

    @Override public Type<OpenPayload> type() { return TYPE; }

    static void handle(OpenPayload msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer p) || !p.level().isLoaded(msg.pos())
            || p.distanceToSqr(msg.pos().getCenter()) > 64) return;
        if (p.level().getBlockEntity(msg.pos()) instanceof StockTickerBlockEntity be) CategoryChestMenu.open(p, be);
    }
}
