package org.wastastic.compiler;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

final class Tuples {
    private Tuples() {}

    static @NotNull Class<?> getTupleClass(@NotNull String signature) {
        return TUPLE_CLASSES.computeIfAbsent(requireNonNull(signature), sig -> {
            if (sig.length() < 2) {
                throw new IllegalArgumentException("Invalid signature: '" + sig + "'");
            }

            var internalName = "org/wastastic/Tuple-" + sig;

            var writer = new ClassWriter(0);
            writer.visit(Opcodes.V16, Opcodes.ACC_FINAL, internalName, null, "java/lang/Object", null);

            var constructorDescriptor = new StringBuilder("(");

            for (var i = 0; i < sig.length(); i++) {
                var descriptor = switch (sig.charAt(i)) {
                    case 'I' -> "I";
                    case 'J' -> "J";
                    case 'F' -> "F";
                    case 'D' -> "D";
                    case 'L' -> "Ljava/lang/Object;";
                    default -> throw new IllegalArgumentException("Invalid signature: '" + sig + "'");
                };

                writer.visitField(Opcodes.ACC_FINAL, Integer.toString(i), descriptor, null, null);
                constructorDescriptor.append(descriptor);
            }

            constructorDescriptor.append(")V");

            var constructorWriter = writer.visitMethod(0, "<init>", constructorDescriptor.toString(), null, null);
            constructorWriter.visitCode();

            var maxStack = 3;
            var localIndex = 0;
            for (var i = 0; i < sig.length(); i++) {
                constructorWriter.visitInsn(Opcodes.DUP);
                switch (sig.charAt(i)) {
                    case 'I' -> {
                        constructorWriter.visitVarInsn(Opcodes.ILOAD, localIndex);
                        constructorWriter.visitFieldInsn(Opcodes.PUTFIELD, internalName, Integer.toString(i), "I");
                        localIndex += 1;
                    }

                    case 'J' -> {
                        constructorWriter.visitVarInsn(Opcodes.LLOAD, localIndex);
                        constructorWriter.visitFieldInsn(Opcodes.PUTFIELD, internalName, Integer.toString(i), "J");
                        localIndex += 2;
                        maxStack = 4;
                    }

                    case 'F' -> {
                        constructorWriter.visitVarInsn(Opcodes.FLOAD, localIndex);
                        constructorWriter.visitFieldInsn(Opcodes.PUTFIELD, internalName, Integer.toString(i), "F");
                        localIndex += 1;
                    }

                    case 'D' -> {
                        constructorWriter.visitVarInsn(Opcodes.DLOAD, localIndex);
                        constructorWriter.visitFieldInsn(Opcodes.PUTFIELD, internalName, Integer.toString(i), "D");
                        localIndex += 2;
                        maxStack = 4;
                    }

                    case 'L' -> {
                        constructorWriter.visitVarInsn(Opcodes.ALOAD, localIndex);
                        constructorWriter.visitFieldInsn(
                            Opcodes.PUTFIELD,
                            internalName,
                            Integer.toString(i),
                            "Ljava/lang/Object"
                        );
                        localIndex += 1;
                    }
                }
            }

            constructorWriter.visitInsn(Opcodes.RETURN);
            constructorWriter.visitMaxs(maxStack, localIndex);
            constructorWriter.visitEnd();

            writer.visitEnd();

            try {
                return MethodHandles.lookup().defineClass(writer.toByteArray());
            } catch (IllegalAccessException exception) {
                throw new UnsupportedOperationException("Unable to define tuple class", exception);
            }
        });
    }

    private static final ConcurrentHashMap<String, Class<?>> TUPLE_CLASSES = new ConcurrentHashMap<>();
}
