# Login throttling behind a reverse proxy

Failed logins are counted by account and by the connection address. Forwarded
headers are ignored unless the immediate peer is explicitly trusted. This
prevents a client from choosing its own throttle identity.

## Attempt limits

Set these properties in the installation's mounted `common.properties`:

| Property                                              | Default | What it limits                                                  |
| ----------------------------------------------------- | ------- | --------------------------------------------------------------- |
| `org.openelisglobal.login.throttle.maxAttempts`       | `10`    | Failed sign-ins for one login name across all client addresses. |
| `org.openelisglobal.login.throttle.maxClientAttempts` | `100`   | Failed sign-ins from one client address across all login names. |
| `org.openelisglobal.login.throttle.windowMinutes`     | `15`    | The rolling window, in minutes, for both limits.                |

Either limit can refuse a sign-in before its password is checked. Attempts
currently checking a password also reserve space in both budgets, so concurrent
requests cannot bypass a limit. A successful sign-in clears that login name's
failures; it does not clear the client address's failures.

Changing `maxAttempts` does not change `maxClientAttempts`. For example, ten
failed attempts for one login name block further attempts for that name even
from a different address; one hundred failures spread across different names
block further attempts from that client address. Failures stop counting as they
age out of the window.

Workstations that reach OpenELIS with the same client address share the address
budget. Configure trusted proxies as described below so the bundled proxy does
not combine all clients into one budget. Where clients still share an address,
choose `maxClientAttempts` for that shared traffic separately from the per-login
limit. All three values must be positive integers.

Existing installations retain their mounted properties file. Add
`org.openelisglobal.login.throttle.maxClientAttempts=100` there to make the
existing default explicit, or set the intended address limit. Restart the
OpenELIS application after changing these settings. Omitting the property keeps
the default of 100; this update does not change that default.

## Client addresses behind proxies

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
