package org.openelisglobal.testvariant.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1142 / FR-46–55 — membership of a test in a specimen-variant assay group.
 *
 * <p>
 * Two tests are variants of the same assay when and only when they share a
 * {@code group_id} (FR-46). A test belongs to at most one group (unique on
 * {@code test_id}). {@code test_id} is a numeric FK stored as String via
 * {@code LIMSStringNumberUserType}, the established idiom. The audit
 * {@code @Version} column ({@code last_updated}) comes from {@link BaseObject};
 * the table's separate {@code lastupdated} (DEFAULT now()) is DB-filled and not
 * mapped — mirrors {@code TestTerminologyMapping}.
 */
@Entity
@Table(name = "test_variant_link", schema = "clinlims")
public class TestVariantLink extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "group_id", nullable = false, length = 36)
    private String groupId;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    public TestVariantLink() {
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

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }
}
