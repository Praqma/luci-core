package net.praqma.luci.utils

import groovy.transform.CompileStatic
import groovy.transform.Immutable


class SystemCheck {

    private PrintWriter out

    SystemCheck(PrintWriter out) {
        this.out = out
    }

    boolean perform() {
        out.println ''
        String header = "Luci system check"
        out.println header
        out.println '=' * header.length()
        boolean isWindows = System.properties['os.name'].toLowerCase().contains('windows')
        if (isWindows) {
            out.println "Luci doesn't wan to play. You are using Windows. Please upgrade."
            return false
        }
        out.println "PATH = ${Binary.path.join(':')}"
        Binary.known.values().each { Binary binary ->
            binary.report(out)
            out.println ''
        }
        out.flush()
    }
}
