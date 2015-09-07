package net.praqma.luci.model

import net.praqma.luci.docker.DockerHost
import net.praqma.luci.test.TestDockerHosts
import org.junit.Test

class LuciboxModelTest {

    LuciboxModel createBox() {
        DockerHost host = TestDockerHosts.primary
        assert host != null

        LuciboxModel box = new LuciboxModel('lucitest')
        box.dockerHost = host
        box.service('jenkins') {

        }
        box.service('artifactory') {

        }
        box.initialize()
        return box
    }

    @Test
    void testPrintInformation() {
        LuciboxModel box = createBox()
        box.printInformation()
    }

    @Test
    void testTakeDown() {
        LuciboxModel box = createBox()
        // Take down without starting it
        box.takeDown()

        // Start it and take down
        box.bringUp()
        box.takeDown()

        // Cleanup
        box.destroy()
    }

    @Test
    void testDestroy() {
        LuciboxModel box = createBox()
        // Destroy without starting it
        box.destroy()

        // Start it and destroy
        box.bringUp()
        box.destroy()
    }

}
