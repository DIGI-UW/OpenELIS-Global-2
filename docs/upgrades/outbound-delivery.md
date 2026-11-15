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
