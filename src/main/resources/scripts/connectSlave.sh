#! /bin/sh

slaveName=$1

slaveJar=/luci/data/jenkinsSlave/slave.jar

while true ; do
  echo "Connecting to master"
  /luci/mixins/java/bin/java -jar $slaveJar -jnlpUrl http://lucibox/jenkins/computer/$slaveName/slave-agent.jnlp
  echo "Connection to Master terminated! Reconnect attempt in 2 minutes "
  # Retry logic is also built into slave.jar. It is quite exceptional to end up here
  sleep 120
done
