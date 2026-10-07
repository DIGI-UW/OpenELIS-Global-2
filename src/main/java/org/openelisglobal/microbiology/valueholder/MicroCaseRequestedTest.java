package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/** Records order-stage case ownership before a specimen or analysis exists. */
@Entity
@Table(name = "micro_case_requested_test", schema = "clinlims")
public class MicroCaseRequestedTest extends BaseObject<String> {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = java.util.UUID.randomUUID().toString();

    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    @Column(name = "request_id", nullable = false)
    private Integer requestId;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "case_role", nullable = false, length = 20)
    private String caseRole;

    @Column(name = "collected_in_sets", nullable = false)
    private boolean collectedInSets;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "created_by", nullable = false, length = 20)
    private String createdBy;

    public String getId() {
        return id;
    }

    public void setId(String value) {
        id = value;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String value) {
        caseId = value;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer value) {
        requestId = value;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String value) {
        testId = value;
    }

    public String getCaseRole() {
        return caseRole;
    }

    public void setCaseRole(String value) {
        caseRole = value;
    }

    public boolean isCollectedInSets() {
        return collectedInSets;
    }

    public void setCollectedInSets(boolean value) {
        collectedInSets = value;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp value) {
        createdAt = value;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String value) {
        createdBy = value;
    }
}
