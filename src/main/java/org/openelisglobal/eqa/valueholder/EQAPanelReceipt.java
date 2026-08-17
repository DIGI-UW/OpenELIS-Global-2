package org.openelisglobal.eqa.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
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
import java.sql.Date;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Participant-side confirmation that a panel arrived (FR-V2.1-20). One row per
 * cycle per participating lab, which is also what makes the derived
 * participant-state lookup a single-row read.
 *
 * <p>
 * {@code shipmentId} is an Integer, not the Long used elsewhere in the EQA
 * spine: it points at the pre-existing shipment table, whose primary key is
 * INTEGER. It is null for panels received without a tracked shipment.
 */
@Getter
@Setter
@Entity
@Table(name = "eqa_panel_receipt", uniqueConstraints = @UniqueConstraint(name = "uq_eqa_panel_receipt_cycle_enrollment", columnNames = {
        "cycle_id", "lab_enrollment_id" }))
public class EQAPanelReceipt extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_panel_receipt_generator")
    @SequenceGenerator(name = "eqa_panel_receipt_generator", sequenceName = "eqa_panel_receipt_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQACycle cycle;

    @Column(name = "lab_enrollment_id", nullable = false)
    private Long labEnrollmentId;

    @Column(name = "shipment_id")
    private Integer shipmentId;

    @Column(name = "received_date", nullable = false)
    private Date receivedDate;

    @Column(name = "received_by", nullable = false)
    private Long receivedBy;

    @Column(name = "received_temp_c", precision = 5, scale = 2)
    private BigDecimal receivedTempC;

    @Column(name = "integrity_ok", nullable = false)
    private Boolean integrityOk = true;

    /** Required by the service when integrityOk is false. */
    @Column(name = "integrity_notes", columnDefinition = "TEXT")
    private String integrityNotes;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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
