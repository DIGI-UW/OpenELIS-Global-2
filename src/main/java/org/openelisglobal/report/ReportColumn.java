package org.openelisglobal.report;

import java.io.Serializable;

/**
 * Data Transfer Object representing a column definition in a report.
 */
public class ReportColumn implements Serializable {

    private static final long serialVersionUID = 1L;

    private String header;
    private String type;
    private String key;

    public ReportColumn() {
    }

    public ReportColumn(String key, String header, String type) {
        this.key = key;
        this.header = header;
        this.type = type;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
