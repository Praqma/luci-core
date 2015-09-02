package net.praqma.luci.model

import groovy.transform.CompileStatic
import net.praqma.luci.docker.Containers

@CompileStatic
class ArtifactoryModel extends BaseServiceModel implements WebfrontendService {


    @Override
    void addToComposeMap(Map map, Containers containers) {
        super.addToComposeMap(map, containers)
    }
}
