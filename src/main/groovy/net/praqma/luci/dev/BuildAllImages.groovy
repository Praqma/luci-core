package net.praqma.luci.dev

import groovyx.gpars.GParsPool
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Dataflows
import groovyx.gpars.dataflow.Promise
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.docker.DockerHostImpl
import net.praqma.luci.utils.ClasspathResources

/**
 * Build all docker images for Luci
 */
class BuildAllImages {

    /**
     * Directory
     */
    private File dockerImagesDir

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
        File versionsFile
        if (System.properties['net.praqma.luci.projectRoot'] != null) {
            versionsFile = new File(System.properties['net.praqma.luci.projectRoot'], 'buildSrc/src/main/resources/docker/imageVersions.properties')
        } else {
            versionsFile = new ClasspathResources().resourceAsFile('docker/imageVersions.properties')
        }

        assert versionsFile.exists()

        // Directory containing directory for each image to build
        File dockerDir = versionsFile.parentFile
        println "Build images in directory: ${dockerDir}"

        Properties props = new Properties()
        versionsFile.withInputStream {
            props.load(it)
        }

        Collection<DockerImage> images = props.collect { String key, String version ->
            File ctxDir = new File(dockerDir, key)
            assert ctxDir.exists()
            new DockerImage(ctxDir, version)
        }
        Collection<String> imageNames = images*.fullImageName

        // Mapping (full) image name to the exit code for the build of that imag
        // The Dataflows are key by the full name of the image
        Dataflows buildResults = new Dataflows()
        // 'none' is speciel for no luci base image.
        // Set build result for 'none' to 0, so build begins for images that doesn't depend on luci images
        buildResults['none'] = 0

        Collection<Integer> rcs = GParsPool.withPool(images.size()) {
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
                    if (buildResults[baseImage] == 0) { // Will block until build result is ready
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

}
