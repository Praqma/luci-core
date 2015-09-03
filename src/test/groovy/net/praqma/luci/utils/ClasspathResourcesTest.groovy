package net.praqma.luci.utils

import org.junit.Test


class ClasspathResourcesTest {

    @Test
    void testLoadResourcesFromJar() {
        // Test with the Junit Test class. It is in the junit jar
        File f = new ClasspathResources().resourceAsFile('org/junit/Test.class')
        assert f.exists()
    }
}
