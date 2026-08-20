package org.openelisglobal.microbiology.form;

public class MicroReferenceAdminQueryForm {
    public String q;
    public String status;
    public String category;
    public String workflow;
    public String authority;
    public String organism;
    public String antibiotic;
    public String method;
    public String specimenTypeId;
    public String sort = "name";
    public int page = 1;
    public int pageSize = 20;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getAntibiotic() {
        return antibiotic;
    }

    public void setAntibiotic(String antibiotic) {
        this.antibiotic = antibiotic;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSpecimenTypeId() {
        return specimenTypeId;
    }

    public void setSpecimenTypeId(String specimenTypeId) {
        this.specimenTypeId = specimenTypeId;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
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
