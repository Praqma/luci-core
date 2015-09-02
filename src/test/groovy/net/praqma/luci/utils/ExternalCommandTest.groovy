package net.praqma.luci.utils

import net.praqma.luci.utils.ExternalCommand
import org.junit.Test


class ExternalCommandTest {

    @Test
    void testExecute() {
        ExternalCommand cmd = new ExternalCommand()
        Closure input = { OutputStream os ->
            os << "HelloWorld\n"
        }
        Closure output = { InputStream is ->
            String firstLine = is.readLines()[0].trim()
            assert firstLine == '11'
        }
        int exitCode = cmd.execute('sh', '-c', 'read x ; echo $x | wc -c', in: input, out: output)
        assert exitCode == 0
    }

    @Test
    void testDockerComposeIsOnPath() {
        int rc = new ExternalCommand().execute('docker-compose', '--version')
        assert rc == 0
    }

    @Test
    void testWriteToStringBuffer() {
        StringBuffer buffer = "" << ""
        new ExternalCommand().execute("echo", "abc", out: buffer)
        assert buffer.toString().trim() == 'abc'

    }
}
