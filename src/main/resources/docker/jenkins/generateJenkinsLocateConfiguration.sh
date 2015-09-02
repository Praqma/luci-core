#!/bin/sh

# define parameters which are passed in.
jenkinsUrl=$1
adminEmail=$2

cat << EOF
<?xml version='1.0' encoding='UTF-8'?>
<jenkins.model.JenkinsLocationConfiguration>
  <adminAddress>$adminEmail</adminAddress>
  <jenkinsUrl>$jenkinsUrl</jenkinsUrl>
</jenkins.model.JenkinsLocationConfiguration>
EOF
