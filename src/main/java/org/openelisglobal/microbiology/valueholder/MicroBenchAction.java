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
@Table(name = "micro_bench_action", schema = "clinlims")
public class MicroBenchAction extends BaseObject<String> {
    @Id
    @Column(name = "id")
    private String id = UUID.randomUUID().toString();
    @Column(name = "case_id")
    private String caseId;
    @Column(name = "action")
    private String action;
    @Column(name = "before_state")
    private String beforeState;
    @Column(name = "after_state")
    private String afterState;
    @Column(name = "performed_by")
    private String performedBy;
    @Column(name = "performed_at")
    private Timestamp performedAt;
    @Column(name = "undone_by")
    private String undoneBy;
    @Column(name = "undone_at")
    private Timestamp undoneAt;
}
