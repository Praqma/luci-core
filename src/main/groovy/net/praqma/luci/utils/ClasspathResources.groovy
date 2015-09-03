package net.praqma.luci.utils

import com.google.common.io.ByteStreams
import com.google.common.io.Files
import groovy.transform.CompileStatic

/**
 * Helper class to work with resources found on classpath
 */
@CompileStatic
class ClasspathResources {

    private ClassLoader classLoader

    static File extractedResoucesDir

    ClasspathResources(Class<?> cls) {
        this(cls.classLoader)
    }

    ClasspathResources(ClassLoader classLoader = null) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().contextClassLoader
        }
        this.classLoader = classLoader
    }

    static File getExtractedResoucesDir() {
        if (this.@extractedResoucesDir == null) {
            this.@extractedResoucesDir = Files.createTempDir()
        }
        this.@extractedResoucesDir.mkdirs()
        return this.@extractedResoucesDir
    }

    File resourceAsFile(String resource, String name = null) {
        if (name == null) {
            // Use last part of resource as name
            name = new File(resource).name
        }
        File target = new File(extractedResoucesDir, name)
        if (!target.exists()) {
            // When running expanded (i.e. not from jar we could retrieve the resource
            // as url and get to the file directly. But it is really the special
            // case to run expanded to lets always get resource and read it
            InputStream stream = classLoader.getResourceAsStream(resource)
            if (stream == null) {
                throw new IllegalArgumentException("Resouces '${resource}' not found")
            }
            target.withOutputStream {
                ByteStreams.copy(stream, it)
            }
        }
        return target
    }
}
