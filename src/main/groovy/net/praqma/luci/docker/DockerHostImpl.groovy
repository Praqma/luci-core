package net.praqma.luci.docker

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.praqma.luci.docker.net.praqma.luci.docker.hosts.DockerMachineHost

@CompileStatic
class DockerHostImpl implements DockerHost {

    static DockerHost fromDockerMachine(String name) {
        return new DockerMachineHost(name)
    }

    static DockerHost getDefault() {
        String dockerMachine = System.properties['net.praqma.luci.dockerMachine'] as String
        if (dockerMachine != null) {
            println "Using ${dockerMachine} as default host. Specified as system property"
            return fromDockerMachine(dockerMachine)
        } else {
            return fromEnv()
        }
    }

    static DockerHostImpl fromEnv() {
        if (System.getenv("DOCKER_HOST") != null) {
            return fromVariables(System.getenv()).orig("env vars") as DockerHostImpl
        } else {
            return null
        }
    }

    static DockerHostImpl fromVariables(Map<String, String> map) {
        String dockerHost = map['DOCKER_HOST']
        String dockerTlsVerify = map['DOCKER_TLS_VERIFY']
        String dockerCertPath = map['DOCKER_CERT_PATH']

        if (dockerHost == null || dockerHost == '') {
            throw new RuntimeException("'DOCKER_HOST' not defined")
        }
        DockerHostImpl h = new DockerHostImpl()
        h.uri = URI.create(dockerHost)
        h.tls = dockerTlsVerify == "1"
        if (dockerCertPath) {
            h.certPath = new File(dockerCertPath).absoluteFile
        } else {
            h.certPath = null
        }
        // Initialize is a noop for this host. So mark it now, no harm in that
        h.initialize()
        return h
    }

    @CompileDynamic
    static DockerHostImpl fromEnvVarsString(String s) {
        Map<String, String> m = [:]
        s.readLines().each { String line ->
            if (line.startsWith('export ')) {
                line = line.substring(7).trim()
                def (key, v) = line.split('=')
                if (v[0] == '"' && v[-1] == '"') {
                    v = v[1..-2]
                }
                m[key] = v
            }
        }
        return fromVariables(m)
    }


}
