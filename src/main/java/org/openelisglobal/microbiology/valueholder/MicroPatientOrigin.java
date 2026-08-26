package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_patient_origin", schema = "clinlims")
public class MicroPatientOrigin extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "whonet_code", nullable = false, length = 10)
    private String whonetCode;

    @Column(name = "is_active", nullable = false, length = 2)
    private String isActive = "Y";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getWhonetCode() {
        return whonetCode;
    }

    public void setWhonetCode(String whonetCode) {
        this.whonetCode = whonetCode;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
