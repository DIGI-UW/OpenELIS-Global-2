package org.openelisglobal.analyzer.form;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Lab-facing analyzer instance input. Reusable profile behavior and defaults
 * are selected by profile ID and revision rather than copied into this request.
 */
public class AnalyzerInstanceRequest {

    @Size(min = 1, max = 100, message = "Analyzer name must be between 1 and 100 characters")
    private String name;

    private String profileId;

    private Integer profileRevision;

    private List<String> testUnitIds;

    private ObjectNode connectionValues;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public Integer getProfileRevision() {
        return profileRevision;
    }

    public void setProfileRevision(Integer profileRevision) {
        this.profileRevision = profileRevision;
    }

    public List<String> getTestUnitIds() {
        return testUnitIds;
    }

    public void setTestUnitIds(List<String> testUnitIds) {
        this.testUnitIds = testUnitIds;
    }

    public ObjectNode getConnectionValues() {
        return connectionValues == null ? null : connectionValues.deepCopy();
    }

    public void setConnectionValues(ObjectNode connectionValues) {
        this.connectionValues = connectionValues == null ? null : connectionValues.deepCopy();
    }
}
