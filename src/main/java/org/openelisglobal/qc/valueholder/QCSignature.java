package org.openelisglobal.qc.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Table(name = "qc_signature")
public class QCSignature extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qc_signature_generator")
    @SequenceGenerator(name = "qc_signature_generator", sequenceName = "qc_signature_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "signed_at", nullable = false)
    private Timestamp signedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}
