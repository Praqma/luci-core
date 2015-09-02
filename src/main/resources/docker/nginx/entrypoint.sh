#! /bin/bash

luciboxName=unknown
port=80

while getopts "s:n:p:" arg; do
  case $arg in
      s) services=$OPTARG          ;;  # enabled services
      n) luciboxName=$OPTARG       ;;  # name of lucibox
      p) port=$OPTARG              ;;  # The port where nginx is listning
  esac
done
echo "Services: $services"

export LUCIBOX_NAME=$luciboxName
export LUCIBOX_PORT=$port

shift $((OPTIND-1))

for s in $services ; do
    ln -s /luci/etc/nginx/available.d/$s.conf /luci/etc/nginx/conf.d/
done

/luci/bin/generateIndexHtml.sh $luciboxName $services > /luci/wwwroot/index.html

# if `docker run` first argument start with `--` the user is passing nginx launcher arguments
if [[ $# -lt 1 ]] || [[ "$1" == "--"* ]]; then
   exec nginx -g "daemon off;" "$@"
fi

# Assume user want to run his own process, for sample a `bash` shell to explore this image
exec "$@"



