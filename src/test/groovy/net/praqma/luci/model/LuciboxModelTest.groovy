package net.praqma.luci.model

import com.google.common.io.Files
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.test.TestDockerHosts
import org.junit.Test

class LuciboxModelTest {

    @Test
    void test() {
        DockerHost host = TestDockerHosts.primary
        assert host != null

        LuciboxModel box = new LuciboxModel('lucitest')
        box.dockerHost = host
        box.service('jenkins') {

        }
        box.service('artifactory') {

        }
        box.initialize()

        box.printInformation()
        box.takeDown()
        box.bringUp(Files.createTempDir())
        box.destroy()
    }
}
