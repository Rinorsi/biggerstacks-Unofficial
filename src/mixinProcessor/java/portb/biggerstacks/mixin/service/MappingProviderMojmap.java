/*
 * Copyright (c) PORTB
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.service;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.common.MappingProvider;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingProviderMojmap extends MappingProvider
{
    private static final Pattern CLASS_PATTERN  = Pattern.compile("^([^\\s]+) -> ([^\\s]+):$");
    private static final Pattern METHOD_PATTERN = Pattern.compile("^(?:\\d+:\\d+:)?([^\\s]+)\\s+([^\\s(]+)\\(([^)]*)\\) -> (\\S+)$");
    private static final Pattern FIELD_PATTERN  = Pattern.compile("^([^\\s]+)\\s+([^\\s]+) -> (\\S+)$");

    public MappingProviderMojmap(Messager messager, Filer filer)
    {
        super(messager, filer);
    }

    @Override
    public void read(File input) throws IOException
    {
        if (input == null || !input.isFile())
        {
            this.messager.printMessage(Diagnostic.Kind.ERROR, "Mapping file not found: " + input);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(input.toPath(), StandardCharsets.UTF_8))
        {
            String currentDeobfClass = null;
            String currentObfClass   = null;

            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.isEmpty() || line.startsWith("#"))
                {
                    continue;
                }

                if (!Character.isWhitespace(line.charAt(0)))
                {
                    Matcher matcher = CLASS_PATTERN.matcher(line.trim());
                    if (matcher.matches())
                    {
                        currentDeobfClass = toInternalName(matcher.group(1));
                        currentObfClass = toInternalName(matcher.group(2));
                        this.classMap.forcePut(currentDeobfClass, currentObfClass);
                    }
                    else
                    {
                        warn("Failed to parse class line '" + line + "'");
                    }
                    continue;
                }

                if (currentDeobfClass == null || currentObfClass == null)
                {
                    continue;
                }

                String body = line.trim();
                if (body.isEmpty())
                {
                    continue;
                }

                if (body.indexOf('(') >= 0)
                {
                    parseMethodLine(body, currentDeobfClass, currentObfClass);
                }
                else
                {
                    parseFieldLine(body, currentDeobfClass, currentObfClass);
                }
            }
        }
    }

    private void parseMethodLine(String body, String ownerDeobf, String ownerObf)
    {
        Matcher matcher = METHOD_PATTERN.matcher(body);
        if (!matcher.matches())
        {
            warn("Failed to parse method mapping '" + body + "'");
            return;
        }

        String returnType = matcher.group(1);
        String name       = matcher.group(2);
        String params     = matcher.group(3);
        String obfName    = matcher.group(4);

        String descriptor = '(' + mapParameterList(params) + ')' + mapType(returnType);
        this.methodMap.forcePut(new MappingMethod(ownerDeobf, name, descriptor),
                                new MappingMethod(ownerObf, obfName, descriptor));
    }

    private void parseFieldLine(String body, String ownerDeobf, String ownerObf)
    {
        Matcher matcher = FIELD_PATTERN.matcher(body);
        if (!matcher.matches())
        {
            warn("Failed to parse field mapping '" + body + "'");
            return;
        }

        String type    = matcher.group(1);
        String name    = matcher.group(2);
        String obfName = matcher.group(3);

        String descriptor = mapType(type);
        this.fieldMap.forcePut(new MappingField(ownerDeobf, name, descriptor),
                               new MappingField(ownerObf, obfName, descriptor));
    }

    private static String toInternalName(String name)
    {
        return name.replace('.', '/');
    }

    private static String mapParameterList(String parameters)
    {
        if (parameters == null || parameters.isBlank())
        {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        Arrays.stream(parameters.split(","))
              .map(String::trim)
              .filter(param -> !param.isEmpty())
              .map(MappingProviderMojmap::mapType)
              .forEach(builder::append);
        return builder.toString();
    }

    private static String mapType(String type)
    {
        String trimmed = type.trim();
        int    depth   = 0;

        while (trimmed.endsWith("[]"))
        {
            depth++;
            trimmed = trimmed.substring(0, trimmed.length() - 2);
        }

        String descriptor;
        switch (trimmed)
        {
            case "void":
                descriptor = "V";
                break;
            case "boolean":
                descriptor = "Z";
                break;
            case "byte":
                descriptor = "B";
                break;
            case "char":
                descriptor = "C";
                break;
            case "short":
                descriptor = "S";
                break;
            case "int":
                descriptor = "I";
                break;
            case "long":
                descriptor = "J";
                break;
            case "float":
                descriptor = "F";
                break;
            case "double":
                descriptor = "D";
                break;
            default:
                descriptor = 'L' + toInternalName(trimmed) + ';';
                break;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < depth; i++)
        {
            builder.append('[');
        }
        builder.append(descriptor);
        return builder.toString();
    }

    private void warn(String message)
    {
        this.messager.printMessage(Diagnostic.Kind.WARNING, message);
    }
}
