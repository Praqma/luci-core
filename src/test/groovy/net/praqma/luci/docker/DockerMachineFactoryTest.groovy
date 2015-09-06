package net.praqma.luci.docker

import net.praqma.luci.docker.hosts.DockerMachineFactory
import org.junit.Test


class DockerMachineFactoryTest {

    @Test
    void testDebugFlag() {
        DockerMachineFactory f = new DockerMachineFactory()
        assert f.commandLine('test')[0..1] == ['docker-machine', 'create']
        f.debug = true
        assert f.commandLine('test')[0..2] == ['docker-machine', '--debug', 'create']
    }

    @Test
    void testNameTemplating() {
        DockerMachineFactory f = new DockerMachineFactory()
        assert f.commandLine('dummy') == ['docker-machine', 'create', 'dummy']
    }

    @Test
    void testDriver() {
        DockerMachineFactory f = new DockerMachineFactory()
        f.driver = "foo"
        f.options 'opt-key': 'opt-value'
        f.createArgs << 'bar'
        assert f.commandLine('dummy') == ['docker-machine', 'create', '--driver', 'foo', '--opt-key', 'opt-value', 'dummy', 'bar']
    }

    @Test
    void testBindings() {
        DockerMachineFactory f = new DockerMachineFactory()
        // Create a binding to a map
        f.bindings.settings = ['zetta.username': 'abc']
        // X uses the settings map to lookup value
        f.options x: '${settings["zetta.username"]}'
        assert f.commandLine('mach') == ['docker-machine', 'create', '--x', 'abc', 'mach']
    }

    @Test
    void testLookupFunction() {
        DockerMachineFactory f = new DockerMachineFactory()
        // Create a binding to a function
        f.bindings.lookup = { key, defaultValue = null ->
            [ one: '1', two: '2'][key] ?: defaultValue ?: { throw new RuntimeException("${key} is not defined")}()
        }
        f.options x: '${lookup("one")}'
        assert f.commandLine('mach') == ['docker-machine', 'create', '--x', '1', 'mach']
    }



}
