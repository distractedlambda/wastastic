package org.wastastic;

import org.objectweb.asm.Type;

import static org.objectweb.asm.Type.getInternalName;

public final class UnreachableException extends IllegalStateException {
    UnreachableException() {}

    static String INTERNAL_NAME = getInternalName(UnreachableException.class);
}
