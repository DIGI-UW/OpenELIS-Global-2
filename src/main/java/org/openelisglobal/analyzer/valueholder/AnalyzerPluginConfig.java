package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;

@Entity
@Table(name = "analyzer_plugin_config")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class AnalyzerPluginConfig extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "analyzer_id", nullable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analyzerId;

    @Type(type = "jsonb")
    @Column(name = "config", columnDefinition = "jsonb", nullable = false)
    private String config = "{}";

    @PrePersist
    protected void prePersist() {
        if (config == null || config.trim().isEmpty()) {
            config = "{}";
        }
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = (config == null || config.trim().isEmpty()) ? "{}" : config;
    }

    @Override
    public String getId() {
        return analyzerId;
    }

    @Override
    public void setId(String id) {
        this.analyzerId = id;
    }
}
