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

    /** Log the command executed */
    boolean logCommand = false

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

    /**
     * Construct the command line for docker-machine to crate the machine
     * @param machineName Name of machine
     * @param hideSensitiveData If true sensitive data like password is hidden
     * @return
     */
    @VisibleForTesting
    List<String> commandLine(String machineName) {
        List<String> cmd = ['docker-machine']
        if (debug) cmd << '--debug'
        cmd << 'create'
        Map<String, String> optionMap = buildCompleteOptionsMap()
        cmd.addAll(optionMap.collect { key, value -> ["--${key}".toString(), value] }.flatten())
        cmd.addAll(createArgs)
        cmd = bindPlaceholders(cmd, machineName)
        return cmd
    }

    DockerHost getOrCreate(String machineName) {
        StringBuffer err = "" << ""
        println "Creating machine: '${machineName}'"
        ExternalCommand ec = new ExternalCommand()
        ec.sensitiveData = sensitiveData()

        int rc = ec.execute(*commandLine(machineName), err: err, log: logCommand)
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

    Collection<String> sensitiveData(Map<String, String> optionMap = null) {
        if (optionMap == null) optionMap = buildCompleteOptionsMap()
        List<String> list = optionMap.keySet().findAll { String key ->
            // Assume that data is sensitive if key contains the string 'password'
            key.contains('password')
        }.collect { String key -> optionMap[key] }
        return bindPlaceholders(list, '#name#')
    }

    private List<String> bindPlaceholders(List<String> list, String machineName) {
        Map<String, String> binds = [ name: machineName ]
        binds.putAll(bindings)
        TemplateEngine engine = new SimpleTemplateEngine()
        def x = { String input -> engine.createTemplate(input).make(binds).toString() }

        return list.collect { x(it) }
    }
}
