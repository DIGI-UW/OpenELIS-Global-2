# Upgrading OpenELIS 2.x

## Special 2.0 to 2.3 Instructions

If you have an OpenELIS Global 2.2 or earlier (but not 1.x) you will need to
create a client facing keystore. The easiest way to do this is to simple copy
the keystore.

`cp /etc/openelis-global/keystore /etc/openelis-global/client_facing_keystore`

After this, you can upgrade as normal.

##Upgrades

OpenELIS Global uses an "upgrade in place" mechanism that saves all the already
entered data, as well as custom tests, system settings, etc.

The default action of the installer is to install/upgrade. If the installer can
find an existing version of OpenELIS, it runs the upgrade, if not, it installs.

[Download latest installer package Here:](https://www.dropbox.com/sh/47lagjht4ynpcg8/AABORyLmkpVTtRReeD6wSnJra?dl=0)

OpenELIS uses a versioning system where for OE version 2.6 for example, 2.6.1.x
is the Alpha release, 2.6.2.x is the Beta, and 2.6.3.1 is the first production
ready release. If you are using a production system, be sure to use a production
tested release which will have a 3 in the third place x.x.3.x

So, to upgrade, one must download the new installer, EG for the RC of OpenELIS
2.3

`curl -L -O https://www.dropbox.com/s/h2t20m6kzfj519y/OpenELIS-Global_2.3.2.5_Installer.tar.gz`

Then unpack the file

`tar -xvf OpenELIS-Global_2.3.2.5_Installer.tar.gz`

and finally, run the isntaller to upgrade

`sudo python2 setup_OpenELIS.py`
