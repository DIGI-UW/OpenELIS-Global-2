package org.openelisglobal.security.certs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import org.openelisglobal.security.certs.service.TruststoreServiceImpl;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.io.FileSystemResource;

/**
 * Runs as the actual container application user in
 * test_truststore_permissions.sh.
 */
public class TruststoreWriteProbe {
    public static void main(String[] args) throws Exception {
        Path store = Path.of(System.getenv("SSL_TRUSTSTORE_PATH"));
        TruststoreServiceImpl service = new TruststoreServiceImpl();
        var fields = new DirectFieldAccessor(service);
        fields.setPropertyValue("trustStore", new FileSystemResource(store));
        fields.setPropertyValue("trustStorePassword", System.getenv("OE_TRUSTSTORE_PROBE_PASSWORD").toCharArray());
        try (var input = Files.newInputStream(Path.of("/tmp/partner.pem"))) {
            service.addTrustedCert("uploaded-partner",
                    CertificateFactory.getInstance("X.509").generateCertificate(input));
        }
        if (!KeyStore.getInstance(store.toFile(), System.getenv("OE_TRUSTSTORE_PROBE_PASSWORD").toCharArray())
                .containsAlias("uploaded-partner")) {
            throw new AssertionError("Uploaded certificate was not persisted");
        }
        System.out.println("Certificate upload persisted as the application user");
    }
}
