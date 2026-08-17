package org.openelisglobal.eqa.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.security.converter.EncryptionConverter;

/**
 * One material sample within a panel (FR-V2.1-12), carrying the sealed target
 * value.
 *
 * <p>
 * {@code targetValue} is encrypted at rest by the existing
 * {@link EncryptionConverter} (FR-V2.1-16). Three properties of that converter
 * shape how this column may be used: the ciphertext is salted per write, so the
 * column can never be indexed, made unique, or compared with {@code =}; blank
 * and whitespace values pass through unencrypted, so the service must reject
 * them rather than rely on the converter; and decrypting a value that was not
 * written as ciphertext throws, so nothing may seed plaintext here.
 *
 * <p>
 * Whether a caller is allowed to *see* the decrypted value depends on the
 * parent panel's status and the caller's unblind permission — that rule lives
 * in the DTO mapping (T-11), not here, because the entity cannot see the
 * caller.
 */
@Getter
@Setter
@Entity
@Table(name = "eqa_panel_sample", uniqueConstraints = @UniqueConstraint(name = "uq_eqa_panel_sample_panel_code", columnNames = {
        "panel_id", "sample_code" }))
public class EQAPanelSample extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_panel_sample_generator")
    @SequenceGenerator(name = "eqa_panel_sample_generator", sequenceName = "eqa_panel_sample_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panel_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQAPanel panel;

    @Column(name = "sample_code", nullable = false, length = 50)
    private String sampleCode;

    /** Participant-facing obfuscated identifier for in-house blinding. */
    @Column(name = "blind_code", length = 50)
    private String blindCode;

    @Column(name = "analyte_id", nullable = false)
    private Long analyteId;

    @Convert(converter = EncryptionConverter.class)
    @Column(name = "target_value", length = 512)
    private String targetValue;

    @Column(name = "target_unit", length = 50)
    private String targetUnit;

    @Column(name = "acceptance_range_low", precision = 15, scale = 5)
    private BigDecimal acceptanceRangeLow;

    @Column(name = "acceptance_range_high", precision = 15, scale = 5)
    private BigDecimal acceptanceRangeHigh;

    @Column(name = "source_reference", length = 255)
    private String sourceReference;

    @Column(name = "sys_user_id", nullable = false)
    private String sysUserId;

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }
}
