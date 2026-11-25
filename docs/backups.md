# Backups and restoration

Select the installed version from the
[default distribution tags](https://github.com/DIGI-UW/openelis-docker/tags).
Open `docs/backup-restore.md` in that tag to find the backup and restore
commands for your installation.

Back up the database, configuration, installation encryption key, certificate
stores and Analyzer Bridge state together. A database backup without the key
that encrypted its connection secrets is incomplete. Keep backups private and
test restoration on a separate instance before an upgrade.

The retired Python installer and its backup template are no longer shipped.
Removing their source does not remove scripts already installed on a server.
Operators must identify their current backup job, retention policy and offsite
copy before replacing it. The current distribution instructions do not imply
that automated offsite backups have been configured.
