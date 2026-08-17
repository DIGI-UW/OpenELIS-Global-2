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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Date;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Append-only competency log for an analyst (FR-V2.1-22).
 *
 * <p>
 * It exists because a missed deadline is an absence of a result, not a result:
 * such a row has no value and no score, so eqa_participant_result alone cannot
 * feed the V2.3 competency dashboard. Escalations and triage dismissals land
 * here too.
 *
 * <p>
 * Service-write-only: a direct REST create must be refused (AC-V2.1-21).
 * {@code nceId} is an Integer because nc_event's primary key is INTEGER.
 */
@Getter
@Setter
@Entity
@Table(name = "eqa_analyst_competency_event")
public class EQAAnalystCompetencyEvent extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_analyst_competency_event_generator")
    @SequenceGenerator(name = "eqa_analyst_competency_event_generator", sequenceName = "eqa_analyst_competency_event_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "analyst_id", nullable = false)
    private Long analystId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private EQACompetencyEventType eventType;

    @Column(name = "event_date", nullable = false)
    private Date eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQAProgram scheme;

    /** Null for cross-cycle events. */
    @Column(name = "cycle_id")
    private Long cycleId;

    @Column(name = "participant_result_id")
    private Long participantResultId;

    @Column(name = "analyte_id")
    private Long analyteId;

    @Column(name = "nce_id")
    private Integer nceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dismissal_category", length = 40)
    private EQADismissalCategory dismissalCategory;

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
