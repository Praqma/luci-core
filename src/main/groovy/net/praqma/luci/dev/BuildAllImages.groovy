package net.praqma.luci.dev

import groovyx.gpars.GParsPool
import groovyx.gpars.dataflow.Dataflows
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.docker.DockerHostImpl

/**
 * Build all docker images for Luci
 */
class BuildAllImages {

    private File dockerImagesDir

    BuildAllImages(File dir) {
        assert dir != null
        this.dockerImagesDir = dir
    }

    boolean build(Collection<DockerHost> hosts) {
        hosts.each {
            println "***\n*** Building on ${it}\n***\n"
            build(it)
        }
    }

    boolean build(DockerHost dockerHost = null, boolean doPush = false) {
        if (dockerHost == null) {
            dockerHost = DockerHostImpl.getDefault()
        }
        File versionsFile = new File(dockerImagesDir, 'imageVersions.properties')
        assert versionsFile.exists()

        // Directory containing directory for each image to build
        println "Build images in directory: ${dockerImagesDir}"

        Properties props = new Properties()
        versionsFile.withInputStream {
            props.load(it)
        }

        Collection<DockerImage> images = props.collect { String key, String version ->
            File ctxDir = new File(dockerImagesDir, key)
            assert ctxDir.exists()
            new DockerImage(ctxDir, version)
        }
        Collection<String> imageNames = images*.fullImageName

        // Mapping (full) image name to the exit code for the build of that imag
        // The Dataflows are key by the full name of the image
        Dataflows buildResults = new Dataflows()
        Collection<Integer> rcs = GParsPool.withPool(20) {
            // Collect images to the exit code for building it
            images.collectParallel { DockerImage image ->
                String baseImage = image.baseImage
                int rc = 1 // set non-zero to indicate error until we have successful build
                if (baseImage.startsWith('luci/')) {
                    if (!imageNames.contains(baseImage)) {
                        println "WARNING: '${image.name}' has base '${baseImage}' which is not part of build. Did you forget to update version?"
                    }
                } else {
                    baseImage = 'none'
                }
                try {
                    println "${image.fullImageName} waiting for ${baseImage}"
                    if (baseImage == 'none' || buildResults[baseImage] == 0) { // Will block until build result is ready
                        println "Building image ${image.fullImageName} with base ${baseImage}"
                        rc = image.build(dockerHost)
                    } else {
                        println "Skipping ${image.fullImageName}. Base image (${baseImage}) is not built"
                    }
                } finally {
                    // Set result for this build, triggering dependent builds
                    buildResults[image.fullImageName] = rc
                    println "Finish '${image.fullImageName}' with rc: ${rc}"
                }

                if (doPush && rc == 0) {
                    rc = image.push(dockerHost)
                }
                rc
            }
        }

        boolean answer = rcs.every { it == 0 }
        println "DONE. Built all images"
        return answer
    }

    static void main(String[] args) {
        assert args.size() == 1 || args.size() == 2

        File dir = new File(args[0])
        boolean doPush = args.size() > 1 ? Boolean.parseBoolean(args[1]) : false
        DockerHost host = DockerHostImpl.default
        boolean success = new BuildAllImages(new File(args[0], 'docker')).build(host, doPush)

        if (!success) {
            throw new RuntimeException("Error building images")
        }
    }

}
