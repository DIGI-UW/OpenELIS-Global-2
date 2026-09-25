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

Production systems should run a release from a supported line. The supported
lines and the versioning rules are listed in
[RELEASES.md](https://github.com/DIGI-UW/OpenELIS-Global-2/blob/develop/RELEASES.md).

So, to upgrade, one must download the new installer, EG for the RC of OpenELIS
2.3

`curl -L -O https://www.dropbox.com/s/h2t20m6kzfj519y/OpenELIS-Global_2.3.2.5_Installer.tar.gz`

Then unpack the file

`tar -xvf OpenELIS-Global_2.3.2.5_Installer.tar.gz`

and finally, run the isntaller to upgrade

`sudo python2 setup_OpenELIS.py`
