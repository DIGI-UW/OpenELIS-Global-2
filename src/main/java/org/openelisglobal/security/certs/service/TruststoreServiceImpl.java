package org.openelisglobal.security.certs.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class TruststoreServiceImpl implements TruststoreService {

    @Value("${server.ssl.trust-store}")
    private Resource trustStore;

    @Value("${server.ssl.trust-store-password}")
    private char[] trustStorePassword;

    @Override
    public void addTrustedCert(String alias, Certificate certificate)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keystore = KeyStore.getInstance(trustStore.getFile(), trustStorePassword);
        keystore.setCertificateEntry(alias, certificate);
        // write the changes to the file
        try (FileOutputStream fos = new FileOutputStream(trustStore.getFile().getAbsolutePath())) {
            keystore.store(fos, trustStorePassword);
        }
    }
}
