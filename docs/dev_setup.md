## Developer Setup

## Install and Update Ubuntu Desktop (cur ver 20.04)

1. Boot to a live version of Ubuntu Desktop on a cd or a usb key
1. Install Ubuntu on the target hard drive
1. Update ubuntu once it is finished installing
   1. `sudo apt update`
   1. `sudo apt upgrade`
   1. `sudo apt full-upgrade`
   1. `sudo apt autoremove`

## Install Java (cur ver11)

Detailed instructions found
[here](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-on-ubuntu-18-04)

1. `sudo apt install default-jdk`
1. `java -version`
1. `javac -version`

## Install Docker

Detailed instructions found
[here](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-16-04)

1. `curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -`
1. `sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"`
1. `sudo apt-get update`
1. `apt-cache policy docker-ce`
1. `sudo apt-get install -y docker-ce`
1. `sudo systemctl status docker`

## Setup Docker (Optional)

Detailed instructions found
[here](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-16-04).
This will enable you to call docker without calling sudo.

1. `sudo usermod -aG docker ${USER} `
1. `su - ${USER}`
1. `id -nG`

## Install Docker-Compose

Detailed instructions found
[here](https://www.digitalocean.com/community/tutorials/how-to-install-docker-compose-on-ubuntu-18-04)

1. `` sudo curl -L https://github.com/docker/compose/releases/download/1.21.2/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose ``
1. `sudo chmod +x /usr/local/bin/docker-compose`
1. `docker-compose --version`

## Install Chrome

1. Download Chrome
1. Run Chrome installer
1. Make chrome your default browser

## Download OpenELIS-Global-2

1. Fork the repo at
   [https://github.com/I-TECH-UW/OpenELIS-Global-2](https://github.com/I-TECH-UW/OpenELIS-Global-2)
1. `cd /path/to/eclipse/workspace`
1. `git clone [git@github.com](mailto:git@github.com):{Your_Github_Account}/OpenELIS-Global-2.git --recurse-submodules`

## Test If OpenELIS-Global-2 Deploys

1. Open terminal to the OpenELIS-Global-2 directory
1. `docker-compose up -d --build`
1. Access the application at
   [https://localhost:8443/OpenELIS-Global](https://localhost:8443/OpenELIS-Global)
1. Might need to dismiss a security warning

## Developing in Eclipse

### Install Maven

Detailed instructions
[here](https://linuxize.com/post/how-to-install-apache-maven-on-ubuntu-18-04/)

1. `sudo apt install maven`
1. `mvn -version`

### Install Eclipse

1. Navigate to Eclipse download website (https://www.eclipse.org/downloads/)
1. Download the installer
1. Run the installer, choosing the Java ee version of eclipse

### Optionally Install Eclipse Plugins

1. Jaspersoft Reports
1. eGit

### Install Lombok in Eclipse

Instructions are here https://projectlombok.org/setup/eclipse

### Setup Project in Eclipse

1. File > Import > Projects from Folder or Archive
1. ensure OpenELIS-Global-2 is selected (and dataexport-core and dataexport-api
   which are under eclipse)
1. ensure detect and configure project natures is selected
1. finish
1. right click OpenELIS-Global-2 project > properties > Java Build Path >
   Projects > classpath > add
1. add dataexport-core and dataexport-api

### Setup Tomcat Server

1. [Download tomcat](https://tomcat.apache.org/download-90.cgi) as tar.gz (cur
   ver 9)
1. Extract into to your eclipse workspace directory (or wherever you want, but
   remember the location)
1. Select servers tab in eclipse
1. Right click in the servers tab, and select new > server
1. Find tomcat 9 in the list, click it, and select next
1. Select your tomcat installation directory (wherever you extracted it in
   step 2)
1. Click next
1. Add Open-Elis-Global-2 to the server and click finish

### Configure Tomcat server

1. Double click on the tomcat 9.0 server in the servers tab
1. Click ‘Open launch configuration’
1. Navigate to the Arguments tab
1. Add the following 3 arguments to the VM arguments section, substituting
   whatever your local arguments are for your database
   1. -Ddatasource.password=clinlims -Ddatasource.username=clinlims
      -Ddatasource.url=jdbc:postgresql://localhost:15432/clinlims?currentSchema=clinlims
1. click OK to finish
1. optionally, extend the server timeout to 200 seconds

### Configure Tomcat to Support ssl

1. Expand the Servers folder in the projects tab
1. Expand the server that runs OE
1. Copy the contents of OpenELIS-Global-2/dev/tomcat/oe_server.xml into Tomcat
   v9.0 Server at localhost-config/server.xml
1. Edit Tomcat v9.0 Server at localhost-config/server.xml, substituting your
   paths
   1. replace &lt;Server port="-1" shutdown="SHUTDOWN"> with &lt;Server
      port="8005" shutdown="SHUTDOWN">
   1. replace keystoreFile=”/run/secrets/keystore” with
      keystoreFile=”/path/to/openelis_global_2/dev/https/oe_selfsigned.keystore”
   1. replace truststoreFile="/run/secrets/truststore" with
      truststoreFile="/path/to/openelis_global_2/dev/https/oe_selfsigned.truststore"
   1. save

### Point to your dev common.properties

1. `sudo mkdir /run/secrets`
1. `sudo ln -s /path/to/project/dev/eclipse_common.properties /run/secrets/common.properties`
1. this is the file that will allow you to configure OE while running in
   eclipse. Ensure values filled out work for your dev environment. do not
   commit changes made for your dev environement.

### create directory for logs

1. `sudo mkdir /var/lib/openelis-global/logs/`
1. `sudo chmod 777 /var/lib/openelis-global/logs/`

### Run Everything but OE in docker

1. Comment out the oe.openelisci.org service in docker-compose.yml
1. `docker-compose up -d`
1. if oe is already running in docker, kill the container

### Start Tomcat in Eclipse

1. Make sure your project builds (project clean will normally trigger this)
1. Click start server in normal, or debug mode
