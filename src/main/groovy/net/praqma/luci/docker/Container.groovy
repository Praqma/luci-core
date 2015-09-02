package net.praqma.luci.docker

import com.google.common.io.ByteStreams
import com.google.common.io.Files
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.praqma.luci.model.LuciboxModel
import net.praqma.luci.utils.ExternalCommand

/**
 * Class to represent containers.
 * <p>
 * Only services containers are created with docker-compose
 */
@CompileStatic
class Container {

    private Images dockerImage
    final String luciName
    private ContainerKind kind
    private LuciboxModel box
    private Set<String> volumes = [] as Set
    private ExternalCommand ec

    Container(Images dockerImage, LuciboxModel box, DockerHost host, ContainerKind kind, String name) {
        this.dockerImage = dockerImage
        this.box = box
        this.luciName = name
        this.kind = kind
        this.ec = new ExternalCommand(host)
    }

    String getName() {
        return "${box.name}__${luciName}"
    }

    Volume volume(String v) {
        volumes << v
        return new Volume(v)
    }

    @CompileDynamic
    void create() {
        List<String> v = volumes.collect { ['-v', it] }.flatten()
        StringBuffer out = "" << ""
        int rc = ec.execute('docker', 'create', *v, '--name', name,
                '-l', "${ContainerInfo.CONTAINER_KIND_LABEL}=${kind.name()}" as String,
                '-l', "${ContainerInfo.BOX_NAME_LABEL}=${box.name}" as String,
                '-l', "${ContainerInfo.CONTAINER_LUCINAME_LABEL}=${luciName}" as String,
                dockerImage.imageString, err: out)
        if (rc != 0) {
            if (out.toString().indexOf('is already in use by container') > 0) {
                println "Using existing container '${name}' on ${ec.dockerHost}"
            } else {
                println out.toString()
                throw new RuntimeException("Failed to create container: '${name}'")
            }
        }
    }

    /**
     * Remove the underlying docker container if exists
     */
    void remove() {
        ec.execute('docker', 'rm', '-fv', name)
    }

    class Volume {
        private String path

        Volume(String path) {
            this.path = path
        }

        VolumeFile file(String filePath) {
            return new VolumeFile(this, filePath)
        }
    }

    String getVolumesFromArg() {
        return "--volumes-from=${name}"
    }

    class VolumeFile {

        private Volume volume
        private String filePath

        VolumeFile(Volume volume, String filePath) {
            assert volume != null && filePath != null
            this.volume = volume
            this.filePath = filePath
        }

        void addStream(InputStream stream) {
            assert stream != null
            File f = new File(filePath)
            // We need to create a file with the name of the file in the container (that is the way the cp command works)
            File tempDir = Files.createTempDir()
            File tempFile = new File(tempDir, f.name)
            ByteStreams.copy(stream, new FileOutputStream(tempFile))

            String completePath = f.parent ? new File(new File(volume.path), f.parent).absolutePath : volume.path
            ec.execute('docker', 'cp', tempFile.absolutePath, "${name}:${completePath}" as String)
            tempFile.delete()
            tempDir.delete()
        }

        void addResource(String resource) {
            InputStream stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resource)
            if (stream == null) {
                throw new IllegalArgumentException("Unable to find resouces '${resource}'")
            }
            addStream(stream)
        }

    }
}
