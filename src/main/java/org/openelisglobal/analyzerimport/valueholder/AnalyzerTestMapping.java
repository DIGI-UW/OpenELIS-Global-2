/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzerimport.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Maps analyzer-specific test names to OpenELIS test IDs. Uses composite
 * primary key (@EmbeddedId) combining analyzerTypeId and analyzerTestName.
 *
 * <p>
 * Test mappings are a property of the plugin TYPE (capability), not a physical
 * device instance. A GeneXpert plugin always maps "MTB RIF" to the same
 * OpenELIS test, regardless of which physical device sent the data.
 */
@Entity
@Table(name = "analyzer_test_map")
public class AnalyzerTestMapping extends BaseObject<AnalyzerTestMappingPK> {

    private static final long serialVersionUID = 2L;

    @EmbeddedId
    private AnalyzerTestMappingPK compoundId = new AnalyzerTestMappingPK();

    @Column(name = "test_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    /** Legacy column — nullable, no longer part of PK. */
    @Column(name = "analyzer_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analyzerId;

    @Transient
    private String uniqueIdentifyer;

    public void setCompoundId(AnalyzerTestMappingPK compoundId) {
        uniqueIdentifyer = null;
        this.compoundId = compoundId;
    }

    public AnalyzerTestMappingPK getCompoundId() {
        return compoundId;
    }

    @Override
    public String getStringId() {
        return compoundId == null ? "0" : compoundId.getAnalyzerTypeId();
    }

    public void setAnalyzerTypeId(String analyzerTypeId) {
        uniqueIdentifyer = null;
        compoundId.setAnalyzerTypeId(analyzerTypeId);
    }

    public String getAnalyzerTypeId() {
        return compoundId == null ? null : compoundId.getAnalyzerTypeId();
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public String getAnalyzerTestName() {
        return compoundId == null ? null : compoundId.getAnalyzerTestName();
    }

    public void setAnalyzerTestName(String analyzerTestName) {
        uniqueIdentifyer = null;
        compoundId.setAnalyzerTestName(analyzerTestName);
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestId() {
        return testId;
    }

    public void setUniqueIdentifyer(String uniqueIdentifyer) {
        this.uniqueIdentifyer = uniqueIdentifyer;
    }

    public String getUniqueIdentifyer() {
        if (GenericValidator.isBlankOrNull(uniqueIdentifyer)) {
            uniqueIdentifyer = getAnalyzerTypeId() + "-" + getAnalyzerTestName();
        }

        return uniqueIdentifyer;
    }

    @Override
    public void setId(AnalyzerTestMappingPK id) {
        setCompoundId(id);
    }

    @Override
    public AnalyzerTestMappingPK getId() {
        return getCompoundId();
    }
}
