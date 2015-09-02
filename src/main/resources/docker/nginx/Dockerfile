FROM luci/base:0.2

RUN apt-key adv --keyserver hkp://pgp.mit.edu:80 --recv-keys 573BFD6B3D8FBC641079A6ABABF5BD827BD9BF62
RUN echo "deb http://nginx.org/packages/mainline/debian/ jessie nginx" >> /etc/apt/sources.list

ENV NGINX_VERSION 1.9.4-1~jessie

RUN apt-get update && \
    apt-get install -y ca-certificates nginx=${NGINX_VERSION}

RUN apt-get install -y apache2-utils

# forward request and error logs to docker log collector
RUN ln -sf /dev/stdout /var/log/nginx/access.log && ln -sf /dev/stderr /var/log/nginx/error.log

RUN rm /etc/nginx/conf.d/default.conf

VOLUME ["/var/cache/nginx"]

COPY luci.conf           /etc/nginx/conf.d/
COPY services/*.conf     /luci/etc/nginx/available.d/
COPY entrypoint.sh       /

RUN mkdir -p /luci/wwwroot && mkdir -p /luci/etc/nginx/conf.d
#COPY wwwroot/* /luci/wwwroot/

COPY bin/* /luci/bin/

COPY certs/server.key /luci/etc/nginx/certs/server.key
COPY certs/server.crt /luci/etc/nginx/certs/server.crt

EXPOSE 80 443

ENTRYPOINT ["/tini", "--", "/entrypoint.sh"]

