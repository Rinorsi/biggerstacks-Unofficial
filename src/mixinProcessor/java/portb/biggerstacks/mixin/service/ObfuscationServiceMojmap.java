/*
 * Copyright (c) PORTB
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.service;

import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.service.IObfuscationService;
import org.spongepowered.tools.obfuscation.service.ObfuscationTypeDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ObfuscationServiceMojmap implements IObfuscationService
{
    public static final String TYPE_KEY                 = "named";
    public static final String INPUT_FILE_OPTION        = "mojmapMappingFile";
    public static final String EXTRA_INPUT_FILES_OPTION = "mojmapExtraMappingFiles";
    public static final String OUTPUT_FILE_OPTION       = "mojmapOutputFile";

    @Override
    public Set<String> getSupportedOptions()
    {
        return Set.of(INPUT_FILE_OPTION, EXTRA_INPUT_FILES_OPTION, OUTPUT_FILE_OPTION);
    }

    @Override
    public Collection<ObfuscationTypeDescriptor> getObfuscationTypes(IMixinAnnotationProcessor ap)
    {
        if (ap == null)
        {
            return Collections.emptyList();
        }

        List<String> requestedTypes = ap.getOptions("mappingTypes");
        if (requestedTypes == null)
        {
            return Collections.emptyList();
        }

        for (String type : requestedTypes)
        {
            if (TYPE_KEY.equalsIgnoreCase(type))
            {
                return Collections.singletonList(new ObfuscationTypeDescriptor(
                        TYPE_KEY,
                        INPUT_FILE_OPTION,
                        EXTRA_INPUT_FILES_OPTION,
                        OUTPUT_FILE_OPTION,
                        ObfuscationEnvironmentMojmap.class
                ));
            }
        }

        return Collections.emptyList();
    }
}
