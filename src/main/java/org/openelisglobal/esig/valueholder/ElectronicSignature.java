package org.openelisglobal.esig.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Represents an electronic signature event per 21 CFR Part 11.
 *
 * <p>
 * Each record captures a legally-binding signature with:
 * <ul>
 * <li>Signer identity (printed name, user ID)</li>
 * <li>Signature meaning (what the signer is attesting to)</li>
 * <li>Timestamp (when the signature occurred)</li>
 * <li>Link to the signed record</li>
 * <li>Authentication metadata (method, IP, user agent)</li>
 * </ul>
 *
 * <p>
 * This table is append-only. Records must never be updated or deleted at the
 * application level to maintain regulatory compliance.
 *
 * @see SignatureMeaning
 * @see AuthMethod
 */
@Entity
@Table(name = "electronic_signature")
public class ElectronicSignature extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "electronic_signature_seq")
    @SequenceGenerator(name = "electronic_signature_seq", sequenceName = "electronic_signature_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    /**
     * Foreign key to system_user. The user who executed the signature.
     */
    @NotNull(message = "Signer ID is required")
    @Column(name = "signer_id", nullable = false)
    private Long signerId;

    /**
     * Full name of the signer at the time of signing. Denormalized for compliance —
     * name changes don't alter historical records.
     */
    @NotBlank(message = "Signer printed name is required")
    @Size(max = 255)
    @Column(name = "signer_name_printed", nullable = false, length = 255)
    private String signerNamePrinted;

    /**
     * The legal meaning of this signature (what the signer is attesting to).
     */
    @NotNull(message = "Signature meaning is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "signature_meaning", nullable = false, length = 30)
    private SignatureMeaning signatureMeaning;

    /**
     * Server-generated timestamp when the signature was executed (UTC).
     */
    @NotNull(message = "Signed at timestamp is required")
    @Column(name = "signed_at", nullable = false)
    private Timestamp signedAt;

    /**
     * Type of record being signed (e.g., "RESULT", "ANALYSIS").
     */
    @NotBlank(message = "Record type is required")
    @Size(max = 100)
    @Column(name = "record_type", nullable = false, length = 100)
    private String recordType;

    /**
     * Primary key of the record being signed.
     */
    @NotNull(message = "Record ID is required")
    @Column(name = "record_id", nullable = false)
    private Long recordId;

    /**
     * Required when signature_meaning is REJECTED. Explains why the result was
     * rejected.
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * Sequence number within the signing session. 1 = first signature (full auth),
     * 2+ = subsequent signatures (password-only).
     */
    @NotNull(message = "Session signing sequence is required")
    @Column(name = "session_signing_sequence", nullable = false)
    private Integer sessionSigningSequence;

    /**
     * Authentication method used to verify credentials for this signature.
     */
    @NotNull(message = "Auth method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false, length = 20)
    private AuthMethod authMethod;

    /**
     * IP address of the client that executed the signature.
     */
    @Size(max = 45)
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /**
     * Browser user agent string of the signing client.
     */
    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    public ElectronicSignature() {
        super();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getSignerId() {
        return signerId;
    }

    public void setSignerId(Long signerId) {
        this.signerId = signerId;
    }

    public String getSignerNamePrinted() {
        return signerNamePrinted;
    }

    public void setSignerNamePrinted(String signerNamePrinted) {
        this.signerNamePrinted = signerNamePrinted;
    }

    public SignatureMeaning getSignatureMeaning() {
        return signatureMeaning;
    }

    public void setSignatureMeaning(SignatureMeaning signatureMeaning) {
        this.signatureMeaning = signatureMeaning;
    }

    public Timestamp getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Timestamp signedAt) {
        this.signedAt = signedAt;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Integer getSessionSigningSequence() {
        return sessionSigningSequence;
    }

    public void setSessionSigningSequence(Integer sessionSigningSequence) {
        this.sessionSigningSequence = sessionSigningSequence;
    }

    public AuthMethod getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Checks if this signature is a rejection.
     *
     * @return true if signature meaning is REJECTED
     */
    public boolean isRejection() {
        return SignatureMeaning.REJECTED.equals(signatureMeaning);
    }

    /**
     * Checks if this was the first signature in the session (full authentication).
     *
     * @return true if session signing sequence is 1
     */
    public boolean isFirstSignatureInSession() {
        return sessionSigningSequence != null && sessionSigningSequence == 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ElectronicSignature that = (ElectronicSignature) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
