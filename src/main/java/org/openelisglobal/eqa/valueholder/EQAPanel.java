package org.openelisglobal.eqa.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A panel of material distributed to participants (FR-V2.1-11), shared by the
 * in-house (V2.4) and provider (V2.5) flows rather than forked per scheme type.
 * Carries its own source and inventory columns (FR-V2.1-17), because the prep →
 * ready_to_ship gate reads them.
 */
@Getter
@Setter
@Entity
@Table(name = "eqa_panel")
public class EQAPanel extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_panel_generator")
    @SequenceGenerator(name = "eqa_panel_generator", sequenceName = "eqa_panel_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "scheme_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQAProgram scheme;

    /** Null until the panel is bound to a cycle. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQACycle cycle;

    @Column(name = "panel_name", nullable = false, length = 255)
    private String panelName;

    /** Derived from the scheme type; the FRS defines no closed vocabulary. */
    @Column(name = "panel_type", length = 30)
    private String panelType;

    @Column(name = "prepared_by")
    private Long preparedBy;

    @Column(name = "prepared_at")
    private Timestamp preparedAt;

    /**
     * Required for in-house panels; the panel-writing service (T-11) must enforce
     * this, where the parent scheme's type is known.
     */
    @Column(name = "unblind_date")
    private Date unblindDate;

    /**
     * How the panel left DISTRIBUTED (FR-V2.4-10). sys_user_id cannot carry this on
     * its own, because the scheduler also writes a real user id.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "unblind_method", length = 20)
    private EQAUnblindMethod unblindMethod;

    @Column(name = "unblinded_by")
    private Long unblindedBy;

    @Column(name = "unblinded_at")
    private Timestamp unblindedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EQAPanelStatus status = EQAPanelStatus.PREPARING;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private EQAPanelSourceType sourceType;

    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    @Column(name = "vendor_name", length = 255)
    private String vendorName;

    @Column(name = "vendor_lot", length = 100)
    private String vendorLot;

    @Column(name = "vendor_certificate_ref", length = 255)
    private String vendorCertificateRef;

    @Column(name = "aliquots_produced", nullable = false)
    private Integer aliquotsProduced = 0;

    @Column(name = "aliquots_reserved", nullable = false)
    private Integer aliquotsReserved = 0;

    @Column(name = "aliquots_shipped", nullable = false)
    private Integer aliquotsShipped = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_temp", length = 30)
    private EQAStorageTemp storageTemp;

    @Column(name = "expiration_date")
    private Date expirationDate;

    /** Gates the provider cycle's prep_in_progress → ready_to_ship transition. */
    @Column(name = "homogeneity_qc_passed", nullable = false)
    private Boolean homogeneityQcPassed = false;

    @Column(name = "homogeneity_qc_notes", columnDefinition = "TEXT")
    private String homogeneityQcNotes;

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

    @PrePersist
    public void prePersist() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }
}
