package org.openelisglobal.configuration.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A spelling the catalog files use for a record that exists under another name,
 * remembered when someone resolved it once so the next import resolves it by
 * itself. The alias is stored normalized (letters and digits only, lower case)
 * so the match is as forgiving as the description lookup.
 */
@Entity
@Table(name = "reference_alias", schema = "clinlims")
public class ReferenceAlias extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "reference_type", nullable = false, length = 30)
    private String referenceType;

    @Column(name = "alias", nullable = false, length = 255)
    private String alias;

    @Column(name = "target_id", nullable = false, length = 36)
    private String targetId;

    @Column(name = "sys_user_id", precision = 10, scale = 0)
    private Integer systemUserId;

    public ReferenceAlias() {
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

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }
}
