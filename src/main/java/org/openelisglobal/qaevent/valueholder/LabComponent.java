package org.openelisglobal.qaevent.valueholder;

import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

public class LabComponent extends BaseObject<String> {
    private String id;
    private String name;
    private Timestamp lastmodified;

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

    public Timestamp getLastmodified() {
        return lastmodified;
    }

    public void setLastmodified(Timestamp lastmodified) {
        this.lastmodified = lastmodified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LabComponent that = (LabComponent) o;
        return id == that.id && Objects.equals(name, that.name) && Objects.equals(lastmodified, that.lastmodified);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, lastmodified);
    }
}
