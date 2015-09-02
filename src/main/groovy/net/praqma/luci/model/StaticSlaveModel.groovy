package net.praqma.luci.model

import groovy.transform.CompileStatic
import net.praqma.luci.docker.Containers

@CompileStatic
class StaticSlaveModel extends BaseServiceModel implements AuxServiceModel {

    String dockerImage

    String slaveName

    List<String> labels = []

    int executors = 2

    void dockerImage(String image) {
        this.dockerImage = image
    }

    void labels(String... names) {
        this.labels.addAll(names)
    }

    @Override
    void addToComposeMap(Map map, Containers containers) {
        assert box != null
        super.addToComposeMap(map, containers)
        map.image = dockerImage
        map.links = ["${ServiceEnum.WEBFRONTEND.name}:nginx" as String, "${ServiceEnum.JENKINS.name}:master" as String]
        map.command = ['sh', '/luci/data/jenkinsSlave/slaveConnect.sh', slaveName]
        map.volumes_from = [containers.jenkinsSlave(dockerHost).name]
    }

}
