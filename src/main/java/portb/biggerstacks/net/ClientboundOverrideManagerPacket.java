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
import portb.biggerstacks.Constants;
import portb.biggerstacks.config.TemplateOverrideConfig;

import java.util.ArrayList;
import java.util.List;

public record ClientboundOverrideManagerPacket(List<TemplateOverrideConfig.StackOverride> overrides) implements CustomPacketPayload
{
    public static final Type<ClientboundOverrideManagerPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "open_override_manager"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOverrideManagerPacket> STREAM_CODEC = StreamCodec.of(
            ClientboundOverrideManagerPacket::write,
            ClientboundOverrideManagerPacket::read);

    public ClientboundOverrideManagerPacket(RegistryFriendlyByteBuf buf)
    {
        this(readOverrides(buf));
    }

    private static ClientboundOverrideManagerPacket read(RegistryFriendlyByteBuf buf)
    {
        return new ClientboundOverrideManagerPacket(buf);
    }

    private static void write(RegistryFriendlyByteBuf buf, ClientboundOverrideManagerPacket packet)
    {
        buf.writeVarInt(packet.overrides.size());
        for (TemplateOverrideConfig.StackOverride entry : packet.overrides)
        {
            writeOverride(buf, entry);
        }
    }

    private static List<TemplateOverrideConfig.StackOverride> readOverrides(RegistryFriendlyByteBuf buf)
    {
        int size = buf.readVarInt();
        List<TemplateOverrideConfig.StackOverride> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            result.add(readOverride(buf));
        }
        return result;
    }

    static TemplateOverrideConfig.StackOverride readOverride(RegistryFriendlyByteBuf buf)
    {
        TemplateOverrideConfig.TargetType targetType = TemplateOverrideConfig.TargetType.valueOf(buf.readUtf());
        TemplateOverrideConfig.StackCategory category = TemplateOverrideConfig.StackCategory.valueOf(buf.readUtf());
        String target = buf.readUtf();
        return new TemplateOverrideConfig.StackOverride(targetType, target, category);
    }

    static void writeOverride(RegistryFriendlyByteBuf buf, TemplateOverrideConfig.StackOverride overrideEntry)
    {
        buf.writeUtf(overrideEntry.targetType().name());
        buf.writeUtf(overrideEntry.category().name());
        buf.writeUtf(overrideEntry.target());
    }

    @Override
    public Type<ClientboundOverrideManagerPacket> type()
    {
        return TYPE;
    }
}
