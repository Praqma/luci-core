package net.praqma.luci.utils

/**
 * Helper class to work with resources found on classpath
 */
class ClasspathResources {

    private ClassLoader classLoader

    ClasspathResources(ClassLoader classLoader = null) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().contextClassLoader
        }
        this.classLoader = classLoader
    }

    File resourceAsFile(String resource) {
        URL url = classLoader.getResource(resource)
        if (url == null) {
            throw new IllegalArgumentException("Resouces '${resource}' not found")
        }
        if (url.protocol != 'file') {
            // TODO implement when executing from jar
            throw new RuntimeException("Not implemented yet handling non-file resources")
        }
        // See https://weblogs.java.net/blog/kohsuke/archive/2007/04/how_to_convert.html
        try {
            return new File(url.toURI())
        } catch (URISyntaxException) {
            return new File(url.path)
        }
    }
}
