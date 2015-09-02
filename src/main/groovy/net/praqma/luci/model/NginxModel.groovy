package net.praqma.luci.model

import groovy.transform.Immutable
import net.praqma.luci.docker.Containers

class NginxModel extends BaseServiceModel {

    private Collection<User> users = []

    def user(String name, String password) {
        users << new User(name, password)
    }

    @Override
    void addToComposeMap(Map map, Containers containers) {
        super.addToComposeMap(map, containers)
        map.ports = ["${box.port}:80" as String]
        map.links = box.services.findAll { it.includeInWebfrontend }.collect { BaseServiceModel service ->
            "${service.serviceName}:${service.serviceName}" as String
        }
        def services = box.services.findAll { it.includeInWebfrontend }*.serviceName
        map.command = ['-s', services.join(' '), '-n', box.name, '-p', box.port as String]
    }

}

@Immutable
class User {
    String name
    String password
}