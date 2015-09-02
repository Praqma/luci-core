package net.praqma.luci.dev

import groovy.transform.Memoized
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.utils.ExternalCommand

/**
 * Helper class to build a single docker image
 */
class DockerImage {

    private File contextDir

    private String version

    DockerImage(File contextDir, String version) {
        this.contextDir = contextDir
        this.version = version
    }

    File getDockerFile() {
        return new File(contextDir, 'Dockerfile')
    }

    String getName() {
        return contextDir.name
    }

    String getFullImageName() {
        return "luci/${name}:${version}"
    }

    @Memoized
    String getBaseImage() {
        return dockerFile.withInputStream { InputStream stream ->
            String fromLine = stream.readLines().find { String line -> line.startsWith("FROM ") }
            if (fromLine == null) {
                throw new RuntimeException("no FROM instruction found in Dockerfile ${dockerFile.path}")
            }
            fromLine.split(' ')[1].trim()
        }
    }

    int build(DockerHost host) {
        Closure c = { InputStream stream ->
            stream.eachLine { String line ->
                println "${name}:\t${line}"
            }
        }
        ExternalCommand ec = new ExternalCommand(host)
        StringBuffer err = "" << ""
        int rc = ec.execute('docker', 'build', '-t', fullImageName, dockerFile.parent, err: err, out: c)
        if (rc != 0) {
            println "ERROR in ${name}"
            println "Error: " + err.toString()
        }
        return rc
    }

    int push(DockerHost host) {
        println "Pushing: ${fullImageName}"
        ExternalCommand ec = new ExternalCommand(host)
        StringBuffer err = "" << ""
        int rc = ec.execute('docker', 'push', fullImageName, err: err)
        if (rc != 0) {
            println "ERROR in ${name}"
            println "Error: " + err.toString()
        }
        return rc
    }

    String toString() {
        return fullImageName
    }
}
