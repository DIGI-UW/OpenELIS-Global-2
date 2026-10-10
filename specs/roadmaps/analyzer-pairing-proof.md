# Analyzer pairing proof

Part of the [analyzer roadmap](../analyzers/roadmap.md), item 7. Repositories:
the Analyzer Bridge first, then OpenELIS.

Goal: the pairing code never crosses the wire, and each side proves it from its
own view of the TLS handshake, so something that intercepts the first pairing
cannot pair in the Bridge's place.

### Facts

- Today OpenELIS trusts whatever certificate answers at the Bridge address
  while pairing (`AnalyzerBridgePairingServiceImpl.pair`, lines 87 to 97, with
  `BridgeTls.ObservingServerTrustManager`), sends the raw code over that
  connection (`requestPairing`), and pins the `bridgeCertificateSha256` the
  answer names, refusing it only when it differs from the certificate observed
  (`analyzer.bridgePairing.error.certificateMismatch`).
- Something in the middle can relay the code to the real Bridge, present its
  own certificate to OpenELIS and name that certificate in the answer;
  OpenELIS then pins it for good. The Bridge pins the
  `serverCertificateSha256` OpenELIS sends (`PairingController`, line 54), so
  the same party can rewrite that too and read deliveries.
- The Bridge sees OpenELIS's client certificate in its own handshake
  (`PairedPeerFilter.clientCertificate`, `PairingController` line 49) and
  knows its own fingerprint (`bridge.fingerprint()`).
- A generated code is 20 characters from `SecureRandom`
  (`PairingState`, line 134), so a captured proof cannot be brute-forced
  offline. A configured `BRIDGE_PAIRING_CODE` can be anything.
- `PairingState` already refuses a wrong code (`WRONG_CODE`) and any code
  once paired (`CLOSED`).
- `docs/analyzers/bridge-pairing.md` tells operators to pair where nothing can
  sit between the two, because the first pairing is not yet bound to the
  certificates.

### Build

```
- [ ] P1 Bridge: PairingController.pair accepts proof = HMAC-SHA256(code, "oe-pair-v1" ‖ oeClientCertSha256 ‖ oeServerCertSha256 ‖ nonce) with oeServerCertSha256 and a random nonce, in place of the code. It recomputes the proof with the client certificate from its own handshake, its own fingerprint, and the request's oeServerCertSha256 and nonce, and compares in constant time; a mismatch takes the existing WRONG_CODE and CLOSED rules
- [ ] P2 Bridge: on success it answers with bridgeProof = HMAC-SHA256(code, "bridge-pair-v1" ‖ the same fields) beside bridgeCertificateSha256. There is no fallback to the raw code
- [ ] P3 Bridge: a configured BRIDGE_PAIRING_CODE shorter than the strength the proof needs (about 16 random characters) stops startup with a clear message, unless a PAKE (SPAKE2 or CPace) replaces the HMAC for short codes
- [ ] P4 Bridge tests: a proof over another client certificate is refused; over another server certificate is refused; the right proof pairs; the answer's proof verifies
- [ ] P5 Bridge release
- [ ] P6 OpenELIS: requestPairing sends the proof, oeServerCertSha256 and the nonce instead of the code, with observedBridgeCertSha256 from ObservingServerTrustManager; it verifies bridgeProof against its own view before saving the pin, and a mismatch is certificateMismatch
- [ ] P7 OpenELIS: the tools/openelis-analyzer-bridge pin moves to the release in the same PR as P6
- [ ] P8 OpenELIS test: a bridgeProof over another Bridge fingerprint is refused and nothing is saved
- [ ] P9 Integration: a relaying proxy with its own certificate between OpenELIS and the Bridge makes pairing fail on both sides
- [ ] P10 bridge-pairing.md and the spec's rule 9 say the code is proved, not sent; the "pair where nothing can sit between" paragraph goes
```

### Done when

1. A relay with its own certificate cannot complete a pairing. (P9)
2. The code is not in any pairing request or answer. (P4, P6)
3. Existing pairings keep working: their pins are already stored, and the
   proof only changes the pairing request.
