package org.openelisglobal.testmethod.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Date;
import java.util.UUID;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;

@Entity
@Table(name = "test_method", schema = "clinlims")
public class TestMethod extends EnumValueItemImpl {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "test_id", nullable = false)
    private String testId;

    @Column(name = "method_id", nullable = false)
    private String methodId;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "effective_date", nullable = false)
    private Date effectiveDate;

    @Column(name = "is_active", nullable = false)
    private String isActive = "Y";

    public TestMethod() {
        super();
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getMethodId() {
        return methodId;
    }

    public void setMethodId(String methodId) {
        this.methodId = methodId;
    }

    public boolean getIsDefaultMethod() {
        return isDefault;
    }

    public void setIsDefaultMethod(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    @Override
    public String getIsActive() {
        return isActive;
    }

    @Override
    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
