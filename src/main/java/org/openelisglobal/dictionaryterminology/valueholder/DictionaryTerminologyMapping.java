package org.openelisglobal.dictionaryterminology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Links an answer (a dictionary entry used as a result option) to a
 * standard-terminology code (LOINC / SNOMED / CIEL / OCL) with a relationship
 * qualifier. An answer row is shared by every test that offers it and standard
 * answer codes are global, so a mapping has no test, component or specimen
 * scope. Unique on {@code (dictionary_id, source, code)}; the SAME_AS LOINC
 * mapping stays denormalized on {@code dictionary.loinc_code}.
 *
 * <p>
 * {@code dictionary_id} is a numeric FK (String via LIMSStringNumberUserType).
 * The audit {@code @Version} column ({@code last_updated}) comes from
 * {@link BaseObject}; the table's separate {@code lastupdated} (DEFAULT now())
 * is filled by the DB and not mapped.
 */
@Entity
@Table(name = "dictionary_terminology_mapping", schema = "clinlims")
public class DictionaryTerminologyMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "dictionary_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String dictionaryId;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "relationship", length = 20)
    private String relationship;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "is_active", nullable = false, length = 2)
    private String isActive = "Y";

    public DictionaryTerminologyMapping() {
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

    public String getDictionaryId() {
        return dictionaryId;
    }

    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
