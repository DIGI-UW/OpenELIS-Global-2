# HTTPS for a development server

Use the same source-development command on localhost and a public server. The
development launcher owns Compose layering and certificate setup.

## Local development

Leave `LETSENCRYPT_DOMAIN=localhost` in `.env` and run:

```bash
scripts/dev-stack up
scripts/dev-stack url
```

The stack uses self-signed certificates and random loopback ports. Browser tests
accept the test certificate. No public DNS or certificate registration is
needed.

## Public development server

Set `LETSENCRYPT_DOMAIN` to the server's domain and `LETSENCRYPT_EMAIL` to the
certificate contact address in `.env`. Both the primary domain and
`bridge.<domain>` must resolve to this server. Ports 80 and 443 must be
reachable for the existing HTTP-01 certificate flow.

```bash
scripts/dev-stack doctor
scripts/dev-stack up
scripts/dev-stack url
scripts/dev-stack logs -f proxy
```

The launcher selects public ingress, renders the domain configuration, obtains
or renews certificates and reloads the worktree's proxy. Re-run `up` after
configuration changes. Do not call an alternate development Compose recipe or
certificate script against fixed global container names.

Port/bind overrides are documented in `.env.example` for servers with an
existing router. Certificates under `volume/letsencrypt` belong to this
checkout; preserve them when maintaining the server.

Published-image production deployment is a separate interface. Follow
[the setup guide](dev_setup.md#run-published-images) and the installer's
certificate configuration rather than using the source-development launcher.
