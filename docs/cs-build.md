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
- git checkout reduced-stack

## **_Create Certificates_**

It is recommended to Generate a CA and generate certificates off of this CA,
then tell all the servers to trust the same CA (assuming you have control over
said CA so only services you want are trusted), but using the same cert for all
the services should also work.

Useful links:

Creating a CA:
[https://scriptcrunch.com/create-ca-tls-ssl-certificates-keys/](https://scriptcrunch.com/create-ca-tls-ssl-certificates-keys/)

Creating a cert with SAN:
[https://www.golinuxcloud.com/openssl-generate-csr-create-san-certificate/](https://www.golinuxcloud.com/openssl-generate-csr-create-san-certificate/)

Keystore Truststore:
[http://docs.openelis-global.org/en/latest/install/](http://docs.openelis-global.org/en/latest/install/)

Pem key > ./prod/ssl/cs.key

Pem cert > ./prod/ssl/cs.crt

Pem key & pem.crt > ./prod/ssl/cs.keystore

Pem cert and/or Pem CA cert > ./prod/ssl/cs.truststore

openssl pkcs12 -inkey prod/ssl/cs.key -in prod/ssl/cs.crt -export -out
prod/ssl/cs.keystore

keytool -import -alias csCert -file prod/ssl/cs.crt -storetype pkcs12 -keystore
prod/ssl/cs.truststore

Make sure OE instances trust the cert (or better yet, the CA) for the
Consolidated-server by loading them into their truststore. If the
Consolidated-server is behind a load balancer that does ssl offloading, this
means OE will need to trust the offloaders cert or CA

## Configuring Projects

Choose one or the other

### Quick Config Container(s):

- find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i
  's/host\.openelis\.org/&lt;insert server address here>/g'
- find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i
  's/databaseAdminPassword/&lt;insert database admin password for cs_admin
  here>/g'
- find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i
  's/databasePassword/&lt;insert database password for cs_user here>/g'
- find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i
  's/passwordForKeystore/&lt;insert keystore password here>/g'
- find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i
  's/passwordForTruststore/&lt;insert truststore password here>/g'
- If not docker db:
  - find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i
    's/consolidated-server-data:5432/&lt;insert server:port for database>/g'

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
