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
 */
package org.openelisglobal.program.valueholder;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.test.valueholder.TestSection;

@JsonAutoDetect(fieldVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
public class Program extends BaseObject<String> {

    @JsonProperty("code")
    @Pattern(regexp = "(?i)^[a-z0-9_ ]*$")
    private String code;

    @JsonProperty("id")
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String id;

    @JsonProperty("programName")
    @Pattern(regexp = "(?i)^[a-z0-9-_ ]*$")
    private String programName;

    @JsonProperty("questionnaireUUID")
    private UUID questionnaireUUID;

    private TestSection testSection;

    @JsonProperty("manuallyChanged")
    private Boolean manuallyChanged;

    // OGC Programs V2: CLINICAL / ENVIRONMENTAL / VECTOR. Mirrors panel.domain
    // (OGC-224) and test_section.domain (OGC-1020) so the picker can filter
    // by order.domain. Existing rows backfilled to CLINICAL by Liquibase 105.
    private String domain = "CLINICAL";

    // Deactivate/reactivate flag; 'Y'/'N' to match panel / test_section / test.
    private String isActive = "Y";

    // Many-to-many replacement for the single testSection FK; the legacy
    // testSection field stays for readers that have not migrated yet.
    private Set<TestSection> labUnits = new HashSet<>();

    public Program() {
        super();
    }

    public String getCode() {
        return this.code;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getProgramName() {
        return this.programName;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public UUID getQuestionnaireUUID() {
        return questionnaireUUID;
    }

    public void setQuestionnaireUUID(UUID questionnaireUUID) {
        this.questionnaireUUID = questionnaireUUID;
    }

    public TestSection getTestSection() {
        return testSection;
    }

    public void setTestSection(TestSection testSection) {
        this.testSection = testSection;
    }

    public Boolean getManuallyChanged() {
        return manuallyChanged;
    }

    public void setManuallyChanged(Boolean manuallyChanged) {
        this.manuallyChanged = manuallyChanged;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public Set<TestSection> getLabUnits() {
        return labUnits;
    }

    public void setLabUnits(Set<TestSection> labUnits) {
        this.labUnits = labUnits == null ? new HashSet<>() : labUnits;
    }

    /**
     * Audit-trail projection of the lab unit set. The history diff skips
     * collections unless the entity offers a {@code get<Field>_Audit()} view, so
     * this is what a lab-unit change is recorded as.
     */
    public String getLabUnits_Audit() {
        if (labUnits == null || labUnits.isEmpty()) {
            return "";
        }
        return labUnits.stream().map(TestSection::getId).filter(Objects::nonNull).sorted()
                .collect(Collectors.joining(","));
    }
}
