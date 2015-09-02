package net.praqma.luci.utils

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Memoized

import java.util.regex.Matcher

/**
 * Represent 'important' external programs for Luci to work.
 */
@CompileStatic
class Binary {

    static List<File> path = constructPath()

    static Map<String, Binary> known = [:]

    String name
    String minVersion

    /** Is this binary critical for Luci to work? */
    boolean critical = true

    private extractVersionPattern

    File getExecutable() {
        String path = findExecutable(name)
        return path ? new File(path) : null
    }

    static {
        init()
    }

    private static void init() {
        Binary docker = new Binary(name: 'docker', minVersion: '1.8.0')
        docker.extractVersionPattern = /.*version ([\d.]*).*/
        known[docker.name] = docker

        Binary dockerCompose = new Binary(name: 'docker-compose', minVersion: '1.4.0')
        dockerCompose.extractVersionPattern = /^docker-compose version: ([\d.]*).*/
        known[dockerCompose.name] = dockerCompose

        Binary dockerMachine = new Binary(name: 'docker-machine', minVersion: '0.4.0')
        dockerMachine.extractVersionPattern = /^docker-machine version ([\d.]*).*/
        dockerMachine.critical = false
        known[dockerMachine.name] = dockerMachine
    }

    private static String findExecutable(String name) {
        // TODO handle windows
        File file = (File) path.findResult { File pathElement ->
            File f = new File(pathElement, name)
            f.canExecute() ? f : null
        }
        return file?.path
    }

    @Memoized
    private String getVersionLine() {
        if (executable == null) {
            throw new RuntimeException("No executable found for '${name}'")
        }
        assert executable != null
        [executable, '--version'].execute().text.readLines()[0]
    }

    void report(PrintWriter out) {
        out.println("${name}:")
        if (!executable) {
            out.println "\tno executable found"
        } else {
            out.println("\tExecutable: ${executable}")
            if (actualVersion) {
                out.println "\tVersion: ${actualVersion} (output: '${versionLine}')"
            } else {
                out.println "\tUnable to detect version from: ${versionLine}"
            }
            out.println "\tMinimum required version: ${minVersion}"
        }
        out.flush()
    }

    @Memoized @CompileDynamic
    private String getActualVersion() {
        if (executable) {
            Matcher matcher = versionLine =~ extractVersionPattern
            if (matcher.matches()) {
                return matcher[0][1]
            } else {
                return null
            }
        }
    }

    private static List<File> constructPath() {
        List<String> path = []
        String luciPath = System.properties['net.praqma.luci.path'] as String
        if (luciPath) {
            path.addAll(pathToList(luciPath))
        }
        path.addAll(pathToList(System.getenv('PATH')))
        return path.collect { new File(it) }
    }

    private static List<String> pathToList(String pathString) {
        return pathString.split(File.pathSeparator) as List<String>
    }

}
