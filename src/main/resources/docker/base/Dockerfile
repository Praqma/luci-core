FROM debian:jessie

MAINTAINER info@praqma.net

# Make sure the package repository is up to date and install often used packages
RUN apt-get update && apt-get upgrade -y && apt-get install -y unzip curl && apt-get clean

# Add luci user and set password
RUN useradd -ms /bin/bash -u 5824 luci

# Add Tini, https://github.com/krallin/tini
ENV TINI_VERSION v0.5.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini
ENTRYPOINT ["/tini", "--"]