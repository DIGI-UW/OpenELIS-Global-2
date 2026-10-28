# Install OpenELIS Global

The installation guide for OpenELIS Global 3.x is maintained on the OpenELIS
wiki, so there is one copy to keep tested and up to date:

**[Install OpenELIS Global](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/239632402/Install+OpenELIS+Global)**

It installs OpenELIS on a single Ubuntu Server LTS machine with Docker Compose,
and is written for the person who sets up and looks after the lab's server. It
covers:

1. [Prepare the server](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1711472648):
   Ubuntu Server and Docker.
2. Get OpenELIS, one of:
   - [Online install](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1712160806):
     the server downloads the current development build.
   - [Offline install](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1711636518):
     build a bundle of the latest release on another computer and copy it to
     the server.
3. [Configure and start OpenELIS](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1711407126):
   passwords, time zone, network ports, first start and first login.
4. [Set up an HTTPS certificate](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1712029723)
   (optional): your own certificate, or a free one from Let's Encrypt.

To look after a running server:

- [Back up and restore OpenELIS Global](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1711669276)
- [Upgrade or remove OpenELIS Global](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1711931398)
- [Troubleshoot an OpenELIS Global install](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1712128011)

A French translation is at
[Installer OpenELIS Global](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1701281856/Installer+OpenELIS+Global).

## Older installer package

Earlier versions of this page described an installer package with
`setup_OpenELIS.py` (`install/installerTemplate`), last published for 3.2.0.1.
It is no longer maintained and the wiki guide does not cover it. If your server
was installed with it, contact the OpenELIS team before upgrading.

## Developers

To run OpenELIS from source, see [Development setup](dev_setup.md).
