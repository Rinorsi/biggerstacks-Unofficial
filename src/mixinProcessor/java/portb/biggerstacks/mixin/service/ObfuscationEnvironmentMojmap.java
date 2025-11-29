/*
 * Copyright (c) PORTB
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.service;

import org.spongepowered.tools.obfuscation.ObfuscationEnvironment;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.mapping.IMappingProvider;
import org.spongepowered.tools.obfuscation.mapping.IMappingWriter;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;

public class ObfuscationEnvironmentMojmap extends ObfuscationEnvironment
{
    protected ObfuscationEnvironmentMojmap(ObfuscationType type)
    {
        super(type);
    }

    @Override
    protected IMappingProvider getMappingProvider(Messager messager, Filer filer)
    {
        return new MappingProviderMojmap(messager, filer);
    }

    @Override
    protected IMappingWriter getMappingWriter(Messager messager, Filer filer)
    {
        return new MappingWriterMojmap(messager, filer);
    }
}
