package org.openelisglobal.externalconnections.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import java.net.URI;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.URIConverter;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.valueholder.Localization;

@Entity
@Table(name = "external_connection")
public class ExternalConnection extends BaseObject<Integer> {

    public enum AuthType {
        CERTIFICATE("certificate", "externalconnections.authtype.cert"),
        BASIC("basic", "externalconnections.authtype.basic"),
        // BEARER("bearer", "externalconnections.authtype.bearer"),
        NONE("none", "externalconnections.authtype.none");

        private String value;
        private String messageKey;

        AuthType(String value, String messageKey) {
            this.value = value;
            this.messageKey = messageKey;
        }

        public String getValue() {
            return value;
        }

        public String getMessage() {
            return MessageUtil.getMessage(messageKey);
        }
    }

    public enum ProgrammedConnection {
        // CLINIC_SEARCH("clinlic_search", "externalconnections.clinicsearch"),
        SMPP_SERVER("smpp_server", "externalconnections.smppserver"),
        BMP_SMS_SERVER("bmp_sms_server", "externalconnections.bmpsms"),
        INFO_HIGHWAY("info_highway", "externalconnections.infohighway"),
        SMTP_SERVER("smtp_server", "externalconnections.smtpserver");

        private String value;
        private String messageKey;

        ProgrammedConnection(String value, String messageKey) {
            this.value = value;
            this.messageKey = messageKey;
        }

        public String getValue() {
            return value;
        }

        public String getMessage() {
            return MessageUtil.getMessage(messageKey);
        }
    }

    private static final long serialVersionUID = -6727446336117070253L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "external_connection_generator")
    @SequenceGenerator(name = "external_connection_generator", sequenceName = "external_connection_seq", allocationSize = 1)
    private Integer id;

    @Column
    private Boolean active;

    @Convert(converter = URIConverter.class)
    @Column
    private URI uri;

    @Column(name = "programmed_connection", unique = true)
    @Enumerated(EnumType.STRING)
    private ProgrammedConnection programmedConnection;

    @Valid
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "name_localization_id", referencedColumnName = "id")
    private Localization nameLocalization;

    @Valid
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "description_localization_id", referencedColumnName = "id")
    private Localization descriptionLocalization;

    @Column(name = "active_authentication_type")
    @Enumerated(EnumType.STRING)
    private AuthType activeAuthenticationType;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public ProgrammedConnection getProgrammedConnection() {
        return programmedConnection;
    }

    public void setProgrammedConnection(ProgrammedConnection programmedConnection) {
        this.programmedConnection = programmedConnection;
    }

    public AuthType getActiveAuthenticationType() {
        return activeAuthenticationType;
    }

    public void setActiveAuthenticationType(AuthType activeAuthenticationType) {
        this.activeAuthenticationType = activeAuthenticationType;
    }

    public Localization getDescriptionLocalization() {
        return descriptionLocalization;
    }

    public void setDescriptionLocalization(Localization descriptionLocalization) {
        this.descriptionLocalization = descriptionLocalization;
    }

    public Localization getNameLocalization() {
        return nameLocalization;
    }

    public void setNameLocalization(Localization nameLocalization) {
        this.nameLocalization = nameLocalization;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
