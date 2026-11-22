package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@Table(name = "micro_culture_proposal", schema = "clinlims")
public class MicroCultureProposal extends BaseObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id")
    private String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inoculation_id", nullable = false)
    private MicroCaseInoculation inoculation;
    @Column(name = "recorded_by", nullable = false)
    private String recordedBy;
    @Column(name = "recorded_at", nullable = false)
    private Timestamp recordedAt;
    @Column(name = "signal")
    private String signal;
    @Column(name = "confirmed_by")
    private String confirmedBy;
    @Column(name = "confirmed_at")
    private Timestamp confirmedAt;
}
