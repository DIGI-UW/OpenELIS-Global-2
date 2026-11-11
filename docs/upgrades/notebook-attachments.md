# Notebook attachments

Notebook templates and entries accept JPEG, PNG, PDF and UTF-8 plain-text files
up to 10 MB each. The notebook save service validates the complete submitted
file list before changing notebook content, files or pages. Invalid uploads
return HTTP 400; the existing notebook remains intact.

Previews open typed files without access to the originating window. Text that
contains markup remains plain text. Unsupported historical files remain stored
and show an unavailable-preview message. An unchanged historical file can be
retained while editing its owning notebook; replacing it requires a supported
file. No database migration is required.

The obsolete HTTP database-backup receiver and its Windows/Haiti upload scripts
have been removed. Sites that installed copies must remove the CGI endpoint and
replace their upload destination. Existing remote backups are not removed by
this source change. The Linux helper remains an input to the legacy native
installer; this change does not endorse its FTP transport.
