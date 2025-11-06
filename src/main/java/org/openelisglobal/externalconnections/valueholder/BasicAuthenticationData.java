package org.openelisglobal.externalconnections.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Base64;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.security.converter.EncryptionConverter;

@Entity
@Table(name = "basic_authentication_data")
public class BasicAuthenticationData extends ExternalConnectionAuthenticationData {

    private static final long serialVersionUID = -3749126139570042912L;

    @Column
    private String username;

    @Column
    @Convert(converter = EncryptionConverter.class)
    private String password;

    @Override
    public AuthType getAuthenticationType() {
        return AuthType.BASIC;
    }

    @Override
    public boolean hasAuthenticationString() {
        return true;
    }

    @Override
    public String getAuthenticationString() {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
