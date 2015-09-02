#! /bin/bash

nodesDir=${JENKINS_HOME:-/var/jenkins_home}/nodes/
for s in "$@" ; do
    slave=(${s//:/ })
    slaveName=${slave[0]}
    executors=${slave[1]}
    labels=${slave[@]:2}
    echo "Creating name node '$slaveName'"
    dir="$nodesDir/$slaveName"
    mkdir -p $dir
    cat > "$dir/config.xml" <<EOF
<?xml version='1.0' encoding='UTF-8'?>
<slave>
  <name>$slaveName</name>
  <description></description>
  <remoteFS>/var/jenkins</remoteFS>
  <numExecutors>$executors</numExecutors>
  <mode>NORMAL</mode>
  <retentionStrategy class="hudson.slaves.RetentionStrategy\$Always"/>
  <launcher class="hudson.slaves.JNLPLauncher"/>
  <label>$labels</label>
  <nodeProperties/>
  <userId>anonymous</userId>
</slave>
EOF
done
