# Login throttling behind a reverse proxy

Failed logins are counted by account and by the connection address. Forwarded
headers are ignored unless the immediate peer is explicitly trusted. This
prevents a client from choosing its own throttle identity.

Set `org.openelisglobal.login.throttle.trustedProxies` in `common.properties` to
the comma-separated IP addresses or CIDRs of the reverse proxies that reach OE2.
Include each proxy in a multi-proxy chain. Use dedicated proxy addresses or a
dedicated proxy-only network; do not trust the whole lab LAN or all private
networks. Each proxy must append its actual peer to `X-Forwarded-For`. OE2 reads
the chain from the nearest proxy back to the first untrusted address, including
private lab client addresses.

The published and image-build Compose files reserve `172.20.1.122` for Nginx.
The shipped properties trust only that address (`172.20.1.122/32`), so failed
logins from one client do not block other clients behind the bundled proxy.

For a custom network or an additional edge proxy, replace the list with those
proxies' dedicated addresses. The worktree development and local CI overlays use
Docker-assigned networks and therefore also need their own exact proxy addresses
when testing client isolation. An empty list ignores forwarded headers and
counts clients by socket peer; use it for direct connections only. A missing or
malformed forwarded chain uses the socket peer. Verify two distinct clients
after changing the network: exhaust failures from one, then confirm the other
can sign in, and confirm a forged header cannot evade the first limit.

Counts are local to one application process and reset on restart. This is not a
distributed throttle.
