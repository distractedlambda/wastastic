module wastastic.core {
    requires org.objectweb.asm;
    requires org.objectweb.asm.util;
    requires transitive jdk.incubator.foreign;
    requires static org.jetbrains.annotations;
    exports org.wastastic;
}
