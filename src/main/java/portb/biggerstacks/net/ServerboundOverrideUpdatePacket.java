/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import portb.biggerstacks.Constants;
import portb.biggerstacks.config.TemplateOverrideConfig;

import java.util.ArrayList;
import java.util.List;

public record ServerboundOverrideUpdatePacket(List<TemplateOverrideConfig.StackOverride> overrides) implements CustomPacketPayload
{
    public static final Type<ServerboundOverrideUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "update_overrides"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundOverrideUpdatePacket> STREAM_CODEC = StreamCodec.of(
            ServerboundOverrideUpdatePacket::write,
            ServerboundOverrideUpdatePacket::read);

    public ServerboundOverrideUpdatePacket(RegistryFriendlyByteBuf buf)
    {
        this(readOverrides(buf));
    }

    private static ServerboundOverrideUpdatePacket read(RegistryFriendlyByteBuf buf)
    {
        return new ServerboundOverrideUpdatePacket(buf);
    }

    private static void write(RegistryFriendlyByteBuf buf, ServerboundOverrideUpdatePacket packet)
    {
        buf.writeVarInt(packet.overrides.size());
        for (TemplateOverrideConfig.StackOverride entry : packet.overrides)
        {
            ClientboundOverrideManagerPacket.writeOverride(buf, entry);
        }
    }

    private static List<TemplateOverrideConfig.StackOverride> readOverrides(RegistryFriendlyByteBuf buf)
    {
        int size = buf.readVarInt();
        List<TemplateOverrideConfig.StackOverride> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            list.add(ClientboundOverrideManagerPacket.readOverride(buf));
        }
        return list;
    }

    public static void handle(ServerboundOverrideUpdatePacket packet, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isDedicatedServer())
            {
                if (!(context.player() instanceof ServerPlayer sender) ||
                        !sender.hasPermissions(Constants.CHANGE_STACK_SIZE_COMMAND_PERMISSION_LEVEL))
                {
                    return;
                }
            }

            TemplateOverrideConfig.updateOverrides(packet.overrides());
            TemplateOverrideConfig.reload();

            if (context.player() instanceof ServerPlayer serverPlayer)
            {
                PacketDistributor.sendToPlayer(serverPlayer, new ClientboundOverrideManagerPacket(TemplateOverrideConfig.getOverrides()));
            }
        });
    }

    @Override
    public Type<ServerboundOverrideUpdatePacket> type()
    {
        return TYPE;
    }
}
