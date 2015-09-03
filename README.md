# Luci Understands Continuous Integration

Core library for Luci.

This library defines a model for constructing a Lucibox. To construct just a model you have to
interact directly with the corresponding Groovy classes.

The is a Gradle plugin (in another project) that uses this to configure a Lucibox in Gradle

### Luci Settings

Luci uses settings defined in ~/.luci/settings.properties

Settings:
* testDockerMachine: Name of a Docker machine use for test

### Developers

To execute the test you need to define the Luci setting: testDockerMachine
