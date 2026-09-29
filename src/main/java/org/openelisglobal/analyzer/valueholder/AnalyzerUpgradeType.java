package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Read-only upgrade input. Remove only after verified transfer and source
 * cleanup.
 */
@Entity
@Immutable
@Table(name = "analyzer_type")
public class AnalyzerUpgradeType extends BaseObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", insertable = false, updatable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;
    @Column(name = "name", insertable = false, updatable = false)
    private String name;
    @Column(name = "protocol", insertable = false, updatable = false)
    private String protocol;

    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }
}
