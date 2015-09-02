The key files are generated manually

On OS X I have generated them with:

$ openssl genrsa -des3 -out server.key 1024

$ openssl req -new -key server.key -out server.csr

$ cp server.key server.key.orig

$ openssl rsa -in server.key.orig -out server.key

$ openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt