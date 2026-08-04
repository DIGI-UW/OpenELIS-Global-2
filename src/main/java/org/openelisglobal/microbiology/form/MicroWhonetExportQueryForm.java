package org.openelisglobal.microbiology.form;

public class MicroWhonetExportQueryForm {

    public String from;
    public String to;
    public String significance = "CLINICALLY_SIGNIFICANT";
    public String dedup = "FIRST_ISOLATE_7_DAY";
    public int page = 1;
    public int pageSize = 20;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSignificance() {
        return significance;
    }

    public void setSignificance(String significance) {
        this.significance = significance;
    }

    public String getDedup() {
        return dedup;
    }

    public void setDedup(String dedup) {
        this.dedup = dedup;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
