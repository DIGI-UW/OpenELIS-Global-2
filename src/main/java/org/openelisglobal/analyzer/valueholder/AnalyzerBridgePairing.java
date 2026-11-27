package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OpenELIS's client identity toward the Analyzer Bridge, and the Bridge it is
 * paired with. The identity is created once and kept across pairings; the
 * Bridge fields are empty until OpenELIS first pairs.
 */
@Entity
@Table(name = "analyzer_bridge_pairing")
@DynamicUpdate
public class AnalyzerBridgePairing extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_bridge_pairing_seq_gen")
    @GenericGenerator(name = "analyzer_bridge_pairing_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_bridge_pairing_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    /** PKCS#8, base64, encrypted with the general text encryptor. */
    @Column(name = "client_private_key", nullable = false)
    private String clientPrivateKey;

    /** PEM. */
    @Column(name = "client_certificate", nullable = false)
    private String clientCertificate;

    @Column(name = "bridge_url", length = 512)
    private String bridgeUrl;

    /** PEM. */
    @Column(name = "bridge_certificate")
    private String bridgeCertificate;

    @Column(name = "bridge_certificate_sha256", length = 64)
    private String bridgeCertificateSha256;

    @Column(name = "paired_at")
    private Timestamp pairedAt;

    @Column(name = "paired_by", length = 20)
    private String pairedBy;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getClientPrivateKey() {
        return clientPrivateKey;
    }

    public void setClientPrivateKey(String clientPrivateKey) {
        this.clientPrivateKey = clientPrivateKey;
    }

    public String getClientCertificate() {
        return clientCertificate;
    }

    public void setClientCertificate(String clientCertificate) {
        this.clientCertificate = clientCertificate;
    }

    public String getBridgeUrl() {
        return bridgeUrl;
    }

    public void setBridgeUrl(String bridgeUrl) {
        this.bridgeUrl = bridgeUrl;
    }

    public String getBridgeCertificate() {
        return bridgeCertificate;
    }

    public void setBridgeCertificate(String bridgeCertificate) {
        this.bridgeCertificate = bridgeCertificate;
    }

    public String getBridgeCertificateSha256() {
        return bridgeCertificateSha256;
    }

    public void setBridgeCertificateSha256(String bridgeCertificateSha256) {
        this.bridgeCertificateSha256 = bridgeCertificateSha256;
    }

    public Timestamp getPairedAt() {
        return pairedAt;
    }

    public void setPairedAt(Timestamp pairedAt) {
        this.pairedAt = pairedAt;
    }

    public String getPairedBy() {
        return pairedBy;
    }

    public void setPairedBy(String pairedBy) {
        this.pairedBy = pairedBy;
    }
}
