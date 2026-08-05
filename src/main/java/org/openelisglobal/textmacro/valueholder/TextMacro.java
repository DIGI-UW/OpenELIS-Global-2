package org.openelisglobal.textmacro.valueholder;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "text_macro", schema = "clinlims")
public class TextMacro extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "text_macro_seq_gen")
    @GenericGenerator(name = "text_macro_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "text_macro_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "code", nullable = false, length = 64, unique = true)
    private String code;

    @Column(name = "expansion_text", nullable = false, length = 4000)
    private String expansionText;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "provenance", nullable = false, length = 20)
    private String provenance = "LOCAL";

    @Column(name = "source_key", length = 100)
    private String sourceKey;

    @Column(name = "source_version", length = 50)
    private String sourceVersion;

    @Column(name = "last_updated_by", nullable = false, length = 20)
    private String lastUpdatedBy;

    @ElementCollection(fetch = FetchType.LAZY)
    @BatchSize(size = 100)
    @CollectionTable(name = "text_macro_context", schema = "clinlims", joinColumns = @JoinColumn(name = "macro_id"))
    @Column(name = "context", nullable = false, length = 80)
    @Enumerated(EnumType.STRING)
    private Set<TextMacroContext> contexts = new LinkedHashSet<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getExpansionText() {
        return expansionText;
    }

    public void setExpansionText(String expansionText) {
        this.expansionText = expansionText;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public Set<TextMacroContext> getContexts() {
        return contexts;
    }

    public void setContexts(Set<TextMacroContext> contexts) {
        this.contexts = contexts == null ? new LinkedHashSet<>() : new LinkedHashSet<>(contexts);
    }
}
