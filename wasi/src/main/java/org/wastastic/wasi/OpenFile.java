package org.wastastic.wasi;

final class OpenFile {
    private final int nativeFd;
    private long baseRights;
    private long inheritingRights;

    OpenFile(int nativeFd, int baseRights, int inheritingRights) {
        this.nativeFd = nativeFd;
        this.baseRights = baseRights;
        this.inheritingRights = inheritingRights;
    }

    int nativeFd() {
        return nativeFd;
    }

    long baseRights() {
        return baseRights;
    }

    long inheritingRights() {
        return inheritingRights;
    }
}
