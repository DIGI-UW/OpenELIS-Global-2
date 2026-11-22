package org.openelisglobal.configuration.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One pass of the catalog loader: at start-up, through the reload API, or from
 * the import page as a preview or an apply. Unresolved references point at the
 * run that noticed them.
 */
@Entity
@Table(name = "configuration_import_run", schema = "clinlims")
public class ConfigurationImportRun extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    public static final String SOURCE_STARTUP = "STARTUP";
    public static final String SOURCE_API = "API_RELOAD";
    public static final String SOURCE_PREVIEW = "UI_PREVIEW";
    public static final String SOURCE_APPLY = "UI_APPLY";

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Timestamp startedAt;

    @Column(name = "finished_at")
    private Timestamp finishedAt;

    @Column(name = "summary")
    private String summary;

    @Column(name = "sys_user_id", precision = 10, scale = 0)
    private Integer systemUserId;

    public ConfigurationImportRun() {
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Timestamp finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }
}
