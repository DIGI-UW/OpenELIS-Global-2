# Installing OpenELIS Global

## Default distribution

The supported online path is a released
[openelis-docker distribution](https://github.com/DIGI-UW/openelis-docker/releases).
Select a release that contains `deployment-manifest.json` and follow its tagged
README. It supplies the exact published image digests tested together by OE2,
including certificate initialization and the Analyzer Bridge. No application
build, host Java installation or manual keystore creation is required.

Prepare a Docker Engine host with Docker Compose, choose the release, and
configure its private database credentials and initial administrator password
file. From that distribution directory:

```sh
docker compose pull
docker compose up -d --no-build
docker compose ps
```

An initializer that finishes with `Exited (0)` is expected. Application services
must become ready; inspect service logs rather than treating container creation
as successful installation. Follow the tagged README for first login and public
HTTPS.

## Keys and existing installations

Fresh installation generates a private TLS identity and an independent key for
connection secrets stored in the database. Restart retains both. Public HTTPS
certificates, internal TLS identities and database-secret encryption have
separate lifecycles. Replacing a browser certificate must not replace the
encryption key.

An existing database needs its original effective encryption key. Follow the
[distribution adoption procedure](https://github.com/DIGI-UW/openelis-docker#installation-keys-and-existing-installations)
and [private credential upgrade guide](upgrades/private-install-credentials.md).
Do not delete volumes to trigger initialization. Changing the encryption key
uses the separate
[one-time conversion procedure](upgrades/installation-encryption.md). Back up
the database, configuration, security volumes and Bridge state together.

## Development, testing and historical installers

Source development uses [the development launcher](dev_setup.md). OE2's root
Compose files are build/test inputs and the testing-server deployment
definition; they are not a second lab installation guide. OE2's image publisher
is the sole owner of deployment to the shared testing server.

Historical offline packages remain governed by their original release
instructions. The old Python installer and manual certificate recipes are not
the default path. Offline delivery for manifest-based distributions is deferred.
Older releases without a deployment manifest must not be presented as
implementing this new path.

The initial manifest-enabled release must finish publication before operators
can use these instructions. Until then, this is the candidate delivery contract.
