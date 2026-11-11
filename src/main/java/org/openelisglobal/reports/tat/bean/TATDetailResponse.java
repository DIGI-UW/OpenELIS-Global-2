package org.openelisglobal.reports.tat.bean;

import java.util.List;

public class TATDetailResponse {

    private int totalCount;
    private int page;
    private int pageSize;
    private String calculationMode;
    private List<TATResult> results;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
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

    public String getCalculationMode() {
        return calculationMode;
    }

    public void setCalculationMode(String calculationMode) {
        this.calculationMode = calculationMode;
    }

    public List<TATResult> getResults() {
        return results;
    }

    public void setResults(List<TATResult> results) {
        this.results = results;
    }
}
