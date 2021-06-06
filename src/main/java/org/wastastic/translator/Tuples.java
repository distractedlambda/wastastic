package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V16;

final class Tuples {
    private Tuples() {}

    static @NotNull Class<?> getTupleClass(@NotNull List<ValueType> types) {
        return TUPLE_CLASSES.computeIfAbsent(types, key -> {
            var suffixChars = new char[key.size()];
            for (var i = 0; i < suffixChars.length; i++) {
                suffixChars[i] = key.get(i).getTupleSuffixChar();
            }

            var internalName = "org/wastastic/Tuple-" + new String(suffixChars);

            var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            writer.visit(V16, ACC_FINAL, internalName, null, "java/lang/Object", null);

            var constructorDescriptor = new StringBuilder("(");

            for (var i = 0; i < key.size(); i++) {
                var type = key.get(i);
                var fieldDescriptor = type.getErasedDescriptor();
                writer.visitField(ACC_FINAL, "" + i, fieldDescriptor, null, null);
                constructorDescriptor.append(fieldDescriptor);
            }

            constructorDescriptor.append(")V");

            var constructorWriter = writer.visitMethod(0, "<init>", constructorDescriptor.toString(), null, null);
            constructorWriter.visitCode();

            var localIndex = 0;
            for (var i = 0; i < key.size(); i++) {
                var type = key.get(i);
                constructorWriter.visitInsn(DUP);
                constructorWriter.visitVarInsn(type.getLocalLoadOpcode(), localIndex);
                constructorWriter.visitFieldInsn(PUTFIELD, internalName, "" + i, type.getErasedDescriptor());
                localIndex += type.getWidth();
            }

            constructorWriter.visitInsn(RETURN);
            constructorWriter.visitMaxs(0, 0);
            constructorWriter.visitEnd();

            writer.visitEnd();

            try {
                return MethodHandles.lookup().defineClass(writer.toByteArray());
            } catch (IllegalAccessException exception) {
                throw new UnsupportedOperationException("Unable to define tuple class", exception);
            }
        });
    }

    private static final ConcurrentHashMap<List<ValueType>, Class<?>> TUPLE_CLASSES = new ConcurrentHashMap<>();
}
