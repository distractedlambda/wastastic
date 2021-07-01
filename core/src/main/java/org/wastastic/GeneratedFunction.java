package org.wastastic;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

interface GeneratedFunction {
    String INTERNAL_NAME = getInternalName(GeneratedFunction.class);

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface FunctionName {
        @NotNull String value();

        String DESCRIPTOR = getDescriptor(FunctionName.class);
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ModuleName {
        @NotNull String value();

        String DESCRIPTOR = getDescriptor(ModuleName.class);
    }
}
