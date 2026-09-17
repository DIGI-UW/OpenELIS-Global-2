package org.openelisglobal.reports.vectorsurveillance.valueholder;

/** A sampling-site filter option for the dashboard (US2). */
public class SiteOption {

    private Integer id;
    private String code;
    private String name;

    public SiteOption() {
    }

    public SiteOption(Integer id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
