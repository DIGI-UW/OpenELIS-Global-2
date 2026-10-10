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
@Table(name = "micro_culture_extension", schema = "clinlims")
public class MicroCultureExtension extends BaseObject<String> {
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
    @Column(name = "extend_by")
    private java.math.BigDecimal extendBy;
    @Column(name = "unit")
    private String unit;
    @Column(name = "reason_id")
    @org.hibernate.annotations.Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String reasonId;
    @Column(name = "note")
    private String note;
}
