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

    private String ensureVersion(String imageString) {
        String prefix = 'luci/'
        versionMap() != null
        if (imageString.startsWith(prefix)) {
            if (imageString.indexOf(':') == -1) {
                String version = versionMap()[imageString.substring(prefix.length())]
                if (version == null) {
                    throw new RuntimeException("No version defined for '${imageString}")
                }
                imageString = "${imageString}:${version}"
            }
        }
        return imageString
    }

    @Memoized
    private Map<String, String> versionMap() {
        InputStream stream = Thread.currentThread().contextClassLoader.getResourceAsStream('docker/imageVersions.properties')
        assert stream != null
        Properties answer = new Properties()
        answer.load(stream)
        return answer
    }
}
