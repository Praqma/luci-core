package net.praqma.luci.docker

import net.praqma.luci.model.LuciboxModel
import net.praqma.luci.test.TestDockerHosts
import org.junit.Test


class ContainersTest {

    @Test
    void testSshKeys() {
        DockerHost host = TestDockerHosts.primary

        LuciboxModel box = new LuciboxModel('lucitest')
        Containers c = new Containers(box)
        Container con = c.sshKeys(host)
        // TODO add asserts
    }
}
