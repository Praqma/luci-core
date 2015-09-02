# TODO fix to use debian:jessie
FROM ubuntu:14.04

ENV DOCKER_VERSION=1.8.1 \
    DOCKER_MACHINE_VERSION=0.4.0

RUN apt-get update && apt-get install -y python-dev python-setuptools libffi-dev \
            libssl-dev ca-certificates openssh-client wget curl && apt-get clean \
    && easy_install pip \
    && pip install python-openstackclient python-novaclient \
           python-swiftclient python-heatclient python-cinderclient python-keystoneclient \
           python-neutronclient python-designateclient \
    && pip install -U docker-compose

# Install docker-machine
RUN mkdir /root/bin && wget -q https://github.com/docker/machine/releases/download/v$DOCKER_MACHINE_VERSION/docker-machine_linux-amd64 -O /root/bin/docker-machine

# Install client. We use the --since flag on docker logs, and it only works in Docker 1.7.0 and upwards 
RUN wget -q https://get.docker.com/builds/Linux/x86_64/docker-$DOCKER_VERSION -O /root/bin/docker && chmod +x /root/bin/* 

ENV PATH=/root/bin:$PATH
ADD bin/* /root/bin/


