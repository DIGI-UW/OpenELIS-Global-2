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
 * Analyzer abnormal flag to OpenELIS interpretation mapping (FR-019).
 */
@Entity
@Table(name = "astm_flag_mapping")
public class AstmFlagMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "analyzer_id", nullable = false)
    @NotNull
    private Analyzer analyzer;

    @Column(name = "analyzer_flag", nullable = false, length = 30)
    @NotNull
    @Size(max = 30)
    private String analyzerFlag;

    @Column(name = "openelis_flag", nullable = false, length = 30)
    @NotNull
    @Size(max = 30)
    private String openelisFlag;

    @Column(name = "is_custom", nullable = false)
    private Boolean isCustom = false;

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

    public String getAnalyzerFlag() {
        return analyzerFlag;
    }

    public void setAnalyzerFlag(String analyzerFlag) {
        this.analyzerFlag = analyzerFlag;
    }

    public String getOpenelisFlag() {
        return openelisFlag;
    }

    public void setOpenelisFlag(String openelisFlag) {
        this.openelisFlag = openelisFlag;
    }

    public Boolean getIsCustom() {
        return isCustom;
    }

    public void setIsCustom(Boolean isCustom) {
        this.isCustom = isCustom;
    }
}
