package net.praqma.luci.utils

import groovy.transform.Memoized


class LuciSettings extends Properties {

    @Memoized
    static LuciSettings getInstance() {
        LuciSettings settings = new LuciSettings()
        File f = new File(System.properties['user.home'], '.luci/settings.properties')
        if (!f.exists()) {
            println "Luci settings file not found (${f.path}). Using default settings."
        } else {
            f.withInputStream { settings.load(it) }
        }
        return settings
    }
}
