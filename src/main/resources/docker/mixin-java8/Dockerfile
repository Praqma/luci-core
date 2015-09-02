FROM luci/base:0.2

RUN curl -L -k -b "oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u60-b27/server-jre-8u60-linux-x64.tar.gz | gunzip -c | tar x
RUN mkdir -p /luci/mixins && mv /jdk1.8.0_60 /luci/mixins/java

RUN echo "export JAVA_HOME=/luci/mixins/java\nexport JAVA_VERSION=1.8u60\nexport PATH=/luci/mixins/java/bin:$PATH" > /luci/mixins/java/luci-init.sh

VOLUME /luci/mixins/java
