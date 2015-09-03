package net.praqma.luci.docker

import groovy.transform.Memoized

/**
 * Constants for docker images
 *
 * Version numbers are read from imageVerions.properties
 */
enum Images {

    DATA('debian:jessie'),
    STORAGE('luci/data'),
    MIXIN_JAVA8('luci/mixin-java8'),

    SERVICE_JENKINS('luci/jenkins'),
    SERVICE_NGINX('luci/nginx'),
    SERVICE_ARTIFACTORY('luci/artifactory'),

    TOOLS('luci/tools')

    final String imageString

    Images(String imageString) {
        this.imageString = ensureVersion(imageString)
    }

    private static Map<String, String> m

    private static String ensureVersion(String imageString) {
        String prefix = 'luci/'
        if (m == null) m = versionMap()
        if (imageString.startsWith(prefix)) {
            if (imageString.indexOf(':') == -1) {
                String version = m[imageString.substring(prefix.length())]
                if (version == null) {
                    throw new RuntimeException("No version defined for '${imageString}")
                }
                imageString = "${imageString}:${version}"
            }
        }
        return imageString
    }

    private static Map<String, String> versionMap() {
        String resource = 'docker/imageVersions.properties'
        InputStream stream = Images.classLoader.getResourceAsStream(resource)
        if (stream == null) {
            throw new RuntimeException("Luci: Internal error: Could not read image versions from ${resource}")
        }
        Properties answer = new Properties()
        answer.load(stream)
        return answer
    }

}
