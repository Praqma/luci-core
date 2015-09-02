#! /bin/bash

# Download and install plugins specified on command line

set -e

REF=/usr/share/jenkins/ref/plugins
CACHE=/luci/cache/jenkinsPlugins
mkdir -p $REF

for spec in "$@" ; do
    plugin=(${spec//:/ });
    [[ -z ${plugin[1]} ]] && plugin[1]="latest"
    dest=$REF/${plugin[0]}.jpi
    name=${plugin[0]}:${plugin[1]}
    if [[ ${plugin[1]} != "latest" ]] && [[ -f "$CACHE/$name" ]] ; then
        cp "$CACHE/name" $dest
    else
        echo "Downloading $name"
        curl -s -L -f ${JENKINS_UC}/download/plugins/${plugin[0]}/${plugin[1]}/${plugin[0]}.hpi -o $dest
    fi
done

