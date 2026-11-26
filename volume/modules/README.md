# Installed modules

Drop one module JAR per module into this directory and restart the webapp
container. The compose files mount it read-only at
`/var/lib/openelis-global/modules`, where the module runtime reads it at
start-up. A `<jar>.disabled` marker next to a JAR skips it; an optional
`modules.lock` lists allowed `id@version sha256` digests (strict mode).

See [docs/modules.md](../../docs/modules.md).
