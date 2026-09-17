package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AnalyzerSiteBindingSelectionRequest {

    @NotBlank
    private String siteBindingId;

    @Min(1)
    private int revision;

    @NotBlank
    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    private String bindingFingerprint;

    public String getSiteBindingId() {
        return siteBindingId;
    }

    public void setSiteBindingId(String siteBindingId) {
        this.siteBindingId = siteBindingId;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public String getBindingFingerprint() {
        return bindingFingerprint;
    }

    public void setBindingFingerprint(String bindingFingerprint) {
        this.bindingFingerprint = bindingFingerprint;
    }
}
