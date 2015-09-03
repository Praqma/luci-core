package net.praqma.luci.model

import groovy.transform.CompileStatic
import net.praqma.luci.docker.Images

@CompileStatic
enum ServiceEnum {

    WEBFRONTEND(NginxModel, Images.SERVICE_NGINX),
    JENKINS(JenkinsModel, Images.SERVICE_JENKINS),
    ARTIFACTORY(ArtifactoryModel, Images.SERVICE_ARTIFACTORY)

    final Class<?> modelClass

    final Images dockerImage

    String getName() {
        return name().toLowerCase()
    }

    ServiceEnum(Class<?> modelClass, Images image) {
        this.modelClass = modelClass
        this.dockerImage = image
    }

}
