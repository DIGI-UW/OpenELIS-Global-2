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
@Table(name = "analyzer_plugin_config")
public class AnalyzerUpgradeConfig extends BaseObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "analyzer_id", insertable = false, updatable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;
    @Column(name = "config", insertable = false, updatable = false)
    @Type(type = "org.openelisglobal.hibernate.type.JsonBinaryType")
    private String config;

    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getConfig() {
        return config;
    }
}
