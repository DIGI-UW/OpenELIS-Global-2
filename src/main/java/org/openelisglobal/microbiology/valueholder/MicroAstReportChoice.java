package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_report_choice", schema = "clinlims")
public class MicroAstReportChoice extends BaseObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String value) {
        id = value;
    }

    @Column(name = "isolate_id", nullable = false, length = 36)
    private String isolateId;

    public String getIsolateId() {
        return isolateId;
    }

    public void setIsolateId(String value) {
        isolateId = value;
    }

    @Column(name = "antibiotic_id", nullable = false, length = 36)
    private String antibioticId;

    public String getAntibioticId() {
        return antibioticId;
    }

    public void setAntibioticId(String value) {
        antibioticId = value;
    }

    @Column(name = "reading_id", nullable = false, length = 36)
    private String readingId;

    public String getReadingId() {
        return readingId;
    }

    public void setReadingId(String value) {
        readingId = value;
    }

    @Column(name = "selected_by", nullable = false, length = 20)
    private String selectedBy;

    public String getSelectedBy() {
        return selectedBy;
    }

    public void setSelectedBy(String value) {
        selectedBy = value;
    }

    @Column(name = "selected_at", nullable = false)
    private Timestamp selectedAt;

    public Timestamp getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(Timestamp value) {
        selectedAt = value;
    }
}
