package org.openelisglobal.qaevent.valueholder;

import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

public class NceType extends BaseObject<String> {
    private String id;
    private String name;
    private String displayKey;
    private Integer categoryId;
    private Boolean active;
    private Timestamp lastupdated;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayKey() {
        return displayKey;
    }

    public void setDisplayKey(String displayKey) {
        this.displayKey = displayKey;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NceType nceType = (NceType) o;
        return id == nceType.id && Objects.equals(name, nceType.name) && Objects.equals(displayKey, nceType.displayKey)
                && Objects.equals(active, nceType.active) && Objects.equals(lastupdated, nceType.lastupdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, displayKey, active, lastupdated);
    }
}
