# Optional Review integration

OpenELIS provides two empty Nginx include points for the separately deployed
[Review tooling](https://github.com/DIGI-UW/openelis-review-tooling). With no
active Review files, the proxy serves the ordinary application: no widget
injection, Review submission route, or contact with an external service.

These hooks are in the development, production and Linux installer proxy
configurations. The development configuration applies them to both HTTPS server
blocks. HTTP redirects, application API routes and frontend routing retain their
existing behavior.

## Directory contract

The proxy mounts `REVIEW_CONFIG_DIR` read-only at `/etc/nginx/review`. It
defaults to `./volume/review` for Compose checkouts and
`/var/lib/openelis-global/review` for the installer. Generated contents are
deployment state and stay outside Git.

| Container path                           | Nginx context         | Review tooling supplies                         |
| ---------------------------------------- | --------------------- | ----------------------------------------------- |
| `/etc/nginx/review/active/server/*.conf` | HTTPS server          | Same-origin submission location                 |
| `/etc/nginx/review/active/html/*.conf`   | Frontend `location /` | Widget injection and upstream response encoding |

An empty directory, or an absent `active` entry, is the disabled state. Review
tooling can switch a relative `active` symlink between versioned directories
inside the mounted directory. Mount the parent directory, not individual files,
so switches are visible inside an already running proxy.

Only an operator may write these files. They are executable Nginx configuration;
Grist content and request data must never supply them. Scope, suggestions and
review instructions remain in Grist. OpenELIS carries no Review hostname,
authoring credential, widget code or checklist copy.

## Activation and validation

Installing these hooks on an older deployment requires the normal one-time proxy
configuration and mount update. Once installed, changing the active Review files
needs only `nginx -t` followed by `nginx -s reload` in the existing proxy. Do
not restart the application, frontend, database or FHIR services to toggle
Review. Nginx supports wildcard
[includes](https://nginx.org/en/docs/ngx_core_module.html#include) and
[configuration reloads](https://nginx.org/en/docs/control.html).

The enable/disable command, rollback and public verification belong to Review
tooling. This OpenELIS change provides the hooks only; it does not claim that
the complete installation command or any public migration is delivered.

Run the isolated proxy checks with Docker and OpenSSL available:

```sh
python3 -m unittest discover -s .github/scripts -p test_review_proxy.py -v
```

The tests start Nginx and a local application fixture, using the actual three
proxy configurations and generated certificates. They compare disabled page,
asset, API and submission-path responses with the same proxy without the hooks;
then test repeated enable/disable reloads, one script injection, unaffected
application routes and invalid configuration rejection. Container identities and
start times must remain unchanged across reloads. These checks validate the
proxy boundary, not the widget's UI, real session authorization or human UAT.
