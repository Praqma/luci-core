FROM luci/staticslave-javatools:0.3

ENV DOCKER_VERSION=1.8.1 \
    DOCKER_MACHINE_VERSION=0.4.1 \
    DOCKER_COMPOSE_VERSION=1.4.0

RUN curl -L https://get.docker.com/builds/Linux/x86_64/docker-$DOCKER_VERSION > /usr/local/bin/docker
RUN curl -L https://github.com/docker/machine/releases/download/v$DOCKER_MACHINE_VERSION/docker-machine_linux-amd64 > /usr/local/bin/docker-machine
RUN curl -L https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose


RUN chmod +x /usr/local/bin/docker
RUN chmod +x /usr/local/bin/docker-machine
RUN chmod +x /usr/local/bin/docker-compose

