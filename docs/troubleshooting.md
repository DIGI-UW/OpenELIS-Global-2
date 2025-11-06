# Have you tried turning it off and back on again?

This may be the most basic of advice, but the first step should always be to
restart the docker containers.

`sudo docker restart`

# Disk Space Running Out

System monitoring applications should be installed alongside OE so that disk
space shortages can be discovered ahead of time. When a disk is near full, the
following solutions may be able to help.

## Check mounted log files

The following instructions will require the user have ssh access to the server
running OE OE mounts log files in 2 locations on the system:
`/var/lib/openelis-global/logs/` and `/var/lib/openelis-global/tomcatLogs/` If
the files here are getting large, the cleanup script is not successfully
deleting them. Ensure that `/var/lib/openelis-global/lib/logCleanup.sh` can be
run. The thresholds within `/var/lib/openelis-global/lib/logCleanup.sh` can also
be modified.

## Check docker daemon logging

Docker containers also retain log files. The `docker-compose.yml` should have
logging configured so that log files don't grow too large. If this isn't
working, or if you are using an older version of OE (introduced v2.5.3.3), then
ensure that the docker daemon is configured with reasonable constraints upon
logging. It is recommended to use 'local' logging for docker. Instructions for
configuring the docker daemon with this form of logging are located
[here](https://docs.docker.com/config/containers/logging/local/)

Ensure you:

- restart the docker service after configuring (`sudo system docker restart`)
- recreate the docker containers by bringing them down and then back up again
  (in the same directory as the installer: `sudo docker-compose down`,
  `sudo docker-compose up -d`)

A suggested configuration file is below, which will keep log files below 1GB.
This should be added to, or created at `/var/lib/docker/daemon.json`.

```
{
  "log-driver": "local",
  "log-opts": {
    "max-size": "20m",
    "max-file": "50"
  }
}
```

# Infohighway

If the connection to the infohighway appears to not be working, use the
following steps to ensure that you can bring it back up again

## Check that the connection is possible from the server

The following instructions will require the user have ssh access to the server
running OE

1. run `telnet xxx.xxx.xxx.xxx 443`, with the ip address of the infohighway
1. if the connection can't connect, the server cannot reach the infohighway and
   the network will need to be configured to allow the traffic. Contact a
   network administrator. If the connection succeeds, proceed to the next point.

## Check that the connection info exists, and is correct

The following instructions will use the frontend of OE and will require the user
have admin privileges

1. login to OE
1. navigate to admin > External connections
1. if there is no Entry for infohighway, click `Add`. otherwise click `Edit`
1. ensure that connection type is: `Info Highway`
1. ensure that Authentication Type is: `Basic`
1. ensure the username and password are correct
1. ensure the endpoint URL is correct
   (`https://infohighway.govmu.org/ih-webservice/soap/query`)
1. save the values and try to search for a patient by National ID. If this
   fails, proceed to the next point

## Check that extra_hosts has an entry for infohighway

The following instructions will use files on the machine running ubuntu and will
require the user be able to ssh into the server and has sudo privileges

1. ssh into the server having issues
1. check that the file `/var/lib/config/openelis-global/EXTERNAL_HOSTS` exists
1. check the contents of the file
   `/var/lib/openelis-global/config/EXTERNAL_HOSTS` has an entry that matches
   the ip address of the infohighway (`infohighway.govmu.org:xxx.xxx.xxx.xxx`)
1. rerun the latest installer by running setup_OpenELIS.py in the installer
   directory (usually located in `~/OpenELIS-Global_2.x.x.x_Installer/`)
1. check that the `docker-compose.yml` in the installer directory has an entry
   `oe.openelis.org`, and that under that property there is an `extra_hosts`
   property which contains the entry in
   `/var/lib/openelis-global/config/EXTERNAL_HOSTS`
1. if none exists, add the entry manually, making sure to keep the right
   indentation on the file. and run `sudo docker-compose up -d`
1. search for a patient by National ID. If this fails, contact a higher level of
   support
