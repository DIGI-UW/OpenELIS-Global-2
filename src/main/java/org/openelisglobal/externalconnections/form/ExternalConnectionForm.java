package org.openelisglobal.externalconnections.form;

import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.CertificateAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;

public class ExternalConnectionForm extends BaseForm {

    /** */
    private static final long serialVersionUID = -4622561239579733266L;

    @Valid
    private ExternalConnection externalConnection;

    @Valid
    private BasicAuthenticationData basicAuthenticationData;

    @Valid
    private CertificateAuthenticationData certificateAuthenticationData;

    @Valid
    private List<ExternalConnectionContact> externalConnectionContacts;

    private List<AuthType> authenticationTypes;

    private List<ProgrammedConnection> programmedConnections;

    public ExternalConnection getExternalConnection() {
        return externalConnection;
    }

    public void setExternalConnection(ExternalConnection externalConnection) {
        this.externalConnection = externalConnection;
    }

    public BasicAuthenticationData getBasicAuthenticationData() {
        return basicAuthenticationData;
    }

    public void setBasicAuthenticationData(BasicAuthenticationData basicAuthenticationData) {
        this.basicAuthenticationData = basicAuthenticationData;
    }

    public CertificateAuthenticationData getCertificateAuthenticationData() {
        return certificateAuthenticationData;
    }

    public void setCertificateAuthenticationData(CertificateAuthenticationData certificateAuthenticationData) {
        this.certificateAuthenticationData = certificateAuthenticationData;
    }

    public List<AuthType> getAuthenticationTypes() {
        return authenticationTypes;
    }

    public void setAuthenticationTypes(List<AuthType> authenticationTypes) {
        this.authenticationTypes = authenticationTypes;
    }

    public List<ExternalConnectionContact> getExternalConnectionContacts() {
        return externalConnectionContacts;
    }

    public void setExternalConnectionContacts(List<ExternalConnectionContact> externalConnectionContacts) {
        this.externalConnectionContacts = externalConnectionContacts;
    }

    public List<ProgrammedConnection> getProgrammedConnections() {
        return programmedConnections;
    }

    public void setProgrammedConnections(List<ProgrammedConnection> programmedConnections) {
        this.programmedConnections = programmedConnections;
    }
}
