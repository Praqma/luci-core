package net.praqma.luci.docker

import net.praqma.luci.model.LuciboxModel
import net.praqma.luci.test.TestDockerHosts
import net.praqma.luci.utils.ClasspathResources
import net.praqma.luci.utils.ExternalCommand
import org.junit.BeforeClass
import org.junit.Test


class ContainersTest {

    @BeforeClass
    static void enhanceContainer() {
        // Enhance container class

        /**
         * Execute the command with the container. Fail if exit code is non-zero
         */
        Container.metaClass.verify = { String[] cmd ->
            Container self = delegate
            StringBuffer out = "" << ""
            int rc = new ExternalCommand(self.host).execute('docker', 'run', '--rm', self.volumesFromArg, Images.TOOLS.imageString,
                    *cmd, out: out, err: out)
            if (rc != 0) {
                println "Failed with output:\n${out}"
                assert rc == 0
            }
        }
    }

    @Test
    void testSshKeys() {
        DockerHost host = TestDockerHosts.primary

        LuciboxModel box = new LuciboxModel('lucitest')
        Containers c = new Containers(box)
        Container con = c.sshKeys(host)
        // TODO add asserts
    }

    @Test
    void testJenkinsConfig() {
        DockerHost host = TestDockerHosts.primary
        LuciboxModel box = new LuciboxModel('lucitest')
        box.dockerHost = host
        box.service('jenkins') {
            seedJob.with {
                jobDslFiles(new ClasspathResources().resourceAsFile('testJobDsl.g', 'testJobDsl.groovy'))
            }
        }
        Containers c = new Containers(box)
        Container con = c.jenkinsConfig(box.jenkins)
        con.verify('ls', '/luci/jenkins/jobDsl.d/testJobDsl.groovy')
    }
}
