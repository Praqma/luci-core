#! /bin/bash

[ -f /luci/mixins/java/luci-init.sh ] && source /luci/mixins/java/luci-init.sh

set -e

slaveAgentPort=50000
executors=0

while getopts "d:c:j:e:s:t:a:p:x:" arg; do
  case $arg in
    d) dataContainer=$OPTARG ;;  # Name of data container that is used by slaves
    c) dockerUrl=$OPTARG ;;      # Url for (non-TLS) docker host to run slaves in                    
    j) jenkinsUrl=$OPTARG ;;     # Url to access jenkins with (from the outside)                     
    e) adminEmail=$OPTARG ;;     # Admin email in jenkins configuration
    s) staticSlaves=$OPTARG ;;   # List of slaves. Contains name, executores and labels for each slave
    t) onDemandSlaves=$OPTARG ;; # List of on demand slaves
    a) slaveAgentPort=$OPTARG ;; # Port for communicating with slave agent
    p) plugins=$OPTARG ;;        # Plugins to install
    x) executors=$OPTARG ;;      # Number of executors on masters
  esac
done

shift $((OPTIND-1))

export JENKINS_SLAVE_AGENT_PORT=$slaveAgentPort

# Generate configuragtion files
/luci/bin/generateJenkinsConfigXml.sh $dataContainer $dockerUrl $slaveAgentPort $executors $onDemandSlaves > /usr/share/jenkins/ref/config.xml
/luci/bin/generateJenkinsLocateConfiguration.sh $jenkinsUrl $adminEmail > /usr/share/jenkins/ref/jenkins.model.JenkinsLocationConfiguration.xml

echo "Installing plugins: $plugins"
/luci/bin/addPlugin.sh $plugins

# Copy files from /usr/share/jenkins/ref into /var/jenkins_home
# So the initial JENKINS-HOME is set with expected content. 
# Don't override, as this is just a reference setup, and use from UI 
# can then change this, upgrade plugins, etc.
copy_reference_file() {
	f=${1%/} 
	echo "$f" >> $COPY_REFERENCE_FILE_LOG
    rel=${f:23}
    dir=$(dirname ${f})
    echo " $f -> $rel" >> $COPY_REFERENCE_FILE_LOG
	if [[ ! -e /var/jenkins_home/${rel} ]] 
	then
		echo "copy $rel to JENKINS_HOME" >> $COPY_REFERENCE_FILE_LOG
		mkdir -p /var/jenkins_home/${dir:23}
		cp -r /usr/share/jenkins/ref/${rel} /var/jenkins_home/${rel};
		# pin plugins on initial copy
		[[ ${rel} == plugins/*.jpi ]] && touch /var/jenkins_home/${rel}.pinned
	fi; 
}
export -f copy_reference_file
echo "--- Copying files at $(date)" >> $COPY_REFERENCE_FILE_LOG
find /usr/share/jenkins/ref/ -type f -exec bash -c 'copy_reference_file {}' \;

/luci/bin/createStaticSlaves.sh $staticSlaves

# if `docker run` first argument start with `--` the user is passing jenkins launcher arguments
if [[ $# -lt 1 ]] || [[ "$1" == "--"* ]]; then
   exec java $JAVA_OPTS -jar /usr/share/jenkins/jenkins.war $JENKINS_OPTS "$@"
fi

# As argument is not jenkins, assume user want to run his own process, for sample a `bash` shell to explore this image
exec "$@"
