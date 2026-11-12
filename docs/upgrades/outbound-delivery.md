# Outbound email and FHIR delivery

New installations have no email sender or blind-copy recipient, and FHIR
subscriber destinations require HTTPS by default. Configure only destinations
that the laboratory has authorized to receive its data.

## Email

Configure the sender in the Email delivery channel or set
`org.openelisglobal.mail.from` for the notification sender and channel fallback.
The distribution leaves it blank. Email without a configured sender fails rather
than claiming successful delivery.

`org.openelisglobal.mail.bcc` is blank by default. Set it only when the
laboratory explicitly authorizes copying every applicable message to that
recipient. A notification with its own BCC list uses that list instead of the
global value.

The notification sender requires STARTTLS. The delivery-channel sender defaults
to STARTTLS and also supports implicit TLS. Both validate the SMTP certificate
chain and server hostname. Configure a trusted SMTP server certificate; do not
work around errors by disabling verification. An explicitly configured delivery
channel with security `NONE` still uses plaintext and is appropriate only for an
isolated test receiver. TLS protects the connection to the mail server; it does
not provide end-to-end encryption of the recipient's mailbox.

## FHIR subscriber

Set `org.openelisglobal.fhir.subscriber` to the complete HTTPS base URL, for
example `https://partner.example.org/fhir/`. Blank disables creation of new
subscription/export registrations. An invalid URL or an HTTP URL with the
default policy prevents application startup before any registration changes. The
application no longer silently rewrites a URL for just one delivery path.

`org.openelisglobal.fhir.subscriber.allowHTTP=false` is the shipped default. The
explicit `true` override is intended for isolated test environments without
clinical data. This setting does not change the local FHIR store's transport.

## Existing installations

Mounted property files are retained during upgrades; replacing an image does not
replace their values. Review the active configuration before restarting:

1. Remove the old `openelis@gmail.com` BCC and sender defaults. Configure the
   laboratory sender and only explicitly authorized recipients.
2. Set `org.openelisglobal.fhir.subscriber.allowHTTP=false` and provide the
   partner's actual HTTPS URL. Confirm the certificate is trusted and matches
   the host. Do not assume that changing the scheme makes a partner support TLS.
3. Check both FHIR subscriptions in the local FHIR store and scheduled export
   tasks. Older stored registrations are not erased merely by clearing a
   property. Retire obsolete destinations before treating an export as disabled.
4. Send a synthetic message and FHIR resource to controlled receivers. Confirm
   the exact destination, recipients, successful delivery, and visible failure
   when the receiver is unreachable or its certificate cannot be verified.

Use synthetic data for acceptance. These default changes do not establish that
all deployed sites have adopted the new configuration or that every integration
uses the subscriber mechanism.

## Separate credentials for partner FHIR servers

The local FHIR store uses only `org.openelisglobal.fhirstore.username` and
`org.openelisglobal.fhirstore.password`, plus the installation client
certificate. Other destinations no longer inherit that login or certificate. An
external server with no configured credentials receives an anonymous request.

For remote imports, facility synchronization, provider imports and scheduled
exports, configure each authenticated partner in a private mounted properties
file. Its URI must match the base URL already configured for that workflow.
Trailing slashes and default ports are normalized; different paths are different
destinations.

```properties
org.openelisglobal.fhir.partners=consolidated,registry

org.openelisglobal.fhir.partner.consolidated.uri=https://partner.example.org/fhir
org.openelisglobal.fhir.partner.consolidated.username=lab-account
org.openelisglobal.fhir.partner.consolidated.password=${PARTNER_FHIR_PASSWORD}

org.openelisglobal.fhir.partner.registry.uri=https://registry.example.org/fhir
org.openelisglobal.fhir.partner.registry.token=${REGISTRY_FHIR_TOKEN}
```

Provide those secrets through the deployment's existing private configuration
mechanism. Do not commit them. Each partner uses either username/password or a
Bearer token. Duplicate destinations, a partner matching the local store, and
mixed authentication settings are rejected at startup. Configuration changes
require a restart.

The existing client-registry username/password settings and facility-registry
token exchange remain explicit alternatives for their respective workflows. They
take precedence when those callers supply credentials; they do not fall back to
the local store login. Do not configure the same integration in both places.
Token exchange credentials are JSON encoded and sent only to the configured
HTTPS authentication endpoint.

All external FHIR endpoints require HTTPS. The existing
`subscriber.allowHTTP=true` override permits only the exact configured
subscriber and only anonymous requests for isolated testing. It does not permit
credentials over HTTP or enable plaintext for other partners.

Partner FHIR clients do not retain or send session cookies; identity comes from
the configured Basic or Bearer credentials. Cookie-based partner login is not
supported by this path.

FHIR clients do not follow HTTP redirects. Configure the final base URL.
Pagination must stay inside that same base URL; links to another origin or
another base path are refused before sending a request. Metadata discovery uses
the same partner identity and transport as the main request.

### Compatibility before upgrading

- A partner that previously accepted the internal store login now needs a
  dedicated account in the named-partner configuration. Without it, requests are
  anonymous and authenticated partners will reject them.
- Partners requiring a TLS client certificate will no longer receive the
  installation certificate. This change does not add partner-specific client
  certificates. Such integrations need an explicit certificate configuration
  before they can adopt this version; do not restore the shared identity.
- Redirecting endpoints and pagination through a different hostname or base path
  must be corrected at the partner or in the configured URL.
- Existing mounted property files are retained. Adding an image does not
  populate partner credentials or rotate credentials previously shared with a
  partner. Provision the separate partner account and rotate any exposed
  internal login.
- FHIR server-managed subscriptions deliver from the FHIR server, rather than
  this application's client. These new partner properties do not configure
  authentication for that separate delivery mechanism.

Validate with synthetic data: local store access, each configured partner,
pagination, refusal by a partner when credentials are absent or incorrect, and
certificate verification. A successful test deployment is not evidence that
existing production installations have adopted these settings.
