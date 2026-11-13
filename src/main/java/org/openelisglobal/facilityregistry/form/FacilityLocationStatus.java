package org.openelisglobal.facilityregistry.form;

import java.math.BigDecimal;

/**
 * What the facility location card shows: the coordinates in effect, where they
 * came from, the location mode and the last registration with the facility
 * registry.
 */
public class FacilityLocationStatus {

    private String mode;
    private boolean autolocation;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String source;
    private boolean manualCoordinatesIgnored;
    private boolean registryConfigured;
    private String registryUrl;
    private Boolean lastRegistrationSuccess;
    private String lastRegistrationAt;
    private String lastRegistrationMessage;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isAutolocation() {
        return autolocation;
    }

    public void setAutolocation(boolean autolocation) {
        this.autolocation = autolocation;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isManualCoordinatesIgnored() {
        return manualCoordinatesIgnored;
    }

    public void setManualCoordinatesIgnored(boolean manualCoordinatesIgnored) {
        this.manualCoordinatesIgnored = manualCoordinatesIgnored;
    }

    public boolean isRegistryConfigured() {
        return registryConfigured;
    }

    public void setRegistryConfigured(boolean registryConfigured) {
        this.registryConfigured = registryConfigured;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public Boolean getLastRegistrationSuccess() {
        return lastRegistrationSuccess;
    }

    public void setLastRegistrationSuccess(Boolean lastRegistrationSuccess) {
        this.lastRegistrationSuccess = lastRegistrationSuccess;
    }

    public String getLastRegistrationAt() {
        return lastRegistrationAt;
    }

    public void setLastRegistrationAt(String lastRegistrationAt) {
        this.lastRegistrationAt = lastRegistrationAt;
    }

    public String getLastRegistrationMessage() {
        return lastRegistrationMessage;
    }

    public void setLastRegistrationMessage(String lastRegistrationMessage) {
        this.lastRegistrationMessage = lastRegistrationMessage;
    }
}
