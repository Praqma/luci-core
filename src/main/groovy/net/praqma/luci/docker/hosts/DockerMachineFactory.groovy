package net.praqma.luci.docker.hosts

import com.google.common.annotations.VisibleForTesting
import groovy.text.SimpleTemplateEngine
import groovy.text.TemplateEngine
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.utils.ExternalCommand
import net.praqma.luci.utils.LuciSettings

/**
 * Specify a factory to create docker machines.
 */
class DockerMachineFactory {

    final String name

    /**
     * Arguments for the 'docker-machine create' command
     */
    List<String> createArgs = ['$name']

    Map<String, String> options = [:]

    String driver

    /** Execute docker-machine create with debug flag? */
    boolean debug = false

    Map<String, String> bindings = [:]

    DockerMachineFactory(String name) {
        this.name = name
    }

    void options(Map<String, String> opts) {
        options.putAll(opts)
    }

    void driver(String driver) {
        this.driver = driver
    }

    @VisibleForTesting
    List<String> commandLine(String machineName) {
        Map<String, String> binds = [ name: machineName ]
        binds.putAll(bindings)
        TemplateEngine engine = new SimpleTemplateEngine()
        def x = { String input -> engine.createTemplate(input).make(binds).toString() }

        List<String> cmd = ['docker-machine']
        if (debug) cmd << '--debug'
        cmd << 'create'
        cmd.addAll(buildCompleteOptionsMap().collect { key, value -> ["--${key}".toString(), x(value)] }.flatten())
        createArgs.each { String arg ->
            assert arg != null
            cmd << x(arg)
        }
        return cmd
    }

    DockerHost getOrCreate(String machineName) {
        StringBuffer err = "" << ""
        println "Creating machine: '${machineName}'"
        int rc = new ExternalCommand().execute(commandLine(machineName, LuciSettings.instance), err: err)
        if (rc == 0) {
            return new DockerMachineHost(machineName)
        } else {
            // TODO check if exist
            throw new RuntimeException(err.toString())
        }
    }

    private Map<String, String> buildCompleteOptionsMap() {
        Map<String, String> answer = [:]
        if (driver) answer['driver'] = driver
        answer.putAll(options)
        return answer
    }

}
