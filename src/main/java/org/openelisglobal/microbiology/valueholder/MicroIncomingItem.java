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
@Table(name = "micro_incoming_item", schema = "clinlims")
public class MicroIncomingItem extends BaseObject<String> {
    @Id
    @Column(name = "id")
    private String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private MicroCase microCase;
    @Column(name = "original_case_id", nullable = false)
    private String originalCaseId;
    @Column(name = "source", nullable = false)
    private String source;
    @Column(name = "test_id", nullable = false)
    @org.hibernate.annotations.Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;
    @Column(name = "sample_item_id")
    private String sampleItemId;
    @Column(name = "structured_result", nullable = false)
    private String structuredResult;
    @Column(name = "raw_result_value")
    private String rawResultValue;
    @Column(name = "run_source_key", nullable = false)
    private String runSourceKey;
    @Column(name = "received_at", nullable = false)
    private Timestamp receivedAt;
    @Column(name = "state", nullable = false)
    private String state;
    @Column(name = "placed_by")
    private String placedBy;
    @Column(name = "placed_at")
    private Timestamp placedAt;
    @Column(name = "placement_target")
    private String placementTarget;
    @Column(name = "failure_reason")
    private String failureReason;
}
