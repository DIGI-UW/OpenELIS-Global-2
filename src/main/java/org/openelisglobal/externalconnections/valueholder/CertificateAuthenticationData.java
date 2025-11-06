package org.openelisglobal.externalconnections.valueholder;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.springframework.web.multipart.MultipartFile;

@Entity
@Table(name = "certificate_authentication_data")
public class CertificateAuthenticationData extends ExternalConnectionAuthenticationData {

    private static final long serialVersionUID = 5731070741617843373L;

    // not persisted in db
    @Transient
    private MultipartFile certificate;

    @Override
    public AuthType getAuthenticationType() {
        return AuthType.CERTIFICATE;
    }

    @Override
    public boolean hasAuthenticationString() {
        return false;
    }

    @Override
    public String getAuthenticationString() {
        return null;
    }

    public MultipartFile getCertificate() {
        return certificate;
    }

    public void setCertificate(MultipartFile certificate) {
        this.certificate = certificate;
    }
}
