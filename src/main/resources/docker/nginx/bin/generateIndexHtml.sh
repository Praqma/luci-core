#! /bin/bash

name=$1
shift
services=$@

cat <<EOF
<html>
  <head>
  </head>
  <body>
    <h1>LUCIBOX $name</h1>
    <ul>
    $(for s in $services ; do
      echo "<li><a href="/$s/">$s</a></li>"
    done)
    </ul>
  </body>
</html>
EOF
