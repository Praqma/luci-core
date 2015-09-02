package net.praqma.luci.utils

import com.google.common.io.ByteStreams
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.praqma.luci.docker.DockerHost

@CompileStatic
class ExternalCommand {

    /** Docker commands are executed against this docker host */
    final DockerHost dockerHost

    ExternalCommand(DockerHost dockerHost = null) {
        assert dockerHost == null || dockerHost.uri != null
        this.dockerHost = dockerHost
    }

    @CompileDynamic
    int execute(String... cmd) {
        return execute([:], *cmd)
    }

    int execute(Map mapArgs, String... cmd) {
        assert cmd.findAll { it == null }.empty
        String c = Binary.known[cmd[0]]?.executable
        if (c) {
            cmd = ([c] + cmd[1..-1])
        }
        if (mapArgs.log) {
            println "CMD: ${cmd.join(' ')}"
        }
        ProcessBuilder pb = new ProcessBuilder(cmd)
        Map<String, String> env = pb.environment()
        if (dockerHost == null) {
            // Don't change env
        } else {
            Map<String, String> m = dockerHost.envVars
            m.each { key, value ->
                if (value == null) {
                    env.remove(key)
                } else {
                    env[key] = value
                }
            }
        }

        Process process = pb.start()

        Thread t1 = inThread(mapArgs.in != null ? mapArgs.in : System.in, process.outputStream)
        Thread t2 = outThread(mapArgs.out != null ? mapArgs.out : System.out, process.inputStream)
        Thread t3 = outThread(mapArgs.err != null ? mapArgs.err : System.err, process.errorStream)

        process.waitFor()
        // wait for t2 and t3 to finish, i.e. all output of the process has been processed
        t2.join()
        t3.join()
        // t1 might still be running waiting for input to be consumed. Close the
        // stream it is writing to
        process.outputStream.close()

        int exitValue = process.exitValue()
        if (mapArgs.log) {
            println "CMD: ${cmd.join(' ')}, exit value: ${exitValue}"
        }
        return exitValue
    }

    private Thread inThread(input, OutputStream outputStream) {
        return Thread.start {
            switch (input) {
                case InputStream:
                    ByteStreams.copy(input as InputStream, outputStream)
                    break
                case Closure:
                    (input as Closure)(outputStream)
                    break
                default:
                    throw new IllegalArgumentException("Don't know how to read process input from '${input}'")
            }
        }
    }

    private Thread outThread(output, InputStream inputStream) {
        return Thread.start {
            switch (output) {
                case StringBuffer:
                    StringBuffer buffer = (StringBuffer) output
                    inputStream.eachLine { String line ->
                        buffer << line << "\n"
                    }
                    break
                case OutputStream:
                    ByteStreams.copy(inputStream, output as OutputStream)
                    break
                case Closure:
                    (output as Closure)(inputStream)
                    break
                default:
                    throw new IllegalArgumentException("Don't know how to add process output to '${output}'")
            }
        }
    }

    private static String[] path = System.getenv('PATH').split(File.pathSeparator)
    /**
     * Find a binary on the PATH and if not there look for the first
     * suggestion that exist.
     *
     * @param name
     * @param suggestions
     * @return
     */
    private static String findBinary(String name, String... suggestions) {
        // TODO handle windows
        File file = (File) path.findResult { String pathElement ->
            File f = new File(pathElement, name)
            f.canExecute() ? f : null
        }
        if (file == null) {
            file = (File) suggestions.findResult { String suggestion ->
                File f = new File(suggestion)
                f.canExecute() ? f : null
            }
        }
        return file?.path
    }

}
