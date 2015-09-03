package net.praqma.luci.utils

import com.google.common.io.ByteStreams
import com.google.common.io.Files

/**
 * Helper class to work with resources found on classpath
 */
class ClasspathResources {

    private ClassLoader classLoader

    File extractedResoucesDir = Files.createTempDir()

    ClasspathResources(ClassLoader classLoader = null) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().contextClassLoader
        }
        this.classLoader = classLoader
    }

    File resourceAsFile(String resource) {
        // When running expanded (i.e. not from jar we could retrieve the resource
        // as url and get to the file directly. But it is really the special
        // case to run expanded to lets always get resource and read it
        InputStream stream = classLoader.getResourceAsStream(resource)
        if (stream == null) {
            throw new IllegalArgumentException("Resouces '${resource}' not found")
        }
        File f = File.createTempFile('luci', 'res', extractedResoucesDir)
        f.withOutputStream {
            ByteStreams.copy(stream, it)
        }
        return f
    }
}
