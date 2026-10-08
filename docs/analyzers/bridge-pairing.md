# Pairing OpenELIS with the Analyzer Bridge

OpenELIS and the Analyzer Bridge authenticate each other by certificate, not by
password. They pair once, and from then on each side accepts only the other's
certificate in both directions:

- OpenELIS calls the Bridge (connections, profiles, probes, undelivered results)
  presenting its own certificate, and trusts only the Bridge certificate it
  pinned.
- The Bridge delivers results to `/analyzer/fhir` presenting its own
  certificate. OpenELIS accepts a delivery only from the paired Bridge's
  certificate and records it as the system user. An OpenELIS password or session
  is refused there.

Neither side needs a certificate authority, and host names play no part: each
side trusts the other's certificate by its SHA-256 fingerprint.

## Pairing

The Bridge accepts a pairing only with its pairing code, and only once per code.
The code is either configured (`BRIDGE_PAIRING_CODE` on the Bridge) or generated
when an unpaired Bridge starts, in which case the Bridge prints it in its log.

OpenELIS pairs in one of two ways:

- **With a configured code.** When `ANALYZER_BRIDGE_PAIRING_CODE` (property
  `analyzer.bridge.pairing-code`) is set to the Bridge's code, OpenELIS pairs on
  startup, and on its first call to the Bridge if it is still unpaired. Giving
  both containers the same value pairs them with no manual step.
- **On the Analyzers page.** A Global Admin enters the code there. The page
  shows whether OpenELIS is paired, the Bridge address, the start of the
  Bridge's certificate fingerprint and when it paired, and why a code was
  refused.

While pairing, OpenELIS sends the code and the fingerprint of the HTTPS
certificate it serves, presents its own certificate, and pins the certificate
the Bridge presented once the Bridge confirms it. Results the Bridge receives
before pairing wait in its outbox and are delivered once it is paired.

## Identities

OpenELIS creates its own key pair the first time it pairs and keeps it in the
database, with the private key encrypted. It does not reuse the certificate it
serves HTTPS with, so renewing that certificate does not break the pairing.

The Bridge keeps its key pair and pairing record on its state volume. When it is
configured with a server keystore, it uses that instead.

The Bridge trusts OpenELIS's HTTPS certificate either by the fingerprint
OpenELIS sent while pairing or because its trust store validates it. In the
Compose stacks the Bridge is given the stack's trust store, so OpenELIS stays
trusted after its certificate is regenerated.

## Pairing again

A paired Bridge refuses its current code. To pair again, for example after
reinstalling either side, configure a new code on the Bridge, restart it, and
enter that code on the Analyzers page or set it as OpenELIS's configured code.

## Tomcat

OpenELIS's HTTPS connector requests a client certificate without requiring one,
and accepts any certificate in the handshake through
`org.openelisglobal.tomcat.AnyClientCertificateTrustManager`, which the OpenELIS
images install in Tomcat's `lib` directory. The handshake still proves the
client holds the certificate's private key; OpenELIS then admits only the paired
Bridge's certificate. Browsers and the proxy present no certificate and are
unaffected.

A server.xml that does not request client certificates, such as one installed by
the Linux installer template, leaves OpenELIS unable to receive results from a
paired Bridge.
