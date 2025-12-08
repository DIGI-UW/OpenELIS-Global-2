# Installation

## Before Running

### Firewall Access - out

All should be using http/https ports (80, 443)

- archive.ubuntu.com - Ubuntu package manager
- \*.docker.com - install docker, docker hub
  - get.docker.com
  - download.docker.com
  - hub.docker.com
- github.com - get projects
- repo.maven.apache.org - maven downloads

### Firewall Access - in

- 80, 443 - nginx (covers openhim-console)
- 8080, 5000, 5001, 5050, 5051, 5052, 7788 - openhim-core

### Firewall Access - intraservice

- 5432 - postgres db
- 8444 - fhir server
- 27017 - mongo db

## Installing Commands

### Update Ubuntu:

- sudo apt-get update
- sudo apt-get upgrade

### Install git:

- sudo apt-get install git

### Install Docker:

- curl https://get.docker.com/ | sh -

### Install Docker-compose:

- sudo curl -L
  "https://github.com/docker/compose/releases/download/1.28.2/docker-compose-$(uname
  -s)-$(uname -m)" -o /usr/local/bin/docker-compose
- sudo chmod +x /usr/local/bin/docker-compose
- sudo docker-compose --version

## Downloading projects

### Download Consolidated Server project:

- git clone
  [https://github.com/DIGI-UW/Consolidated-Server.git](https://github.com/DIGI-UW/Consolidated-Server.git)
  --recurse-submodules
- cd Consolidated-Server/
- git checkout develop

## **_Create Certificates_**

It is recommended to Generate a CA and generate certificates off of this CA,
then tell all the servers to trust the same CA (assuming you have control over
said CA so only services you want are trusted), but using the same cert for all
the services should also work.

Example instructions:

Create a CA (adapted from instuctions
[here](https://scriptcrunch.com/create-ca-tls-ssl-certificates-keys/)):

`mkdir openssl && cd openssl`

`openssl genrsa -aes256 -out ca.key 4096`

`openssl req -x509 -new -nodes -key ca.key -sha256 -days 3652 -out ca.crt`

Create a signed key/cert:

`openssl genrsa -aes256 -out server.key 2048`

Replace the arguments in < > before running the next command

```
cat > csr.conf <<EOF
[ req ]
default_bits = 2048
prompt = no
default_md = sha256
req_extensions = req_ext
distinguished_name = dn

[ dn ]
C = <Country-Short-Name>
ST = <State>
L = <City>
O = <Organization>
OU = <Organization Unit>
CN = CS Cert

[ req_ext ]
subjectAltName = @alt_names

[ alt_names ]
DNS.1 = <url-1>
DNS.2 = *.openelis.org
IP.1 = <ip-address-1>
IP.2 = <ip-address-2>

EOF
```

`openssl req -new -key server.key -out server.csr -config csr.conf`

`openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 1826 -extensions req_ext -extfile csr.conf`

`cat server.crt ca.crt > server-chain.crt`

`cd ..`

Move files to correct locations:

`cp openssl/server-chain.crt prod/ssl/cs_frontend.crt`

`cp openssl/server-chain.crt prod/ssl/cs.crt`

`cp openssl/server.key prod/ssl/cs_frontend.key`

`cp openssl/server.key prod/ssl/cs.key`

`openssl pkcs12 -inkey prod/ssl/cs.key -in prod/ssl/cs.crt -export -out prod/ssl/cs.keystore`

`sudo chmod +r prod/ssl/cs.keystore`

`sudo apt install default-jre`

`keytool -import -alias csCert -file prod/ssl/cs.crt -storetype pkcs12 -keystore prod/ssl/cs.truststore`

Ensure the key is encrypted with the same password as the keystore

Make sure OE instances trust the cert (or better yet, the CA) for the
Consolidated-server by loading them into their truststore. If the
Consolidated-server is behind a load balancer that does ssl offloading, this
means OE will need to trust the offloaders cert or CA

## Configuring Projects

Choose one or the other

### Quick Config Container(s):

- Run ./configure.sh
  - Server address: the address that all of the services are running on (this
    will be the url they will reach out to to communicate, it should be the DNS
    entry of the server)
  - Db admin password: password for cs_admin
  - Db password: password for cs_user
  - Keystore password: key encryption password
  - Truststore password: trust encryption password

### Detailed Configure Container(s):

- cd Consolidated-Server/
- Edit ./prod/properties/default.json
  - Set host to the name used to access this server
- edit file ./prod/conf/nginx.conf
  - Replace any host.openelis.org names with the server’s DNS entry
- If using a docker database running on the same machine: Edit
  prod/database/database.env
  - write a unique password
- Otherwise manually create a database, create a user, and run
  ./prod/database/dbInit.sql
- Whichever database is used, fill out the db connection info in:
  - ./prod/database/user
  - ./prod/database/password
- Fill out ./prod/properties/application.yaml
  - Server address:hapi.fhir. Server_address SHOULD BE THE ACCESS POINT TO THIS
    SERVER IE THE OPENHIM PATHWAY
  - Db connection info: datasource.\*
- Fill out ./prod/tomcat/hapi_server.xml
  - truststorePass and keystorePass

### Configure OpenHIM Core:

- sudo docker compose -f docker-compose-production.yml up -d --build
- Log into OpenHIM-console Login to
  [root@openhim.org](mailto:root@openhim.org):openhim-password
- Make note of the password you change to
- Navigate to the certificate page
- Upload your server certificate and key pair for OpenHIM
- Core is now using this cert key pair

### Configure Clients:

- Add a trusted cert for your client (and the chain of CAs you trust)
- Navigate to client tab
- Add client
  - Add new role: fhir-pusher
  - Add new role: fhir-puller
  - Give client ID and Client name
  - Select client certificate as authentication method

### Configure Channels:

- Navigate to channels tab
- Add channel: FHIR Channel
  - HTTP
  - URL pattern: /fhir.\*
  - PRIVATE
  - fhir-pusher
  - fhir-puller
  - Route
  - name: hapi-fhir-jpaserver
  - type: http
  - secured: yes
  - CA ca
  - Host: &lt;server-path>
  - Port: 8444

# Upgrade

## Updating Commands

### Update Ubuntu

- sudo apt-get update
- sudo apt-get upgrade

## Update Git Project

- git stash (optional, unless there are conflicts)
- git submodules init
- git pull origin --recurse-submodules
- git checkout -- configure.sh

## Configuring Projects

Choose one or the other

### Quick Config Container(s):

- Run ./configure.sh
  - Server address: the address that all of the services are running on (this
    will be the url they will reach out to to communicate, it should be the DNS
    entry of the server)
  - Db admin password: password for cs_admin
  - Db password: password for cs_user
  - Keystore password: key encryption password
  - Truststore password: trust encryption password

# Notes

## Passwords

ALL passwords should be recorded into the 1Password Vault
