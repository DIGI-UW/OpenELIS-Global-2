package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Analyzer runtime ASTM configuration (FR-014..018). Connection role,
 * aggregation mode, ports/IPs per data-model.md.
 */
@Entity
@Table(name = "astm_analyzer_config")
public class AstmAnalyzerConfig extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "analyzer_id", nullable = false, unique = true)
    @NotNull
    private Analyzer analyzer;

    @Column(name = "connection_role", nullable = false, length = 10)
    @NotNull
    @Size(max = 10)
    private String connectionRole = "SERVER";

    @Column(name = "server_listen_port")
    private Integer serverListenPort;

    @Column(name = "client_target_ip", length = 64)
    @Size(max = 64)
    private String clientTargetIp;

    @Column(name = "client_target_port")
    private Integer clientTargetPort;

    @Column(name = "aggregation_mode", nullable = false, length = 20)
    @NotNull
    @Size(max = 20)
    private String aggregationMode = "PER_MESSAGE";

    @Column(name = "aggregation_window_seconds")
    private Integer aggregationWindowSeconds;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public String getConnectionRole() {
        return connectionRole;
    }

    public void setConnectionRole(String connectionRole) {
        this.connectionRole = connectionRole;
    }

    public Integer getServerListenPort() {
        return serverListenPort;
    }

    public void setServerListenPort(Integer serverListenPort) {
        this.serverListenPort = serverListenPort;
    }

    public String getClientTargetIp() {
        return clientTargetIp;
    }

    public void setClientTargetIp(String clientTargetIp) {
        this.clientTargetIp = clientTargetIp;
    }

    public Integer getClientTargetPort() {
        return clientTargetPort;
    }

    public void setClientTargetPort(Integer clientTargetPort) {
        this.clientTargetPort = clientTargetPort;
    }

    public String getAggregationMode() {
        return aggregationMode;
    }

    public void setAggregationMode(String aggregationMode) {
        this.aggregationMode = aggregationMode;
    }

    public Integer getAggregationWindowSeconds() {
        return aggregationWindowSeconds;
    }

    public void setAggregationWindowSeconds(Integer aggregationWindowSeconds) {
        this.aggregationWindowSeconds = aggregationWindowSeconds;
    }
}
