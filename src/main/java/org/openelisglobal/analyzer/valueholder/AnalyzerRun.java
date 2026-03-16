package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;

/**
 * Represents a single import execution (batch of results from one file). Holds
 * custom_preview_data (JSONB) for preview rendering (OGC-324).
 */
@Entity
@Table(name = "analyzer_run")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class AnalyzerRun extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "analyzer_file_upload_id", nullable = false)
    private Long analyzerFileUploadId;

    @Column(name = "plugin_id", length = 100)
    private String pluginId;

    @Type(type = "jsonb")
    @Column(name = "custom_preview_data", columnDefinition = "jsonb")
    private String customPreviewData;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnalyzerFileUploadId() {
        return analyzerFileUploadId;
    }

    public void setAnalyzerFileUploadId(Long analyzerFileUploadId) {
        this.analyzerFileUploadId = analyzerFileUploadId;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getCustomPreviewData() {
        return customPreviewData;
    }

    public void setCustomPreviewData(String customPreviewData) {
        this.customPreviewData = customPreviewData;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
