package org.openelisglobal.qaevent.form;

import java.util.List;

/**
 * Form class for NCE badge responses. Maps to the NCEBadgeResponse schema in
 * the OpenAPI specification.
 */
public class NCEBadgeResponseForm {

    private boolean hasNCE;
    private int nceCount;
    private String highestSeverity;
    private List<String> nceNumbers;
    private String badgeColor;

    public NCEBadgeResponseForm() {
        this.hasNCE = false;
        this.nceCount = 0;
    }

    public NCEBadgeResponseForm(boolean hasNCE, int nceCount, String highestSeverity, List<String> nceNumbers,
            String badgeColor) {
        this.hasNCE = hasNCE;
        this.nceCount = nceCount;
        this.highestSeverity = highestSeverity;
        this.nceNumbers = nceNumbers;
        this.badgeColor = badgeColor;
    }

    public boolean isHasNCE() {
        return hasNCE;
    }

    public void setHasNCE(boolean hasNCE) {
        this.hasNCE = hasNCE;
    }

    public int getNceCount() {
        return nceCount;
    }

    public void setNceCount(int nceCount) {
        this.nceCount = nceCount;
    }

    public String getHighestSeverity() {
        return highestSeverity;
    }

    public void setHighestSeverity(String highestSeverity) {
        this.highestSeverity = highestSeverity;
    }

    public List<String> getNceNumbers() {
        return nceNumbers;
    }

    public void setNceNumbers(List<String> nceNumbers) {
        this.nceNumbers = nceNumbers;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public void setBadgeColor(String badgeColor) {
        this.badgeColor = badgeColor;
    }

    /**
     * Convenience method to get badge color based on severity
     */
    public static String getBadgeColorForSeverity(String severity) {
        if (severity == null) {
            return null;
        }

        switch (severity.toLowerCase()) {
        case "critical":
            return "red";
        case "major":
            return "magenta";
        case "minor":
            return "teal";
        default:
            return "gray";
        }
    }

    /**
     * Check if this result has critical NCEs
     */
    public boolean hasCriticalNCEs() {
        return InlineNCERequestForm.Severity.CRITICAL.getDisplayName().equalsIgnoreCase(highestSeverity);
    }

    /**
     * Check if this result has high priority NCEs
     */
    public boolean hasHighPriorityNCEs() {
        return InlineNCERequestForm.Severity.CRITICAL.getDisplayName().equalsIgnoreCase(highestSeverity)
                || InlineNCERequestForm.Severity.MAJOR.getDisplayName().equalsIgnoreCase(highestSeverity);
    }

    @Override
    public String toString() {
        return "NCEBadgeResponseForm{" + "hasNCE=" + hasNCE + ", nceCount=" + nceCount + ", highestSeverity='"
                + highestSeverity + '\'' + ", nceNumbers=" + nceNumbers + ", badgeColor='" + badgeColor + '\'' + '}';
    }
}