# Upgrading OpenELIS Global

Choose a supported release from [the release policy](../RELEASES.md) and follow
that release's [default distribution instructions](install.md). Online and
offline upgrades use the same configuration and pinned image set. Offline hosts
load the supplied image archive before starting Compose with
`--pull never --no-build`.

Before changing images, back up the database, configuration, installation keys,
certificate stores and Analyzer Bridge state together. Rehearse restoration on a
separate test instance using the distribution's
[backup and restore procedure](https://github.com/DIGI-UW/openelis-docker/blob/main/docs/backup-restore.md).

Retain the original effective encryption key when adopting an existing database.
The [credential upgrade guide](upgrades/private-install-credentials.md) covers
private credentials and first-login setup. Key conversion is a separate,
[one-time operation](upgrades/installation-encryption.md); ordinary image
updates must not generate a replacement key for existing encrypted values.

The retired Python installer is no longer built or shipped from this branch.
This does not modify any existing installation. Sites using historical packages
must plan and test their data migration before adopting the current
distribution. The current baseline is not a claim that every historical database
can upgrade in one step.

Historical [1.x to 2.x](upgrade1to2.md) and [2.7](27upgrade.md) material is
retained in Git history for migration research, not as current install
instructions.
