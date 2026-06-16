package org.openelisglobal.compliance.controller.rest.dto;

import java.util.List;

public class PagedExceedanceDTO {
    private int totalCount;
    private int page;
    private int pageSize;
    private List<ExceedanceItemDTO> items;

    public static class ExceedanceItemDTO {
        private String date;
        private String labNumber;
        private String siteId;
        private String siteName;
        private String parameter;
        private String result;
        private String threshold;
        private String status;

        public String getDate() {
            return date;
        }

        public void setDate(String v) {
            date = v;
        }

        public String getLabNumber() {
            return labNumber;
        }

        public void setLabNumber(String v) {
            labNumber = v;
        }

        public String getSiteId() {
            return siteId;
        }

        public void setSiteId(String v) {
            siteId = v;
        }

        public String getSiteName() {
            return siteName;
        }

        public void setSiteName(String v) {
            siteName = v;
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String v) {
            parameter = v;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String v) {
            result = v;
        }

        public String getThreshold() {
            return threshold;
        }

        public void setThreshold(String v) {
            threshold = v;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String v) {
            status = v;
        }
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int v) {
        totalCount = v;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int v) {
        page = v;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int v) {
        pageSize = v;
    }

    public List<ExceedanceItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ExceedanceItemDTO> v) {
        items = v;
    }
}
