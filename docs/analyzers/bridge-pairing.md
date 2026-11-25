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

### Bundled Bridge

The installer mounts a volume containing the Bridge's public certificate
read-only in OpenELIS. When both services have the same configured pairing code,
OpenELIS verifies the certificate during the HTTPS handshake and then sends the
code. If the code was generated on startup, a Global Admin enters just the code
on the Analyzers page. OpenELIS reads its trusted copy of the certificate for
either method. If the file is missing or unreadable, pairing waits.

### Remote Bridge

On the trusted Bridge host, read the full SHA-256 fingerprint from its startup
log. A Global Admin enters the fingerprint and the single-use code on the
Analyzers page. OpenELIS accepts plain hexadecimal or colon-separated byte
pairs. Obtain the fingerprint through the host console or an authenticated
management session. A fingerprint copied from the network address being paired
cannot establish that address's identity.

OpenELIS sends the code only after verifying the certificate. It also sends the
fingerprint of its own HTTPS certificate so the Bridge can verify result
delivery. Results received before pairing wait in the Bridge outbox.

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
OpenELIS reads the pairing from its database wherever it checks it, so the new
pairing applies to result delivery at once, without a restart.

A restart that preserves the Bridge certificate and pairing needs no action. If
the Bridge loses its pairing record but still presents the saved certificate, a
bundled install can pair again with a valid configured code.

If the Bridge certificate changes, OpenELIS keeps the previous pin and stops
automatic pairing. Confirm the replacement on the Bridge host and prepare a new
code. A Global Admin then pairs again on the Analyzers page. A bundled install
verifies against the refreshed public certificate volume. For a remote Bridge,
enter the new fingerprint from its host. An unreachable Bridge or a failed
verification leaves the saved pairing intact.

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
