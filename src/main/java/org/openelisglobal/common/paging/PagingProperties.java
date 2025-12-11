package org.openelisglobal.common.paging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PagingProperties {
    @Value("${org.openelisglobal.paging.patients.pageSize:99}")
    private Integer patientsPageSize;

    @Value("${org.openelisglobal.paging.results.pageSize:99}")
    private Integer resultsPageSize;

    @Value("${org.openelisglobal.paging.validation.pageSize:99}")
    private Integer validationPageSize;

    @Value("${org.openelisglobal.paging.displaylist.pageSize:99}")
    private Integer displayListPageSize;

    @Value("${org.openelisglobal.paging.generic.pageSize:99}")
    private Integer genericPageSize;

    public Integer getPatientsPageSize() {
        return patientsPageSize;
    }

    public void setPatientsPageSize(Integer patientsPageSize) {
        this.patientsPageSize = patientsPageSize;
    }

    public Integer getResultsPageSize() {
        return resultsPageSize;
    }

    public void setResultsPageSize(Integer resultsPageSize) {
        this.resultsPageSize = resultsPageSize;
    }

    public Integer getValidationPageSize() {
        return validationPageSize;
    }

    public void setValidationPageSize(Integer validationPageSize) {
        this.validationPageSize = validationPageSize;
    }

    public Integer getDisplayListPageSize() {
        return displayListPageSize;
    }

    public void setDisplayListPageSize(Integer displayListPageSize) {
        this.displayListPageSize = displayListPageSize;
    }

    public Integer getGenericPageSize() {
        return genericPageSize;
    }

    public void setGenericPageSize(Integer genericPageSize) {
        this.genericPageSize = genericPageSize;
    }
}
