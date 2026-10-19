# Hello: a minimal add-on

A working example of the [add-on mechanism](../../../addons/design.md):

- `service/server.py` (Python standard library) receives `result.validated`
  events, checks their Standard Webhooks signature, and reads the order's
  results through the add-on API with its service key. It also answers its UI
  panel on `/api/summary`, calling OpenELIS with the panel's user token.
- `ui/` is a panel for the patient results page. It runs in a sandboxed iframe
  and gets its context and token from OpenELIS by `postMessage`.
- `pack.sh` builds the package and, if you want the verified badge, signs it
  (openssl only).

## 1. Build and publish the image

```bash
docker build -t ghcr.io/<you>/openelis-addon-hello:1.0.0 service/
docker push ghcr.io/<you>/openelis-addon-hello:1.0.0
docker buildx imagetools inspect ghcr.io/<you>/openelis-addon-hello:1.0.0 | grep Digest
```

Put the image name and digest in `addon.json`:

```json
"image": { "ref": "ghcr.io/<you>/openelis-addon-hello@sha256:<digest>" }
```

OpenELIS refuses an image that is not pinned by digest. The digest in this
folder is a placeholder.

## 2. Build the package

```bash
./pack.sh pack hello.zip
```

That is enough to install it: OpenELIS shows it as "Unverified publisher".

To have it shown as "Verified publisher: example", sign it. Once, create a key
and give the printed line to the laboratories that should recognise you (they
add it to `common.properties` or `extra.properties`):

```bash
./pack.sh keygen example
# addons.trustedPublishers=example=<base64 key>
```

Keep `example.pem` secret. Then build and sign in one command:

```bash
./pack.sh pack hello.zip example
```

The signature covers every file. If anything changes after signing, OpenELIS
shows "Signature does not match the contents" and does not show the package as
verified.

## 3. Install

From the Extensions page, or:

```bash
curl -k -u 'admin:adminADMIN!' -F package=@hello.zip \
  https://localhost/api/OpenELIS-Global/rest/addons
```

The response has the add-on (registered **disabled**), the API key, the webhook
secret and a compose snippet. The key and secret are shown once; save them now
(`POST /rest/addons/hello/credentials` makes new ones).

## 4. Run the service

Add the snippet to `docker-compose.yml`, then `docker compose up -d hello`. For
the panel's calls to its own service, add a route to the proxy configuration:

```nginx
location /addons/hello/api/ {
    proxy_pass http://hello:8080/api/;
    proxy_set_header Cookie "";
}
```

The OpenELIS backend uses a self-signed certificate in the default compose
setup. Mount its CA (from the `certs` volume) and set `OPENELIS_CA_FILE`; the
example only skips verification, with a warning, when that variable is unset.

## 5. Turn it on

On the Extensions page, check the publisher line and the permissions
(`orders.read`, `results.read`) and turn the add-on on, or:

```bash
curl -k -u 'admin:adminADMIN!' -X PUT -H 'Content-Type: application/json' \
  -d '{"enabled":true}' https://localhost/api/OpenELIS-Global/rest/addons/hello/enabled
```

Validate a result: the service logs the order's result count. Turn the add-on
off and nothing more reaches it: no events, and its key is refused.
