package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.objectweb.asm.Type;

import java.lang.invoke.VarHandle;

final class Names {
    private Names() {}

    static final String VAR_HANDLE_INTERNAL_NAME = Type.getInternalName(VarHandle.class);
    static final String VAR_HANDLE_DESCRIPTOR = Type.getDescriptor(VarHandle.class);

    static final String MEMORY_SEGMENT_INTERNAL_NAME = Type.getInternalName(MemorySegment.class);
    static final String MEMORY_SEGMENT_DESCRIPTOR = Type.getDescriptor(MemorySegment.class);

    static final String GENERATED_INSTANCE_INTERNAL_NAME = "org/wastastic/GeneratedModuleInstance";
    static final String GENERATED_INSTANCE_DESCRIPTOR = "Lorg/wastastic/GeneratedModuleInstance;";
    static final String GENERATED_CONSTRUCTOR_DESCRIPTOR = "(Ljava/util/Map;)V";
}
