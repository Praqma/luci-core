#! /bin/sh

slaveName=$1
port=$2

slaveJar=/luci/data/jenkinsSlave/slave.jar

jnlpUrl=http://lucibox:$port/jenkins/computer/$slaveName/slave-agent.jnlp

echo "jnlpUrl: $jnlpUrl"

while true ; do
  echo "Connecting to master"
  /luci/mixins/java/bin/java -jar $slaveJar -jnlpUrl $jnlpUrl
  echo "Connection to Master terminated! Reconnect attempt in 2 minutes "
  # Retry logic is also built into slave.jar. It is quite exceptional to end up here
  sleep 120
done
