package org.openelisglobal.analyzer.form;

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

    private String ipAddress;

    private Integer port;

    private String communicationMode;

    private String transportMode;

    private String connectionRole;

    private String importDirectory;

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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getCommunicationMode() {
        return communicationMode;
    }

    public void setCommunicationMode(String communicationMode) {
        this.communicationMode = communicationMode;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public String getConnectionRole() {
        return connectionRole;
    }

    public void setConnectionRole(String connectionRole) {
        this.connectionRole = connectionRole;
    }

    public String getImportDirectory() {
        return importDirectory;
    }

    public void setImportDirectory(String importDirectory) {
        this.importDirectory = importDirectory;
    }
}
