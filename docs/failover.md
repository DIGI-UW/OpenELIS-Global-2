# Failover setup

There are certain situations, particularly in reference labs where the LIS needs
to be able to have a high availability failover setup. This kind of setup can
minimize downtime in case of any issues, and ensure that a backup is always
ready to go in case it’s needed. Because the goal is to ensure as close to 100%
uptime as possible, please be sure each OpenELIS instance is hosted on a
different physical server. The best practice is to have the server on the same
network, but in a different physical location if possible.

## Prerequisites

You will need the following in order to successfully setup your failover.

- 2 servers capable of running OpenELIS
  - Primary server
  - Secondary server
- Visibility between 2 servers
- Everything required for regular OE setup

## Instructions

1. Setup OpenELIS on the primary server as per
   [normal install/upgrade procedures](/install) ensuring the db is listening on
   port 5432
2. Setup OpenELIS on the secondary server as per
   [normal install/upgrade procedures](/install) ensuring the db is listening on
   port 5432
3. Run `scripts/configurePrimary.sh` on the Primary server
   1. Enter the ip address/dns entry that the primary server sits at. The
      secondary server must be able to reach the primary server at this address
      on port 5432
   2. Enter the ip address/dns entry that the secondary server sits at. The
      primary server must be able to reach the secondary server at this address
      on port 22 for ssh
   3. Enter a user that has sudo permissions on the secondary server
   4. Enter the password for the user on the secondary server
   5. Let the script run, it may look like further passwords are prompted for,
      but they should be automatically entered by the script
4. Ensure that OpenELIS is started on the Primary server
5. Ensure that information that is entered into the db on the Primary appears in
   the db on the Secondary.

## Notes

OpenELIS and the other containers aside from the database will not be running on
the secondary server at the end of this workflow. This is intentional as the DB
is running in standby mode and will reject any updates other than from the
primary which disrupts OpenELIS from running.

The entire OpenLEIS install directory (/var/lib/openelis-global) is copied over
from the primary to the secondary. This will ensure the all config changes are
populated to the secondary AT THE TIME THIS WORKFLOW IS RUN. Later changes will
need to be done on both servers OR the workflow can be re-run to populate the
differences.

The etc directory (/etc/openelis-global) is NOT copied over, so ensure that
certs are well set up on both servers. It is recommended to use the same
keystore, and truststore on both servers, but to use a different
client_facing_keystore IF the original key/cert does not cover the secondary
server’s ip/DNS address.

# Executing Failover

## Prerequisites

- Minimum 2 servers capable of running OpenELIS
  - Original Primary server (target/new Secondary server or decommissioned)
  - Original Secondary server (target/new Primary server)
  - New Secondary Server (if Primary is decommissioned)
- Visibility between target Primary and target Secondary servers
- Everything required for regular OE setup

## Instructions

1. Confirm that the Original Primary server is down. Try restarting the Primary
   to see if that fixes the issue.
   1. cd /var/lib/openelsi-global
   2. sudo docker-compose restart
2. If that doesn’t work, but only some services are down, bring them all down by
   running 3. cd /var/lib/openelsi-global 4. sudo docker-compose down
3. Setup OpenELIS on the new secondary server as per normal install/upgrade
   procedures ensuring the db is listening on port 5432 IF using a new secondary
   server
4. Run `scripts/configurePrimary.sh` on the target primary server 5. Enter the
   ip address/dns entry that the target primary server sits at. The target
   secondary server must be able to reach the target primary server at this
   address on port 5432 6. Enter the ip address/dns entry that the target
   secondary server sits at. The target primary server must be able to reach the
   target secondary server at this address on port 22 for ssh 7. Enter a user
   that has sudo permissions on the target secondary server 8. Enter the
   password for the user on the target secondary server
5. Let the script run, it may look like further passwords are prompted for, but
   they should be automatically entered by the script
6. Ensure that OpenELIS is started on the new Primary server
7. Ensure that information that is entered into the db on the new Primary
   appears in the db on the new Secondary.

## Notes

The same notes from failover setup also apply to this workflow

If a server capable of being the new secondary doesn’t exist yet, but one still
wants to promote an original Secondary server to be the new Primary server, run
the above instructions but leave out instructions 4b, 4c, 4d, and 7. Once a new
Secondary has been allocated, rerun the Failover setup workflow.
