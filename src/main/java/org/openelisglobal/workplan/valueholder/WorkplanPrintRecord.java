package org.openelisglobal.workplan.valueholder;

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
@Table(name = "workplan_print_record", schema = "clinlims")
public class WorkplanPrintRecord extends BaseObject<String> {
    @Id
    @Column(name = "id")
    private String id = UUID.randomUUID().toString();
    @Column(name = "layout")
    private String layout;
    @Column(name = "lab_unit_id")
    private String labUnitId;
    @Column(name = "printed_by")
    private String printedBy;
    @Column(name = "printed_at")
    private Timestamp printedAt;
    @Column(name = "snapshot")
    private String snapshot;
}
