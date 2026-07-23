# Dual-subdomain OpenELIS demo

Two isolated OpenELIS stacks on one host, split by subdomain through a single
lightweight umbrella reverse proxy, each with its own Let's Encrypt certificate.

| Subdomain | Stack | Branch | Jira |
|---|---|---|---|
| `amr.openelis-global.org` | Microbiology MVP | `feat/782-…-microbiology-mvp-…` | OGC-782 |
| `analyzers.openelis-global.org` | Analyzer Types & Mapping **+ analyzer harness** | `codex/ogc-1054-analyzer-qc-mvp` | OGC-1054 |

## Architecture

```
                         :80 / :443
                    ┌──────────────────┐
   Host header ───▶ │  oe-edge-router  │  (nginx; project -p oe-edge)
                    │  TLS x2 (LE)     │
                    └───────┬──────────┘
              amr.* ────────┤ Host-based dispatch over the "oe-edge" network
        analyzers.* ────────┤
             ┌──────────────┴───────────────┐
             ▼                               ▼
   amr stack (-p amr)              analyzers stack (-p analyzers)
   build.docker-compose.yml       harness chain (dev+base+analyzer-test)
   amr-oe / amr-frontend          analyzers-oe / analyzers-frontend
   no host ports                  + astm-simulator + bridge (internal only)
```

The two backend stacks bind **no host ports** — the router reaches them only by
Docker-network alias. Isolation is by Compose **project name** (`-p`), explicit
**`container_name`** overrides (project names don't namespace those), **remapped
subnets** (`amr`=172.24, `analyzers`=172.25; both bases pin 172.20), and
**per-instance harness image tags**.

## Files (all additive — no existing repo file is modified)

- `router/` — `nginx.conf.template` (Host dispatch), `docker-entrypoint.sh`
  (per-domain cert resolution + render), `Dockerfile`, `docker-compose.router.yml`.
- `amr/`, `analyzers/` — one thin `docker-compose.override.yml` each, layered
  **last** onto the respective base chain. Touch only `container_name`/`ports`/
  `networks`/`image`/`environment` — never `volumes`.
- `scripts/` — `generate-certs.sh` (issue both, reuses the repo's certbot
  pattern), `certbot-renew.sh` (renew + reload the router).
- `deploy.sh` — reproducible lifecycle. `.env.example` — config.

## Runbook

```bash
cp .env.example .env      # fill in domains, email, branches (host values pre-filled)

./deploy.sh configure     # install Docker/git, make /opt/oe-{edge,amr,analyzers}, renew cron
./deploy.sh deploy --yes  # build + bring up router + both stacks on self-signed (detached, ~20-40 min)
# → point DNS: amr AND analyzers A-records → the host EIP (both share one IP)
./deploy.sh certs         # issue Let's Encrypt for both once DNS resolves
./deploy.sh status        # instance + both HTTPS codes + container states
```

`configure` + `deploy` need no DNS — the stacks come up on self-signed and are
fully verifiable via Host-header curl. Only `certs` requires DNS (ACME HTTP-01).

## Gotchas (learned from this repo)

- **Overlay loads last.** Each override must be the final `-f`; a base file after
  it silently re-clobbers the `ports: []` / `container_name` overrides.
- **`proxy` is inherited but never started** in either stack (omitted from the
  `up` service list). The umbrella router owns 80/443.
- **Subnet remap is mandatory** — two projects can't both create a 172.20.1.0/24
  network. Static-IP pins are dropped (nothing outside the compose files
  references `172.20.1.121` / `172.21.1.100`; OE finds the DB by service name).
- **Renaming harness containers requires updating** `astm-simulator`'s
  `BRIDGE_CONTAINER_NAME` / `MOCK_CONTAINER_NAME` (docker.sock network manager).
- **Two separate certs**, not one multi-SAN — renewals/failures decoupled.
