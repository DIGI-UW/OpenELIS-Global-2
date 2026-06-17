package org.openelisglobal.testterminology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-949 M10 / OGC-939 — a terminology mapping for a test: links an OpenELIS
 * test to a standard-terminology code (LOINC / SNOMED / CIEL / OCL) with a
 * relationship qualifier. A test can have many mappings, unique on
 * {@code (test_id, source, code)}.
 *
 * <p>
 * {@code test_id} is a numeric FK (String via LIMSStringNumberUserType). The
 * audit {@code @Version} column ({@code last_updated}) comes from
 * {@link BaseObject}; the table's separate {@code lastupdated} (DEFAULT now())
 * is filled by the DB and not mapped — mirrors {@code TestSampleHandling}.
 */
@Entity
@Table(name = "test_terminology_mapping", schema = "clinlims")
public class TestTerminologyMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "relationship", length = 20)
    private String relationship;

    @Column(name = "is_active", nullable = false, length = 2)
    private String isActive = "Y";

    public TestTerminologyMapping() {
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

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
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

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
