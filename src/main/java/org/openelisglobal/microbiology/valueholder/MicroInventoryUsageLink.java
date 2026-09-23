package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_inventory_usage_link", schema = "clinlims")
public class MicroInventoryUsageLink extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    @Column(name = "inventory_usage_id", nullable = false)
    private Long inventoryUsageId;

    @Column(name = "usage_context", nullable = false, length = 30)
    private String usageContext;

    @Column(name = "activity_id", length = 36)
    private String activityId;

    @Column(name = "ast_run_id", length = 36)
    private String astRunId;

    @Column(name = "test_reagent_link_id", nullable = false, length = 36)
    private String testReagentLinkId;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Long getInventoryUsageId() {
        return inventoryUsageId;
    }

    public void setInventoryUsageId(Long inventoryUsageId) {
        this.inventoryUsageId = inventoryUsageId;
    }

    public String getUsageContext() {
        return usageContext;
    }

    public void setUsageContext(String usageContext) {
        this.usageContext = usageContext;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getAstRunId() {
        return astRunId;
    }

    public void setAstRunId(String astRunId) {
        this.astRunId = astRunId;
    }

    public String getTestReagentLinkId() {
        return testReagentLinkId;
    }

    public void setTestReagentLinkId(String testReagentLinkId) {
        this.testReagentLinkId = testReagentLinkId;
    }
}
