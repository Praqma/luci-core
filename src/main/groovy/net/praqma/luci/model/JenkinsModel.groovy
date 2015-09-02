package net.praqma.luci.model

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.praqma.luci.docker.Containers
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.utils.ClasspathResources
import net.praqma.luci.utils.ExternalCommand

@CompileStatic
class JenkinsModel extends BaseServiceModel implements WebfrontendService {

    int slaveAgentPort = -1 // -1 => Let LUCI assign port

    /** Number of executors for master */
    int executors = 0

    private Map<String, StaticSlaveModel> staticSlaves = [:]

    private Map<String, OnDemandSlaveModel> onDemandSlaves = [:]

    private Collection<File> initFiles = []

    @Lazy
    JenkinsSeedJob seedJob = new JenkinsSeedJob()

    /**
     * Map plugin key to version
     */
    private Map<String, String> pluginMap = [:]

    private List<Closure> actions = []

    void plugins(Map<String, String> map) {
        pluginMap.putAll(map)
    }

    /**
     * @return The port for the docker cloud
     */
    private int cloudPort() {
        if (box.socatForTlsHackPort == null) {
            return box.dockerHost.port
        } else {
            box.socatForTlsHackPort
        }
    }

    Collection<File> getInitFiles() {
        return this.initFiles
    }

    void initFiles(File... files) {
        initFiles(files.toList())
    }

    @CompileDynamic
    void initFiles(Iterable<File> files) {
        initFiles.addAll(files as List)
    }

    void addPreStartAction(Closure c) {
        actions << c
    }

    @Override
    @CompileDynamic
    void addToComposeMap(Map map, Containers containers) {
        super.addToComposeMap(map, containers)

        DockerHost h = box.dockerHost
        String url = "http://${h.host}:${h.port}"
        map.command = ['-d', containers.jenkinsSlave(dockerHost).name,
                       '-c', "http://${h.host}:${cloudPort()}" as String,
                       '-j', "http://${h.host}:${box.port}/jenkins" as String,
                       '-e', 'luci@praqma.net',
                       '-x', executors as String,
                       '-a', slaveAgentPort as String]
        if (staticSlaves.size() > 0) {
            // A slave is represented as <name>:<executors>:label1:label2:...
            Collection<String> args = staticSlaves.values().collect { StaticSlaveModel m ->
                String labelString = m.labels.collect { ":${it}" }.join()
                "${m.slaveName}:${m.executors}" + labelString
            }
            map.command << '-s' << args.join(' ')
        }
        if (onDemandSlaves.size() > 0) {
            // A slave is represented as <image>@<name>
            Collection<String> args = onDemandSlaves.values().collect { OnDemandSlaveModel m ->
                "${m.dockerImage}@${m.slaveName}"
            }
            map.command << '-t' << args.join(' ')
        }
        if (pluginMap.size() > 0) {
            map.command << '-p' << pluginMap.collect { key, version -> "${key}:${version}" }.join(' ')
        }
        map.command << '--' << '--prefix=/jenkins'
        map.ports = ["${slaveAgentPort}:${slaveAgentPort}" as String] // for slave connections
        //map.ports << '10080:8080' // Enter container without nginx, for debug
        map.volumes_from <<
                containers.sshKeys(dockerHost).name <<
                containers.java8mixin(dockerHost).name <<
                containers.jenkinsConfig(this).name
    }

    void staticSlave(String slaveName, Closure closure) {
        StaticSlaveModel slave = new StaticSlaveModel()
        slave.slaveName = slaveName
        slave.with closure
        slave.serviceName = "${ServiceEnum.JENKINS.name}${slaveName.capitalize()}"
        slave.box = box
        staticSlaves[slaveName] = slave
        box.addService(slave)
    }

    void onDemandSlave(String slaveName, Closure closure) {
        OnDemandSlaveModel slave = new OnDemandSlaveModel()
        slave.slaveName = slaveName
        slave.with closure
        slave.box = box
        onDemandSlaves[slaveName] = slave
    }

    /**
     * Execute a cli command against jenkins
     */
    @CompileDynamic
    void cli(List<String> cmd, Closure input) {
        new ExternalCommand(dockerHost).execute(["docker", "exec", "${box.name}_${ServiceEnum.JENKINS.name}", *cmd], null, input)
    }

    @Override
    @CompileDynamic
    void prepare() {
        super.prepare()
        staticSlaves.values()*.prepare()
        actions.each { it() }
        initFiles(new ClasspathResources(JenkinsModel.classLoader).resourceAsFile('scripts/luci-init.groovy') as File)
    }

    @Override
    void preStart(LuciboxModel box, Containers containers) {
        super.preStart(box, containers)
        if (slaveAgentPort == -1) {
            slaveAgentPort = assignSlaveAgentPort()
        }
    }

    // Map of slave agent ports assigned to a lucibox
    private static Map<String, Integer> assingedPorts = [:]

    private int assignSlaveAgentPort() {
        if (assingedPorts[box.name] != null) return assingedPorts[box.name]
        Set<Integer> ports = assingedPorts.values() as Set
        ports.addAll(dockerHost.boundPorts())
        Integer port = (50000..50099).find { !ports.contains(it) }
        if (port == null) {
            throw new RuntimeException("No available slave agent port")
        }
        assingedPorts[box.name] = port
        return port
    }
}

class JenkinsSeedJob {
    String name
    File jobDslFile
}
