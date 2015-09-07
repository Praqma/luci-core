package net.praqma.luci.test

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.docker.DockerHostImpl
import net.praqma.luci.utils.LuciSettings


@CompileStatic
class TestDockerHosts {

    @Memoized
    static DockerHost getPrimary() {
        String machineName = LuciSettings.instance['testDockerMachine']
        DockerHost host
        if (machineName == null) {
            throw new RuntimeException("No docker host for test defined. You should define it in Luci settings.")
        } else {
            host = DockerHostImpl.fromDockerMachine(machineName)
        }
        host.initialize()
        return host
    }
}
