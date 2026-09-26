package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

/** Read-only source mappings; runtime uses AnalyzerSiteBinding instead. */
@Entity
@Immutable
@Table(name = "analyzer_test_map")
@IdClass(AnalyzerUpgradeMapping.Key.class)
public class AnalyzerUpgradeMapping {
    @Id
    @Column(name = "analyzer_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analyzerId;
    @Id
    @Column(name = "analyzer_test_name")
    private String sourceCode;
    @Column(name = "test_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;
    @Column(name = "component_id")
    private String componentId;

    public String getSourceCode() {
        return sourceCode;
    }

    public String getTestId() {
        return testId;
    }

    public String getComponentId() {
        return componentId;
    }

    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;
        public String analyzerId;
        public String sourceCode;

        public Key() {
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(analyzerId, key.analyzerId)
                    && Objects.equals(sourceCode, key.sourceCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(analyzerId, sourceCode);
        }
    }
}
