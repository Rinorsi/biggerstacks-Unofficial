/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import portb.biggerstacks.Constants;
import portb.biggerstacks.config.StackSizeRules;
import portb.biggerstacks.config.TemplateOverrideConfig;
import portb.configlib.template.ConfigTemplate;
import portb.configlib.xml.Condition;
import portb.configlib.xml.Operator;
import portb.configlib.xml.OrBlock;
import portb.configlib.xml.Property;
import portb.configlib.xml.Rule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ServerboundCreateConfigTemplatePacket extends GenericTemplateOptionsPacket implements CustomPacketPayload
{
    public static final Type<ServerboundCreateConfigTemplatePacket>                                 TYPE         = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "create_template"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCreateConfigTemplatePacket> STREAM_CODEC = StreamCodec.of(
            ServerboundCreateConfigTemplatePacket::write,
            ServerboundCreateConfigTemplatePacket::read);
    
    public ServerboundCreateConfigTemplatePacket(int normalStackLimit, int potionsStackLimit, int enchantedBooksStackLimit)
    {
        super(normalStackLimit, potionsStackLimit, enchantedBooksStackLimit);
    }
    
    public ServerboundCreateConfigTemplatePacket(RegistryFriendlyByteBuf buf)
    {
        super(buf);
    }
    
    private static ServerboundCreateConfigTemplatePacket read(RegistryFriendlyByteBuf buf)
    {
        return new ServerboundCreateConfigTemplatePacket(buf);
    }
    
    private static void write(RegistryFriendlyByteBuf buf, ServerboundCreateConfigTemplatePacket packet)
    {
        packet.encode(buf);
    }
    
    static void handleCreateConfigTemplate(ServerboundCreateConfigTemplatePacket serverboundCreateConfigTemplatePacket, IPayloadContext context)
    {
        //If on a server, check that the player actually has permissions to do this
        if (FMLEnvironment.dist.isDedicatedServer())
        {
            if (!(context.player() instanceof ServerPlayer sender) ||
                        !sender.hasPermissions(Constants.CHANGE_STACK_SIZE_COMMAND_PERMISSION_LEVEL))
                return;
        }
        
        ConfigTemplate template = ConfigTemplate.generateTemplate(serverboundCreateConfigTemplatePacket);
        TemplateOverrideConfig.getOverrides().forEach(entry -> {
            int stackSize = StackSizeRules.clampStackSize(entry.resolveStackSize(serverboundCreateConfigTemplatePacket));
            template.getRules().add(0,
                                    new Rule(Collections.singletonList(entry.toCondition()),
                                            stackSize)
            );
        });

        if (ModList.get().isLoaded("ic2"))
        {
            //limit ic2 upgrades to 64 (if it is installed), or issues might occur with putting too many into machines
            template.getRules().add(new Rule(Arrays.asList(
                                            new Condition(Property.MOD_ID, Operator.EQUALS, "ic2"),
                                            new OrBlock(
                                                    Arrays.asList(
                                                            new Condition(Property.ID, Operator.STRING_STARTS_WITH, "ic2:upgrade"),
                                                            new Condition(Property.ID, Operator.STRING_ENDS_WITH, "pad_upgrade"),
                                                            new Condition(Property.ID, Operator.STRING_ENDS_WITH, "upgrade_kit")
                                                    )
                                            )
                                    ), 64)
            );
        }
        
        try
        {
            Files.writeString(Constants.RULESET_FILE,
                              template.toXML(),
                              StandardOpenOption.CREATE,
                              StandardOpenOption.TRUNCATE_EXISTING
            );
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public void encode(FriendlyByteBuf buf)
    {
        super.encode(buf);
    }
    
    @Override
    public Type<ServerboundCreateConfigTemplatePacket> type()
    {
        return TYPE;
    }
}
