package scripts

import jenkins.model.*
import hudson.model.*
import hudson.tasks.*
import hudson.model.labels.*
import javaposse.jobdsl.plugin.*
import org.jvnet.hudson.plugins.triggers.startup.*

assert System.getenv('LUCI_SEED_JOB_NAME')
String seedJobName = System.getenv('LUCI_SEED_JOB_NAME')

String jobDslDir = 'jobDsl.d'

println "LUCI: Creating Seed Job: ${seedJobName}"

def project = Jenkins.instance.getItem(seedJobName)
if (project != null) {
    project.delete()
}
project = Jenkins.instance.createProject(FreeStyleProject, seedJobName)
project.assignedLabel = new LabelAtom('master')

// Job dsl are located outside workspace. The jobdsl plugin needs them in workspace
// Create a build step to copy them

project.buildersList.add(new Shell("mkdir -p ${jobDslDir}\ncp -a /luci/jenkins/jobDsl.d/* ${jobDslDir}/"))

// Create build step to execute job dsl

ExecuteDslScripts.ScriptLocation scriptLocation = new ExecuteDslScripts.ScriptLocation('false', "${jobDslDir}/*", 'ignore')
boolean ignoreExisting = false
RemovedJobAction removedJobAction = RemovedJobAction.IGNORE

project.buildersList.add(new ExecuteDslScripts(scriptLocation, ignoreExisting, removedJobAction))

// Add trigger to run when Jenkins starts

project.addTrigger(new HudsonStartupTrigger('master', null))


println "LUCI: Seed job created"