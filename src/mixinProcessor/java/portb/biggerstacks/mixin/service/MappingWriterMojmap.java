/*
 * Copyright (c) PORTB
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.service;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer.MappingSet;
import org.spongepowered.tools.obfuscation.mapping.IMappingWriter;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

public class MappingWriterMojmap implements IMappingWriter
{
    private final Messager messager;

    public MappingWriterMojmap(Messager messager, Filer filer)
    {
        this.messager = messager;
    }

    @Override
    public void write(String outputName,
                      ObfuscationType type,
                      MappingSet<MappingField> fields,
                      MappingSet<MappingMethod> methods)
    {
        if (outputName != null && !outputName.isEmpty())
        {
            this.messager.printMessage(Diagnostic.Kind.NOTE,
                                       "Skipping generation of reobf data for " + type + " (" + outputName + ")");
        }
    }
}
