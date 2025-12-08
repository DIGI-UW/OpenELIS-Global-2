## Bare Metal Server Installation for OpenELIS Global 2.0 on Ubuntu 22.04 LTS

### Setup Ubuntu 22.04.2.0 LTS

1. Boot Ubuntu from a CD. [Note: Use Ubuntu Server 22.04.2.0 LTS, do NOT use
   desktop] [Download](https://ubuntu.com/download/server).
1. Chose UI language and keyboard layout
1. Set the network configuration
1. Use the default mirror
1. Set up the hard disk
1. Name the system: openelis
1. user openelis
1. set password and record it
   - I suggest adding the ssh key for each support user to enable passwordless
     connection.
1. Select Install OpenSSH server
   - this will allow you to ssh into this computer allowing copy/paste for
     Windows users through Putty, or connections via terminal on Mac and from
     the shell in LINUX
1. Skip the optional server snaps
1. Finalize the ubuntu install
1. Reboot

**NOTE: I like to connect via ssh if I’m going to be using a lot of resources
from my own computer here. This allows me to easily copy and paste commands
below. For windows, the best utility I’ve found is Mobaxterm, as it incoperates
a SCP client as well**
[available for free download online](https://mobaxterm.mobatek.net/). .

### Install Prerequisites for OpenELIS

1.  Ensure that the system is connected to the internet properly, you can try to
    ping google DNS at 8.8.8.8

        ping 8.8.8.8

1.  Open a command prompt and enter the following commands- this will install
    the needed services and install updates to the OS since the image was
    created. This updates the system from the sources in the sources list. It
    updates what new packages are available.

            sudo apt update && sudo apt upgrade

1.  Install Net Tools in order to find the IP Address

        sudo apt install net-tools

1.  Install Python

        sudo apt install python3

### Options for installing OpenELIS Global software

You can choose to install OpenELIS in an online mode for servers with fast
internet connections, and offline, using less internet connectivity by providing
a local copy of the images to be loaded.

### Online OpenELIS installation with Docker-Compose

This Option can be used where there is fast internet connectivity

#### Prerequisites for OpenELIS Online Setup

1. Install
   [Docker](https://linuxize.com/post/how-to-install-and-use-docker-on-ubuntu-20-04/)
   and
   [Docker Compose](https://linuxize.com/post/how-to-install-and-use-docker-compose-on-ubuntu-20-04/)

2. Install [git](https://github.com/git-guides/install-git)

#### Steps to Run Online Setup

1.  Clone the
    [OpenELIS-Global docker](https://github.com/DIGI-UW/openelis-docker)
    repository.

        git clone https://github.com/DIGI-UW/openelis-docker.git

1.  Move to the Project directory

         cd  openelis-docker

##### Running OpenELIS Global 3x in Docker

    docker compose up -d

###### The Instaces can be accesed at

| Instance     |                   URL                   | credentials (user : password) |
| ------------ | :-------------------------------------: | ----------------------------: |
| Legacy UI    | https://localhost/api/OpenELIS-Global/  |            admin: adminADMIN! |
| New React UI |           https://localhost/            |            admin: adminADMIN! |

##### Running OpenELIS Global 2x in Docker

    docker compose -f docker-compose-2x.yml up -d

###### The Instaces can be accesed at

| Instance |                   URL                    | credentials (user : password) |
| -------- | :--------------------------------------: | ----------------------------: |
| OpenElis | https://localhost:8443/OpenELIS-Global/  |            admin: adminADMIN! |

##### Running OpenELIS-Global2 from source code in docker

1.  Clone the [OpenELIS Global](https://github.com/DIGI-UW/OpenELIS-Global-2)
    repository.

         git clone https://github.com/DIGI-UW/OpenELIS-Global-2.git

1.  Build and Run the docker images from source code

    docker compose -f build.docker-compose.yml up -d --build

### Downloaded Installer Offline Setup

This Option can be used where there is a slow/unstable internet connectivity

#### Prerequisites for the OpenELIS Offline Setup

##### Create and Load SSL Certificates

OpenELIS uses SSL certificates to securely communicate with other software or
consolidated lab data servers. For a test or temporary instance, use a
self-signed certificate, and for a production instance create a proper signed
certifcate. **You must have a cert and key created and in the keystore and
truststore for the installer to run**

I will include 2 paths, one for generating your own self-signed cert, this is
good for just starting out or experimenting, and for using your real certs,
which is appropriate for production servers. If you have real certificates skip
down to
[Use a real certificate, best for production uses](#Use-a-real-certificate-best-for-production-uses)

##### Use a self signed certificate.

##### Generate a .crt and .key file for the domain you want to use.

The command below is for generating and using a self-signed certifcate. **Note:
for FQDN use \*.openelisci.org**

    sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /etc/ssl/private/apache-selfsigned.key -out /etc/ssl/certs/apache-selfsigned.crt

##### Create keystore from key and cert

Make the directories for the keystore

    sudo mkdir /etc/openelis-global/

    sudo openssl pkcs12 -inkey /etc/ssl/private/apache-selfsigned.key -in /etc/ssl/certs/apache-selfsigned.crt -export -out /etc/openelis-global/keystore

and then

    sudo cp /etc/openelis-global/keystore /etc/openelis-global/client_facing_keystore

**Be sure to remember your keystore password, you will need it later **

##### Create truststore with OpenELIS-Global's cert (or a CA that signs OE certs)

**Choose ONE of the two methods below to create your truststore**

1.  using keytool (more reliable):

        sudo apt install default-jre

        sudo keytool -import -alias oeCert -file /etc/ssl/certs/apache-selfsigned.crt -storetype pkcs12 -keystore /etc/openelis-global/truststore

    - set the truststore password

    **Be sure to remember your truststore password, you will need it later **

    - when prompted if you want to trust the cert type `yes`

2.  using openssl (less reliable, but doesn't require java):

        openssl pkcs12 -export -nokeys -in /etc/ssl/certs/apache-selfsigned.crt -out /etc/openelis-global/truststore

##### Use a real certificate, best for production uses

##### Create keystore from key and cert

Make the directories for the keystore

    sudo mkdir /etc/openelis-global/

make sure to record the password somewhere secure as you will need to enter it
elsewhere)

    sudo openssl pkcs12 -inkey path/toyour/key -in path/to/your/cert -export -out /etc/openelis-global/keystore

enter an export password

**Be sure to remember your keystore password, you will need it later**

and then

    sudo cp /etc/openelis-global/keystore /etc/openelis-global/client_facing_keystore

##### Create truststore with OpenELIS-Global's cert (or a CA that signs OE certs)

**Choose ONE of the two methods below to create your truststore**

1.  using keytool (more reliable):

        sudo apt install default-jre

        sudo keytool -import -alias oeCert -file path/to/your/cert -storetype pkcs12 -keystore /etc/openelis-global/truststore

    - set the truststore password

    **Be sure to remember your truststore password, you will need it later **

    - when prompted if you want to trust the cert type `yes`

2.  using openssl (less reliable, but doesn't require java):

        openssl pkcs12 -export -nokeys -in path/to/your/cert -out /etc/openelis-global/truststore

##### Ensure keystore/truststore permissions are all correct

Ensure all keystores have global read permission

    sudo chmod 644 /etc/openelis-global/keystore /etc/openelis-global/truststore /etc/openelis-global/client_facing_keystore

#### DownLoad and Unzip the Installation Files for Offline Setup

1.  Download OpenELIS Global2 Installer

    a.
    [Download latest installer package Here:](https://www.dropbox.com/sh/47lagjht4ynpcg8/AABORyLmkpVTtRReeD6wSnJra?dl=0)

        curl -L -O https://url_for_the _file.tar.gz

    b. EG: for OE 2.3 :

        curl -L -O https://www.dropbox.com/s/zrk5127xrg8cn6g/OpenELIS-Global_2.3.2.2_Installer.tar.gz

2.  Unpack and enter the installer by running the following commands in
    Terminal, Mobaxterm, or Putty, replacing all in the { } with the appropriate
    values

        tar xzf OpenELIS-Global_{installer_version}_Installer.tar.gz

        cd OpenELIS-Global_{installer_version}_Installer

    a. EG: tar -xvf OpenELIS-Global_2.3.2.2_Installer.tar.gz b. cd
    OpenELIS-Global_2.3.2.2_Installer/

3.  Optionally configure your install by editing setup.ini

    Find the section [DOCKER_VALUES] and set provide_database=True if you would
    like to use a Docker database

4.  Run the install script in Terminal or Putty

        sudo python3 setup_OpenELIS.py

#### Install OpenELIS Global2

**OpenELIS Global uses the following file to set things like the consolidated
server address, it is not overwritten by the installer.
/var/lib/openelisglobal/secrets/extra.properties**

1. Set the site identification number for this instance a. The site number is
   used to set the default test order prefix, and to identify the system to the
   consolidated server and other data systems.

1. Set the time zone for OpenELIS Application a. Select the region that your
   country is in a. Select the country a. Verify the time zone
1. Enter in the keystore password we set earlier
1. Same with the truststore
1. Enter an encryption key, this will help secure your data by encrypting your
   database

OpenELIS uses FHIR for much of its internal and external communication, if you
don't know what the options mean, leave them at the default.

1. Local FHIR store is the link to the local FHIR API
1. The remote FHIR store is used in the use case where OpenELIS is polling for
   lab orders and returning results. EG:
   [the FHIR2 Module for OpenMRS](../deployomrs)
1. The Consolidated Sevrer is a central server which collects lab data for
   reporting, serves as a master facility list, etc.

Wait while install procedure completes

Please note: OpenELIS Global 2.x is designed for and is testing on Chrome only.
Please be sure to use Chrome for OpenELIS.

4. Check if OpenELIS is running at
   https://{server_ip_address}:8443/OpenELIS-Global/

Default user: admin Default password: adminADMIN!

Configure the backup:

Follow the SOP at: [Backup Configuration](../backups)

To set the identifier for this particular instance, use the
/var/lib/openelisglobal/secrets/extra.properties file, and set the organization
value to the same identifier as is set in the consolidated server FHIR location
object. EG:
`org.openelisglobal.remote.source.identifier=Organization/8136bd30-901c-4d47-b133-72de813404ee`
