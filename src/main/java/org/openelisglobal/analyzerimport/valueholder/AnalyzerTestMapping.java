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

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Maps analyzer-specific test names to OpenELIS test IDs. Uses composite
 * primary key (@EmbeddedId) combining analyzerId and analyzerTestName.
 *
 * <p>
 * Test mappings are per-analyzer — each physical instrument owns its own set of
 * test code → OE test mappings, loaded from its profile.
 */
@Deprecated(forRemoval = true)
public class AnalyzerTestMapping extends BaseObject<AnalyzerTestMappingPK> {

    private static final long serialVersionUID = 3L;

    private AnalyzerTestMappingPK compoundId = new AnalyzerTestMappingPK();

    private String testId;

    /**
     * OGC-1129 — optional result component this analyzer target maps to
     * ({@code test_result_component.id}, VARCHAR(36)). Null = the test's PRIMARY
     * component (today's behavior); existing rows are unaffected.
     */
    private String componentId;

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
        return compoundId == null ? "0" : compoundId.getAnalyzerId();
    }

    public void setAnalyzerId(String analyzerId) {
        uniqueIdentifyer = null;
        compoundId.setAnalyzerId(analyzerId);
    }

    public String getAnalyzerId() {
        return compoundId == null ? null : compoundId.getAnalyzerId();
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

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setUniqueIdentifyer(String uniqueIdentifyer) {
        this.uniqueIdentifyer = uniqueIdentifyer;
    }

    public String getUniqueIdentifyer() {
        if (GenericValidator.isBlankOrNull(uniqueIdentifyer)) {
            uniqueIdentifyer = getAnalyzerId() + "-" + getAnalyzerTestName();
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
