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

The default is an empty list. With no trusted proxies, requests behind the same
proxy share its client limit; account limits still apply separately. Direct
connections need no setting. A missing or malformed forwarded chain uses the
socket peer. Verify the proxy addresses after network changes and exercise two
distinct clients before enabling the setting on a live installation.

Counts are local to one application process and reset on restart. This is not a
distributed throttle.
