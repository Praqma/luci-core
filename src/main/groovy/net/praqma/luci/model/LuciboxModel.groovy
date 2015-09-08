package net.praqma.luci.model

import com.google.common.io.Files
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovyx.gpars.GParsPool
import groovyx.gpars.dataflow.DataflowQueue
import net.praqma.luci.docker.ContainerInfo
import net.praqma.luci.docker.ContainerKind
import net.praqma.luci.docker.Containers
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.utils.ExternalCommand
import org.yaml.snakeyaml.Yaml

import static groovyx.gpars.dataflow.Dataflow.task

@CompileStatic
class LuciboxModel {

    private File workDir

    final String name

    /** Indicate if services should use a data container to store data, or if data should be stored in
     * the container itself.
     *
     * The value can be overridden for specific containers
     */
    boolean useDataContainer = true

    /** Port on web frontend (nginx) */
    int port = 80

    /**
     * Mapping luciname of service to the model class
     */
    private Map<String, BaseServiceModel> serviceMap = [:]

    DockerHost dockerHost

    Integer socatForTlsHackPort = null

    /** All hosts where this lucibox is having containers */
    private Collection<DockerHost> allHosts = ([] as Set).asSynchronized()

    LuciboxModel(String name) {
        this.name = name
        service(ServiceEnum.WEBFRONTEND.name)
    }

    void addHost(DockerHost host) {
        if (host != null) allHosts << host
        assert allHosts.every { it != null }
    }

    BaseServiceModel getService(ServiceEnum service) {
        return getService(service.name)
    }

    BaseServiceModel getService(String luciName) {
        return serviceMap[luciName]
    }

    Collection<BaseServiceModel> getServices() {
        return serviceMap.values()
    }

    Collection<AuxServiceModel> getAuxServices() {
        return services.findAll { it instanceof AuxServiceModel } as Collection<AuxServiceModel>
    }

    void addService(BaseServiceModel service) {
        assert serviceMap[service.serviceName] == null
        serviceMap[service.serviceName] = service
    }

    @CompileDynamic
    void service(String serviceName, Closure closure) {
        ServiceEnum e = ServiceEnum.valueOf(serviceName.toUpperCase())
        BaseServiceModel model = e.modelClass.newInstance()
        model.serviceName = serviceName
        model.box = this
        model.dockerImage = e.dockerImage.imageString
        ServiceEnum old = serviceMap.put(e.name, model)
        if (old != null) {
            throw new RuntimeException("Double declaration of service '${serviceName}'")
        }
        def m = this // Don't understand this?!? Works when this is assigned to m, if using 'this' directly it doesn't
        m.metaClass[serviceName] = { Closure c ->
            model.with c
        }
        m.metaClass['get' + serviceName.capitalize()] = { -> model }
        model.with closure
    }

    void service(String... serviceNames) {
        serviceNames.each { String name ->
            service(name) {}
        }
    }

    @CompileDynamic
    private Map buildYamlMap(Containers containers) {
        Map m = [:]
        serviceMap.each { String s, BaseServiceModel model ->
            if (!(model instanceof AuxServiceModel)) {
                m[s] = model.buildComposeMap(containers)
            }
        }
        if (socatForTlsHackPort && dockerHost.tls) {
            m['dockerHttp'] = [
                    image  : 'sequenceiq/socat',
                    ports  : ["${socatForTlsHackPort}:2375" as String],
                    volumes: ['/var/run/docker.sock:/var/run/docker.sock']
            ]
        }
        return m
    }

    void generateDockerComposeYaml(Containers containers, Writer out) {
        Map map = buildYamlMap(containers)
        new Yaml().dump(map, out)
    }

