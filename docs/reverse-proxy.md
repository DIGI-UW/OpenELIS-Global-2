## Run OpenELIS Global on Other Ports or Behind a Reverse Proxy

OpenELIS Global builds some links from the address the browser used: the
redirect to the login page, the page to return to after logging in, SAML and
OAuth callback addresses, and the links in FHIR responses. Those links keep a
non-standard port such as 8443 when OpenELIS Global is published on it, or when
a reverse proxy in front of it reports it.

### Publish OpenELIS Global on other ports

`docker-compose.yml` reads the host ports from these variables, which you can
set in `.env`:

| Variable               | Service             | Default |
| ---------------------- | ------------------- | ------- |
| `OE_HTTP_PORT`         | proxy, HTTP         | 80      |
| `OE_HTTPS_PORT`        | proxy, HTTPS        | 443     |
| `OE_WEBAPP_HTTP_PORT`  | webapp, direct      | 8080    |
| `OE_WEBAPP_HTTPS_PORT` | webapp, direct      | 8443    |
| `OE_FHIR_HTTP_PORT`    | FHIR server, direct | 8081    |
| `OE_FHIR_HTTPS_PORT`   | FHIR server, direct | 8444    |

To serve OpenELIS Global on 8080 and 8443, move the webapp off those ports
first:

```
OE_HTTP_PORT=8080
OE_HTTPS_PORT=8443
OE_WEBAPP_HTTP_PORT=18080
OE_WEBAPP_HTTPS_PORT=18443
```

Then open `https://<server>:8443/`; `http://<server>:8080/` redirects there.

### Put a reverse proxy in front

When another proxy (a hospital gateway, a load balancer, a second nginx)
forwards to OpenELIS Global, have it send either the standard `Forwarded` header
or the `X-Forwarded-*` headers:

```
proxy_set_header Host $host;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Port $server_port;
```

or `Forwarded: proto=https;host="lab.example.org:8443"`. A proxy that rewrites
`Host` to the OpenELIS server's name and sends the original in
`X-Forwarded-Host` (Apache's `ProxyPass` default) works too.

The OpenELIS Global proxy passes these headers to the webapp. The webapp reads
them only from connections on a loopback or private network address (10.0.0.0/8,
172.16.0.0/12, 192.168.0.0/16, 100.64.0.0/10, 169.254.0.0/16, 127.0.0.0/8 and
the IPv6 equivalents), where its proxy sits, and only to build links; they never
change who is logged in or what they may do. A forwarded host that is not a host
name is ignored. Audit records keep the address of the connection itself.
