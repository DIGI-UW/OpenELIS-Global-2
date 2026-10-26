package org.openelisglobal.delivery.service;

import java.util.ArrayList;
import java.util.List;

/** The lab-wide delivery settings (FR-A16), as stored in site_information. */
public class DeliverySettings {

    public static final List<Integer> DEFAULT_RETRY_DELAYS = List.of(1, 5, 15);

    private List<Integer> retryDelays = new ArrayList<>(DEFAULT_RETRY_DELAYS);
    private String defaultFormat = "MASKED";
    private int holdMinutes = 10;
    private boolean sendPartials = true;
    private String labContact;
    private String location = "LOCAL";
    private String centralAddress;
    private boolean secureLinks;
    private int linkDays = 14;
    private String countryCode;

    public List<Integer> getRetryDelays() {
        return retryDelays;
    }

    public void setRetryDelays(List<Integer> retryDelays) {
        this.retryDelays = retryDelays == null ? new ArrayList<>() : new ArrayList<>(retryDelays);
    }

    public String getDefaultFormat() {
        return defaultFormat;
    }

    public void setDefaultFormat(String defaultFormat) {
        this.defaultFormat = defaultFormat;
    }

    public int getHoldMinutes() {
        return holdMinutes;
    }

    public void setHoldMinutes(int holdMinutes) {
        this.holdMinutes = holdMinutes;
    }

    public boolean isSendPartials() {
        return sendPartials;
    }

    public void setSendPartials(boolean sendPartials) {
        this.sendPartials = sendPartials;
    }

    public String getLabContact() {
        return labContact;
    }

    public void setLabContact(String labContact) {
        this.labContact = labContact;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCentralAddress() {
        return centralAddress;
    }

    public void setCentralAddress(String centralAddress) {
        this.centralAddress = centralAddress;
    }

    public boolean isSecureLinks() {
        return secureLinks;
    }

    public void setSecureLinks(boolean secureLinks) {
        this.secureLinks = secureLinks;
    }

    public int getLinkDays() {
        return linkDays;
    }

    public void setLinkDays(int linkDays) {
        this.linkDays = linkDays;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