    /**
     * Should be call when LuciboxModel is constructed,
     * but before it is used
     * <p>
     * This method should not make any call to docker host, or even assume
     * there is valid docker hosts configured
     */
    void initialize(File workDir = null) {
        println "Initilizing Lucibox: '${name}'"
        this.workDir = workDir ?: Files.createTempDir()
        this.workDir.mkdirs()
        if (dockerHost != null) allHosts << dockerHost
        assert allHosts.every { it != null }
        serviceMap.values().each { it.prepare() }
    }

    @CompileDynamic
    void initializeHosts() {
        assert allHosts.every { it != null }
        def hosts = allHosts
        // TODO if allHosts not assigned to local variable allHosts contains an extra null value
        // in withPool block?!? I believe it happens when the lucibox has no defined docker host
        GParsPool.withPool(10) {
            hosts.eachParallel { DockerHost h ->
                assert h != null
                h.initialize()
            }
        }
        assert allHosts.every { it != null }
    }

    private File getDockerComposeFile() {
        return new File(workDir, 'docker-compose.yml')
    }

    /**
     * Called before starting the box.
     * <p>
     * This should not start any containers, but can create (data) containers
     *
     * @return
     */
    Containers preStart() {
        if (dockerHost == null) {
            throw new RuntimeException("No docker host defined for '${name}'")
        }
        Containers containers = new Containers(this)

        serviceMap.values().each { it.preStart(this, containers) }

        new FileWriter(dockerComposeFile).withWriter { Writer w ->
            generateDockerComposeYaml(containers, w)
        }
        return containers
    }

    /**
     * @return Containers belonging to this Lucibox
     */
    Map<String, ContainerInfo> containers(ContainerKind... kinds) {
        DataflowQueue queue = new DataflowQueue<>()
        Map<String, ContainerInfo> answer = [:].asSynchronized()
        allHosts.each { DockerHost host ->
            assert host.isInitialized
            task {
                return host.containers(this, kinds)
            }.whenBound { queue << it }
        }
        allHosts.size().times {
            def val = queue.val
            if (val instanceof Throwable) {
                throw val
            } else {
                answer.putAll(val as Map)
            }
        }
        return answer
    }

    /**
     * Bring up this Lucibox.
     */
    @CompileDynamic
    void bringUp() {
        // Take down any containers that should happend to run, before bringing it up
        // Side effect: It initializes the hosts
        takeDown()

        Containers containers = preStart()

        auxServices.each { AuxServiceModel aux ->
            println "Setting up aux service: ${aux.serviceName} on ${aux.dockerHost}"
            aux.startService(containers)
        }

        new ExternalCommand(dockerHost).execute('docker-compose', '-f', dockerComposeFile.path,
                'up', '-d')

        println "Lucibox '${name} will use docker hosts: ${allHosts*.asString()}"
        println ""
        println "Lucibox '${name}' running at http://${dockerHost.host}:${port}"
    }

    /**
     * Take down the Lucibox.
     * <p>
     * That is stop and remove all service containers.
     */
    void takeDown() {
        initializeHosts()
        removeContainers(ContainerKind.SERVICE)
    }

    /**
     * Destroy the Lucibox.
     * <p>
     * Stop and remove all containers (including data containers) related to this Lucibox.
     */
    void destroy() {
        initializeHosts()
        removeContainers()
    }

    private void removeContainers(ContainerKind... kinds) {
        Collection<ContainerInfo> containers = containers(kinds).values()
        GParsPool.withPool {
            containers.each { ContainerInfo ci ->
                ci.host.removeContainers([ci.id])
            }
        }
    }

    @CompileDynamic
    void printInformation() {
        String header = "Lucibox: ${name}"
        println "\n${header}\n${'=' * header.length()}"
        println "Primary host: ${dockerHost.asString()}"

        println "Aux services:"
        auxServices.each { AuxServiceModel aux ->
            println "\t${aux.serviceName} @ ${aux.dockerHost.asString()}"
        }
    }

    @Memoized
    Collection<DockerHost> getAllHosts() {
        return services*.dockerHost as Set
    }
}

