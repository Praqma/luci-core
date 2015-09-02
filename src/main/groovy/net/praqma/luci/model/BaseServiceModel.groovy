package net.praqma.luci.model

import groovy.transform.CompileStatic
import net.praqma.luci.docker.ContainerInfo
import net.praqma.luci.docker.ContainerKind
import net.praqma.luci.docker.Containers
import net.praqma.luci.docker.DockerHost

@CompileStatic
abstract class BaseServiceModel {

    String dockerImage

    String serviceName

    LuciboxModel box

    DockerHost dockerHost

    /**
     * Indicate if the data for the service should be stored in a data container.
     * If the value is <code>null</code> the value defined in the lucibox is used.
     */
    Boolean useDataContainer

    boolean includeInWebfrontend = false

    Map buildComposeMap(Containers containers) {
        List<String> volumes_from = []
        if (useDataContainer == null ? box.useDataContainer : useDataContainer) {
            volumes_from << containers.storage(dockerHost).name
        }
        Map answer = [
                image         : dockerImage,
                extra_hosts   : [lucibox: box.dockerHost.host],
                container_name: containerName,
                volumes_from  : volumes_from,
                labels        : [(ContainerInfo.BOX_NAME_LABEL)          : box.name,
                                 (ContainerInfo.CONTAINER_KIND_LABEL)    : ContainerKind.SERVICE.name(),
                                 (ContainerInfo.CONTAINER_LUCINAME_LABEL): serviceName]
        ]
        addToComposeMap(answer, containers)
        return answer
    }

    void addToComposeMap(Map map, Containers containers) {

    }

    /**
     * Hook method called for all models before attempting to start it.
     * <p>
     * This method should not assume there is valid docker hosts
     * @param context
     */
    void prepare() {
        box.addHost(dockerHost)
    }

    /**
     * This method is called before the lucibox is started.
     * <p>
     * All hosts are initialized (i.e. you can make calls to docker host)
     *
     * @param context
     */
    void preStart(LuciboxModel box, Containers containers) {

    }

    DockerHost getDockerHost() {
        return this.@dockerHost ?: box.dockerHost
    }

    String getContainerName() {
        return "${box.name}_${serviceName}"
    }
}
