package org.openelisglobal.security.certs.service;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TruststoreService {

    // Installing a CA cert into the JVM truststore is a privileged security
    // operation reachable from user input (ExternalConnectionController's cert
    // upload), so it is gated rather than exempted as infrastructure.
    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_MANAGE')")
    void addTrustedCert(String alias, Certificate certificate)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException;
}
