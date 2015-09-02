FROM luci/tomcat7:0.2

# To update, check https://bintray.com/jfrog/artifactory/artifactory/view
ENV ARTIFACTORY_VERSION 3.9.2
ENV ARTIFACTORY_SHA1 245aeb7b2d77830462067d5a19c3bd32ae014ddf

# Fetch and install Artifactory OSS war archive.
RUN \
  echo $ARTIFACTORY_SHA1 artifactory.zip > artifactory.zip.sha1 && \
  curl -L -o artifactory.zip https://bintray.com/artifact/download/jfrog/artifactory/artifactory-${ARTIFACTORY_VERSION}.zip && \
  sha1sum -c artifactory.zip.sha1 && \
  unzip -j artifactory.zip "artifactory-*/webapps/artifactory.war" -d webapps && \
  rm artifactory.zip

# Expose tomcat runtime options through the RUNTIME_OPTS environment variable.
#   Example to set the JVM's max heap size to 256MB use the flag
#   '-e RUNTIME_OPTS="-Xmx256m"' when starting a container.
RUN echo 'export CATALINA_OPTS="$RUNTIME_OPTS"' > bin/setenv.sh

# Artifactory home
RUN mkdir -p /luci/artifactory
ENV ARTIFACTORY_HOME /luci/artifactory

# Expose Artifactories data, log and backup directory.
VOLUME /luci/artifactory/data
VOLUME /luci/artifactory/logs
VOLUME /luci/artifactory/backup

WORKDIR /luci/artifactory
