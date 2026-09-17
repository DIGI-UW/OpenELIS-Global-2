package org.openelisglobal.analyzer.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

@Deprecated(forRemoval = true)
public class AnalyzerPluginConfig extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String analyzerId;

    // @TypeDef + @Type("jsonb") kept until Hibernate 6 upgrade provides
    // @JdbcTypeCode(SqlTypes.JSON) — no JPA AttributeConverter for JSONB exists
    private String config = "{}";

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
