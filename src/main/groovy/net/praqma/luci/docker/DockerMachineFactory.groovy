package net.praqma.luci.docker

import groovy.text.SimpleTemplateEngine
import groovy.text.TemplateEngine
import net.praqma.luci.docker.net.praqma.luci.docker.hosts.DockerMachineHost
import net.praqma.luci.utils.ExternalCommand

/**
 * Specify a factory to create docker machines.
 */
class DockerMachineFactory {

    final String name

    /**
     * Arguments for the 'docker-machine create' command
     */
    List<String> createArgs = []

    DockerMachineFactory(String name) {
        this.name = name
    }

    DockerHost getOrCreate(String machineName) {
        List cmd = ['docker-machine', 'create']
        Map<String, String> bindings = [ name: machineName]
        TemplateEngine engine = new SimpleTemplateEngine()
        createArgs.each { String arg ->
            String expanded = engine.createTemplate(arg).make(bindings).toString()
            cmd << expanded
        }
        StringBuffer err = "" << ""
        println "Attempting to create machine: '${machineName}'"
        int rc = new ExternalCommand().execute(*cmd, err: err)
        if (rc == 0) {
            return new DockerMachineHost(machineName)
        } else {
            // TODO check if exist
            throw new RuntimeException(err.toString())
        }
    }

}
