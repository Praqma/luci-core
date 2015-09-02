package net.praqma.luci.docker

enum ContainerKind {
    UNKNOWN,
    SERVICE,
    STORAGE,
    CACHE,


    static ContainerKind from(String s) {
        try {
            return valueOf(s.toUpperCase())
        } catch (IllegalArgumentException) {
            println "Illegal value of ContainerKind: '${s}'"
            return UNKNOWN
        }
    }
}
