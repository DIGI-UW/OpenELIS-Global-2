package org.openelisglobal.security.certs.service;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import org.openelisglobal.common.service.CrossDomainService;

@CrossDomainService(callers = "external connection security — internal infrastructure")
public interface TruststoreService {

    void addTrustedCert(String alias, Certificate certificate)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException;
}
