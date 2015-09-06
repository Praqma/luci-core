package net.praqma.luci.utils

import com.google.common.io.ByteStreams
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovyx.gpars.GParsPool
import net.praqma.luci.docker.DockerHost

import java.util.concurrent.Future

@CompileStatic
class ExternalCommand {

    /** Docker commands are executed against this docker host */
    final DockerHost dockerHost

    /** These strings will be masked in logging */
    Collection<String> sensitiveData = []

    ExternalCommand(DockerHost dockerHost = null) {
        assert dockerHost == null || dockerHost.uri != null
        this.dockerHost = dockerHost
    }

    @CompileDynamic
    int execute(String... cmd) {
        return execute([:], *cmd)
    }

    @CompileDynamic
    int execute(Map mapArgs, String... cmd) {
        assert cmd.findAll { it == null }.empty
        String c = Binary.known[cmd[0]]?.executable
        if (c) {
            cmd = ([c] + cmd[1..-1])
        }
        String cmdFormatted
        if (mapArgs.log) {
            cmdFormatted = formatCmdForLogging(cmd.toList())
            println "CMD: ${cmdFormatted}"
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

        GParsPool.withPool(2) {
            // Handle stdin and stderr async
            Future stdin = { -> readInput(mapArgs.in != null ? mapArgs.in : null, process.outputStream) }.callAsync()
            Future stderr = { -> writeOutput(mapArgs.err != null ? mapArgs.err : System.err, process.errorStream) }.callAsync()

            writeOutput(mapArgs.out != null ? mapArgs.out : System.out, process.inputStream)
            stderr.get() // Wait until all stderr is handled

            process.waitFor()
            process.outputStream.close()
            stdin.get()
        }

        int exitValue = process.exitValue()
        if (mapArgs.log) {
            println "CMD: ${cmdFormatted}, exit value: ${exitValue}"
        }
        if (exitValue != 0 && mapArgs.foe) { // foe: Fail on Error
            throw new RuntimeException("Cmd failed, exitCode: ${exitValue}, cmd: ${formatCmdForLogging(cmd.toList())}")
        }
        return exitValue
    }


    private void readInput(input, OutputStream outputStream) {
        switch (input) {
            case null:
                return
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

    private void writeOutput(output, InputStream inputStream) {
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

    String formatCmdForLogging(Iterable<String> cmd) {
        cmd.collect {
            sensitiveData.contains(it) ? '****' : it
        }.join(' ')
    }

}
