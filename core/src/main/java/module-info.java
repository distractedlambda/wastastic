module wastastic.core {
    requires org.objectweb.asm;
    requires transitive jdk.incubator.foreign;
    requires transitive org.jetbrains.annotations;
    exports org.wastastic;
}
