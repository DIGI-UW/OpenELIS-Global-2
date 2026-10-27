# Add-ons

An add-on adds an optional feature to OpenELIS without changing OpenELIS itself:
a laboratory installs it, turns it on, configures it, and can turn it off again.
Examples: a reminder service that texts patients through a local messaging
provider, a clinical decision-support panel next to results, a connector to a
national system.

An add-on is a separate service (a container) plus a package that tells OpenELIS
what the service is allowed to see and where it appears in the UI. OpenELIS
never loads add-on code into its own JVM, and add-on UI never runs in the
OpenELIS page.

This document describes the mechanism. The backend (registry, events, tokens,
add-on API) is in the first pull request; the UI slots and the Extensions
administration page follow in a second one (see [Delivery](#delivery)).

## Why separate services, not plugins

OpenELIS used to load analyzer plugins (jars in `/plugin`) into the webapp. That
mechanism was removed in favour of the
[Analyzer Bridge](https://github.com/DIGI-UW/openelis-analyzer-bridge), a
separate service that talks to OpenELIS over HTTP:

- [#4138](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4138) removed the
  plugin routing and configuration,
- [#4506](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4506) removed the
  plugin API and its shims ("Nothing in OpenELIS loads such a jar"),
- [#4511](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4511) exported and
  dropped the old plugin configuration tables.

Add-ons follow the same direction. A service outside the JVM:

- cannot crash, slow down or leak memory into OpenELIS,
- cannot reach the database, the session or internal services; it sees only the
  API and the events it was granted,
- can be written in any language and released on its own schedule,
- is removed by removing it, with nothing left on the OpenELIS classpath.

The cost is that the operator runs one more container per add-on. OpenELIS does
not start containers (it has no access to the Docker or Podman socket, and
should not), so installing has two steps: register the package in OpenELIS, then
add the service to docker-compose. The install response gives the compose
snippet.

## The model: OpenMRS 3 extensions

The UI part follows the extension system of the OpenMRS 3 frontend, which many
OpenELIS implementers already know:

- concepts: <https://o3-docs.openmrs.org/docs/extension-system>
- `ExtensionDefinition` (name, slot, order, meta):
  <https://github.com/openmrs/openmrs-esm-core/blob/main/packages/framework/esm-globals/src/types.ts>
- `ExtensionSlot` and its `state`:
  <https://github.com/openmrs/openmrs-esm-core/blob/main/packages/framework/esm-react-utils/src/ExtensionSlot.tsx>

What is taken: a page declares named slots (`kebab-case-slot`); an add-on
contributes extensions with `name`, `slot`, `order` and `meta`; the page passes
a `state` object to the slot; an empty slot renders nothing.

What is different: in OpenMRS an extension is a JavaScript module that runs in
the same page as the application. Here an extension is a page from the add-on's
package, shown in a sandboxed iframe. A module in the same page can read the
session, the DOM and local storage, so OpenELIS could not promise that an add-on
only sees what it was granted. The iframe can.

## Package

A package is a zip:

| File             | Content                                                                   |
| ---------------- | ------------------------------------------------------------------------- |
| `addon.json`     | the manifest                                                              |
| `ui/…`           | optional pages, scripts, styles and images for the add-on's UI extensions |
| `SIGNATURE.json` | optional: `{"alg":"ed25519","publisher":…,"keyId":…,"signature":…}`       |

### What protects the laboratory

Three things, none of which depends on a signature:

1. **Consent.** A package is registered turned off. An administrator sees the
   permissions it asks for (scopes, events, slots) and turns it on. An upgrade
   that asks for more is turned off again until reviewed.
2. **The sandbox.** The add-on runs outside OpenELIS. It reaches OpenELIS only
   through the add-on API, from the internal network, with a credential limited
   to its scopes and to specific orders. Its UI runs in an iframe with an opaque
   origin and gets context only by `postMessage`. Network policy for the
   container (for example no internet access) is the operator's, in compose.
3. **Off means off.** Turning an add-on off stops its tokens, its key, its
   events and its UI at once.

### Signature: who published it

A package may be signed. The signature only says who published it; it does not
decide whether the package can be installed. Any valid package can be installed,
signed or not.

The signature is an Ed25519 signature over the `sha256sum` listing of every
other file, sorted by name. The manifest pins the container image by digest, so
the signature covers the code that will run too. OpenELIS knows a publisher's
key from `addons.trustedPublishers` (empty by default), and records one of three
results, shown on the permission screen and in the Extensions list:

| Result       | When                                                                                                                                                         | Shown as                                             |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------- |
| `VERIFIED`   | signed with the registered key of a trusted publisher                                                                                                        | "Verified publisher: _name_"                         |
| `UNVERIFIED` | not signed, or signed by a publisher this server has no key for                                                                                              | "Unverified publisher"                               |
| `MISMATCH`   | a signature is present but does not match the files, or names a trusted publisher with another key, or the manifest claims another publisher than the signer | "Signature does not match the contents", prominently |

Only `VERIFIED` fills `verifiedPublisher`. In every other case the name in
`addon.json` is a claim, and the UI must not present it as verified. A
`MISMATCH` package can still be installed: the administrator decides, with the
warning in front of them.

A verified add-on replaced by a package that is not verified for the same
publisher is turned off until reviewed, so its badge cannot be kept by someone
else's package.

`pack.sh` in [`docs/examples/addons/hello`](../examples/addons/hello/README.md)
builds a package, and signs it in one command for publishers who want the
verified badge (openssl only).

### Manifest (`schemaVersion` 1)

```json
{
  "schemaVersion": 1,
  "id": "hello",
  "name": "Hello add-on",
  "version": "1.0.0",
  "publisher": "example",
  "description": "…",
  "image": { "ref": "ghcr.io/example/hello@sha256:<64 hex>" },
  "runtime": { "port": 8080, "eventsPath": "/events" },
  "scopes": ["orders.read", "results.read"],
  "events": ["result.validated"],
  "extensions": [
    {
      "name": "hello-lookup-panel",
      "slot": "result-lookup-side-panel-slot",
      "page": "panel.html",
      "order": 10,
      "meta": { "title": "Hello", "height": 220 }
    }
  ],
  "config": [
    {
      "key": "greeting",
      "type": "string",
      "label": "Greeting",
      "default": "Hello"
    }
  ]
}
```

Checks: `id` is lowercase (it becomes the compose service name), the version is
semver, the image is pinned by digest, every scope, event and slot is known,
every event's scope is requested, every extension page is in the package, UI
files have an allowed type, config keys are unique and typed (`string`, `number`
with `min`/`max`, `boolean`, `select` with `options`).

The scopes, events and slots are defined once, in `AddonCatalog`. The
administration page reads them from `GET /rest/addons/catalog`.

## Permissions

| Scope          | Allows                                                                 |
| -------------- | ---------------------------------------------------------------------- |
| `orders.read`  | lab number, received date and test count of orders in its context      |
| `results.read` | the results of those orders                                            |
| `patient.read` | sex and date of birth of the patient of those orders; no name, no ids  |
| `ai.context`   | the de-identified context of those orders for decision support (below) |

"In its context" means: for a UI token, the orders on the user's screen; for the
service key, the orders the add-on was sent an event about, during the event
retention period (7 days by default). An add-on never gets a general query over
all orders.

### `ai.context`: de-identified context for assistants

An add-on that sends data to a language model or another outside service should
not receive identifiers at all. With `ai.context` it gets, for the orders of a
UI token:

- `GET /rest/addon-api/v1/ai/context?view=cases|patient`: for each order (or, in
  the `patient` view, for the newest day of one patient's orders) the patient's
  sex and age in years (90 and over grouped), and each result's test code,
  LOINC, name, value, unit, reference range and status; earlier results of the
  same tests (3 per test in the `patient` view, 1 per case) with the number of
  days before. `user` is the OpenELIS user of the token (for per-user limits)
  and `subject` a pseudonym of the patient: stable for this add-on, not
  reversible, different for another add-on.
- `POST /rest/addon-api/v1/ai/deidentify` `{"text": …}`: the text without the
  identifiers of the patients of those orders (names in any order, with or
  without accents, national and other ids, phone numbers, address, e-mail, lab
  numbers) and without identifier-like patterns (lab numbers, phone numbers,
  e-mail addresses, absolute dates, long digit runs). Clinical numbers are kept.

Nothing returned contains a name, a patient id, a lab number or a date. Free
text inside results (test names, text results) goes through the same scrubber.
Only UI tokens can use it (a service key has no screen). When the add-on has
`ai.context` but not `orders.read`, the lab numbers inside its UI token are
sealed (AES-GCM under a key derived from the token key): the frame and the
service hold the token but cannot read them, and `whoami` does not list them.

## Network access

A manifest may declare the hosts outside OpenELIS its service connects to:

```json
"network": { "egress": ["openrouter.ai", "api.openai.com:443"] }
```

Lowercase DNS names with at least two labels, optionally `:port`; no IP address
and no wildcard, at most 10. The list is shown on the permission screen ("Where
it connects outside OpenELIS"), an upgrade that adds a host is turned off until
reviewed, and the compose snippet carries it as a comment. OpenELIS does not run
containers, so enforcing the list is the operator's: for example put the add-on
on an internal network and give it `HTTPS_PROXY` pointing at an allowlisting
forward proxy. An add-on without the field needs nothing outside OpenELIS.

## Secret configuration values

A configuration field of type `secret` (an API key, a token) is never sent back
to the browser: the Extensions page only shows whether it is set, sends it only
when the administrator types a new value (an empty value clears it), and stores
it apart from the plain configuration, encrypted with `EncryptionConverter`
(`addon.secret_config`, Liquibase `025-addon-secret-config.xml`). The add-on's
service reads it with its key through `GET /rest/addon-api/v1/config`. A secret
cannot have a default.

## Install, enable, disable

`POST /rest/addons` (multipart, field `package`, administrators only):

1. reads the zip in memory with limits (10 MB package, 20 MB inflated, no `..`
   or absolute paths; nothing is written to disk),
2. validates the manifest,
3. records the provenance (verified, unverified or mismatch, see above),
4. registers the add-on **disabled**, stores its UI files, and returns once: the
   service API key, the webhook secret and a docker-compose snippet.

The operator adds the snippet to the compose file and starts the service. The
administrator reviews the permissions and turns the add-on on.

Upgrading is installing a newer package with the same id. Credentials and
configuration are kept. The add-on is turned off until an administrator turns it
on again if the new version asks for more scopes or events, or if its provenance
is weaker (a verified add-on replaced by an unverified or mismatched package, or
by another publisher's).

Turning an add-on off takes effect immediately, because every check reads the
registry: its UI extensions disappear, its UI files return 404, no token is
issued, existing tokens and the API key are refused, queued deliveries are
dropped and no new event is queued for it. The container keeps running until the
operator stops it, but it can no longer get anything from OpenELIS.

Secrets: the webhook secret is encrypted at rest with the existing
`EncryptionConverter`; the API key is stored as its SHA-256 only;
`POST /rest/addons/{id}/credentials` issues new ones.

### Secrets at rest

Every add-on secret goes through `EncryptionConverter`, that is, the general
`TextEncryptor` bean every other database secret uses (external connection and
delivery channel passwords, site settings marked encrypted). Add-ons add no key
and no cipher of their own, so when that bean changes key the add-on secrets
follow it. The encrypted add-on columns are:

| Column                         | Holds                                   |
| ------------------------------ | --------------------------------------- |
| `clinlims.addon.event_secret`  | webhook signing secret                  |
| `clinlims.addon.secret_config` | values of `secret` configuration fields |
| `clinlims.addon_secret.value`  | server-side keys (UI token signing key) |

A job that re-encrypts database secrets column by column must list these three.
`AddonEncryptedColumnsTest` fails when an encrypted add-on column is added or
removed, so the list cannot drift silently.

## Events

| Type               | When                                   | Data                                        |
| ------------------ | -------------------------------------- | ------------------------------------------- |
| `order.created`    | a sample row is inserted               | `accessionNumber`, `sampleId`               |
| `result.validated` | an analysis moves to Finalized         | `accessionNumber`, `analysisId`, `testCode` |
| `report.printed`   | the Report Print Queue records a print | `accessionNumber`, `reportType`, `reprint`  |

Events come from database triggers (Liquibase `024-addons.xml`) writing to an
outbox table in the same transaction as the change. Every write path (screens,
REST, FHIR, analyzer import) therefore emits the event, and a rolled-back change
emits nothing. A trigger writes only if an enabled add-on subscribes to that
type, so a laboratory without add-ons pays one indexed lookup per row. Event
data carries codes only; the add-on reads details through the API, under its
scopes.

The dispatcher (`AddonEventDispatcher`, every 2 s) fans each new event out to
the subscribed add-ons, then claims due deliveries with a lease
(`FOR UPDATE SKIP LOCKED`, like the Result Delivery outbox), and POSTs:

- body: a [CloudEvents 1.0](https://cloudevents.io/) JSON event
  (`application/cloudevents+json`),
- headers: `webhook-id`, `webhook-timestamp`, `webhook-signature` in the
  [Standard Webhooks](https://www.standardwebhooks.com/) format, so existing
  libraries verify them.

A 2xx answer is a delivery. Anything else is retried after 5 s, doubling up to
one hour, and given up after 10 attempts. Delivery is at least once: add-ons
de-duplicate on `webhook-id` and reject timestamps older than 5 minutes. Events
are kept 7 days (`addons.events.retentionDays`).

## UI slots

| Slot                                | Where                                                                    | `state`                                                 |
| ----------------------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------- |
| `result-validation-side-panel-slot` | right column of result validation                                        | `page`, `accessionNumbers` (rows on screen, at most 50) |
| `result-lookup-side-panel-slot`     | patient results page (`/PatientResults`)                                 | `page`, `patientId` (see below)                         |
| `app-menu-slot`                     | "Extensions" section of the main menu, opening `/extensions/{id}/{name}` | `page`                                                  |
| `top-nav-actions-slot`              | a button in the top bar opening a panel on the right                     | `page` (the screen: first route segment, kebab-case)    |

The patient results page shows one patient and has no list of lab numbers. It
passes `patientId`; when issuing the token the server replaces it with that
patient's lab numbers (newest first, at most 50) and drops the id, so the add-on
never receives the patient id and the page cannot add lab numbers of its own.

A slot with no extension renders nothing: pages look exactly as before for a
laboratory without add-ons.

An extension's `meta.title` is a text or a map by locale
(`{"en": "AI assistant", "vi": "Trợ lý AI"}`); the UI shows the user's locale,
then the language, then English.

`top-nav-actions-slot` extensions are header buttons (`meta.title`, and
`meta.icon` one of `application`, `magic-wand`, `chat`, `help`, `idea`, `bot`).
The panel they open is told only which screen is open, never the data on it:
add-ons that need the orders on screen use the side-panel slots of those pages.

The host page renders each extension as
`<iframe sandbox="allow-scripts allow-forms">` (no `allow-same-origin`) with
`src=/rest/addons/{id}/ui/{page}`. The files are served with
`Content-Security-Policy: sandbox allow-scripts allow-forms`, so the document
has an opaque origin even if opened directly: it cannot read OpenELIS cookies or
storage, and requests it makes carry no session.

Context goes in only through `postMessage`. The frame sends `ready`; the host
asks `POST /rest/addons/{id}/token` with the slot and the page state; the server
filters the state by the add-on's scopes (only `page` and, with `orders.read`,
up to 50 lab numbers; everything else, such as patient names, is dropped) and
returns a token; the host posts
`{type: "context", slot, extension, context, token, expiresIn, scopes, locale, apiBase}`
to that frame's `contentWindow` only. The frame may send `resize` and
`token-refresh`. The host accepts messages only from the frame's own window.

Each slot lists the roles that may get a token for it (validation needs
`VALIDATION` or `ADMIN`; the patient results page needs one of the roles that
open it), so an add-on cannot widen what a user can already see. Slots open to
every signed-in user (`app-menu-slot`, `top-nav-actions-slot`) never carry
orders: their tokens hold only the page, whatever the browser sends.

The Extensions page (Administration → Extensions) uploads a package, shows the
provenance line ("Verified publisher: _name_", "Unverified publisher" with the
name the package claims, or a prominent "Signature does not match the
contents"), lists what the add-on may read, what it is told about and where it
appears, and asks for that consent again in a dialog before turning it on. It
also edits the configuration form generated from the manifest, issues new
credentials and removes the add-on.

## Tokens and the add-on API

| Credential  | Who holds it                   | Lifetime      | Reads                                     |
| ----------- | ------------------------------ | ------------- | ----------------------------------------- |
| UI token    | one frame, for one user        | 10 minutes    | the lab numbers in the token              |
| service key | the add-on container (env var) | until rotated | orders the add-on was sent an event about |

A UI token is `oeat.<payload>.<HMAC-SHA256>` with a server key that never leaves
the database. Effective scopes are the token's scopes that the add-on still
holds, so removing a scope or disabling the add-on applies at once.

The add-on API is `/rest/addon-api/v1`: `whoami`, `config`,
`orders/{lab number}`, `orders/{lab number}/results`,
`orders/{lab number}/patient`. Each read needs its scope (403) and an order the
credential may read (403).

It is internal. Its own security filter chain accepts only bearer credentials
(no session, no CSRF, stateless), and only from `addons.api.allowedNetworks`
(default: private and loopback ranges, where compose services run) and only if
the request did not pass through a reverse proxy (any `Forwarded`,
`X-Forwarded-*` or `X-Real-IP` header is refused). The frame therefore never
calls it: it sends the token to its own service, which calls OpenELIS from
inside the compose network.

## Running an add-on (docker-compose)

The install response contains a snippet like:

```yaml
services:
  hello:
    image: ghcr.io/example/hello@sha256:…
    restart: unless-stopped
    read_only: true
    cap_drop: [ALL]
    security_opt: ["no-new-privileges:true"]
    user: "65534:65534"
    mem_limit: 256m
    environment:
      OPENELIS_API_URL: https://oe.openelis.org:8443/api/OpenELIS-Global/rest/addon-api/v1
      OPENELIS_API_KEY: oeak_…
      OPENELIS_WEBHOOK_SECRET: whsec_…
    networks:
      - default
```

OpenELIS sends events to `http://<id>:<port><eventsPath>`; the address can be
changed with `PUT /rest/addons/{id}/endpoint`. If the add-on's UI calls its own
service (`apiBase`), the reverse proxy needs one route per add-on, for example
in `nginx-prod.conf`:

```nginx
location /addons/hello/api/ {
    proxy_pass http://hello:8080/api/;
    proxy_set_header Cookie "";
    # answers streamed as server-sent events reach the frame as they come
    proxy_buffering off;
    proxy_read_timeout 180s;
}
```

The frame has an opaque origin, so the add-on's own API answers CORS (any
origin, no credentials). An add-on that streams (server-sent events read with
`fetch`) still works behind a buffering proxy: the answer then arrives at once
at the end. A non-streaming fallback (`?stream=0`, one JSON answer) is the
add-on's to offer.

Do not publish the backend's port 8443 to untrusted networks, or narrow
`addons.api.allowedNetworks` to the compose subnet (`172.20.1.0/24` in the
default compose file).

## Settings

In `common.properties` or `extra.properties`:

| Property                      | Default                     | Meaning                                                    |
| ----------------------------- | --------------------------- | ---------------------------------------------------------- |
| `addons.trustedPublishers`    | empty (nothing is verified) | `name=base64 Ed25519 key[,…]`, for the verified badge only |
| `addons.api.allowedNetworks`  | private and loopback ranges | who may call the add-on API                                |
| `addons.token.ttlSeconds`     | 600                         | UI token lifetime                                          |
| `addons.events.retentionDays` | 7                           | outbox retention                                           |
| `addons.dispatcher.enabled`   | true                        | turn event delivery off for the server                     |

## Delivery

1. **Backend** (this pull request): Liquibase changelog, registry, signature and
   manifest checks, outbox triggers, dispatcher, tokens, add-on API,
   administration REST endpoints, example add-on.
2. **UI**: `ExtensionSlot` and the sandboxed frame host with the `postMessage`
   context, the three slots, and the Extensions page, with English strings in
   `en.json` and a Playwright test that runs an add-on page in the sandbox.
3. **Core additions for assistants**: the `ai.context` scope (de-identified
   context and text), sealed lab numbers in tokens, the `top-nav-actions-slot`
   header slot, `network.egress` in the manifest and on the permission screen,
   and `secret` configuration fields. The AI assistant add-on itself
   (`projects/addons/assistant`) uses all of them and changes nothing in
   OpenELIS.

## Not included

- Starting or stopping containers from OpenELIS.
- Write scopes. Add-ons read; anything they want to change goes through a
  person.
- Enforcing network egress: the manifest declares it and the administrator
  consents to it; the operator's network policy enforces it.
- Per-extension display conditions and per-slot ordering configuration from
  OpenMRS 3; slots render all extensions of enabled add-ons by `order`.
