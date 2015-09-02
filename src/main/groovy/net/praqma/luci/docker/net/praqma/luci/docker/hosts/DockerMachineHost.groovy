package net.praqma.luci.docker.net.praqma.luci.docker.hosts

import net.praqma.luci.docker.DockerHost
import net.praqma.luci.docker.DockerHostImpl
import net.praqma.luci.docker.DockerMachineFactory
import net.praqma.luci.utils.ExternalCommand

class DockerMachineHost implements DockerHost {

    final String machineName

    final DockerMachineFactory factory

    DockerMachineHost(String machineName, DockerMachineFactory factory = null) {
        this.machineName = machineName
        this.factory = factory
        orig("machine:${machineName}")
    }

    void initialize() {
        initWithCountDown(3)
    }

    private void initWithCountDown(int n) {
        if (n == 0) {
            throw new RuntimeException("Failed to initialize ${this}")
        }
        if (isInitialized) {
            return;
        }
        ExternalCommand ec = new ExternalCommand()
        StringBuffer out = "" << ""
        StringBuffer err = "" << ""
        int rc = ec.execute('docker-machine', 'env', machineName, out: out, err: err)
        if (rc != 0) {
            String errString = err.toString()
            if (factory != null && errString.contains("Host does not exist")) {
                factory.getOrCreate(machineName)
                initWithCountDown(n - 1)
                return
            }
            if (err.toString().contains("is not running.")) {
                println "Starting machine: ${machineName}"
                rc = ec.execute('docker-machine', 'start', machineName)
                if (rc != 0) {
                    throw new RuntimeException("failed to start '${machineName}")
                }
                initWithCountDown(n - 1)
                return
            } else {
                throw new RuntimeException(err.toString())
            }
        }
        DockerHost dh = DockerHostImpl.fromEnvVarsString(out.toString()).orig("machine: ${machineName}")
        initFrom(dh)
        println "Initialized docker host '${this}"
    }

    String getStatus() {
        StringBuffer err = "" << ""
        StringBuffer out = "" << ""
        int rc = new ExternalCommand().execute('docker-machine', 'status', machineName, err: err, out: out)
        if (rc == 0) {
            return out.toString()
        } else {
            if (err.toString().indexOf("Host does not exist") > 0) {
                if (factory == null) {
                    return "does not exists. No factory."
                } else {
                    return "does not exists. Factory defined."
                }
            } else {
                throw new RuntimeException(err.toString())
            }
        }
    }

}
