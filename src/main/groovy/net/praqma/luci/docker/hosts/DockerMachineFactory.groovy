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

    /**
     * And option 'foo' with value 'bar' will add '--foo bar' to the docker-machine command executed
     */
    Map<String, String> options = [:]

    /** Execute docker-machine create with debug flag? */
    boolean debug = false

    /** Log the command executed */
    boolean logCommand = false

    /**
     * Bindings used when option values are expanded
     */
    Map<String, String> bindings = [:]

    /**
     * Mapping a property to an option.
     */
    private Map<String, String> prop2opts = [:]


    DockerMachineFactory(String name) {
        this.name = name
        addProperty 'driver', 'driver'
    }

    void options(Map<String, String> opts) {
        options.putAll(opts)
    }

    /**
     * Add a property 'prop' that will define the option 'opt'
     * <p>
     * In other words a getter and setter for 'prop' is added to this
     * objecgt that will store the value in the 'options' map under the
     * key 'opt'
     *
     * @param prop
     * @param opt
     */
    private void addProperty(String prop, String opt) {
        DockerMachineFactory me = this
        me.metaClass["set${prop.capitalize()}"] = { String value ->
            options[opt] = value
        }
        me.metaClass["get${prop.capitalize()}"] = { ->
            return options[opt]
        }
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
