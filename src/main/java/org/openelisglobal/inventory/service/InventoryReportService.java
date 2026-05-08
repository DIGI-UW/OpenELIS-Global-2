package org.openelisglobal.inventory.service;

import jakarta.servlet.http.HttpServletRequest;

public interface InventoryReportService {

    default GeneratedReport generateReport(String reportType, String exportFormat, String startDate, String endDate,
            boolean includeInactive, boolean includeExpired, boolean groupByType, boolean groupByLocation) {
        return generateReport(reportType, exportFormat, startDate, endDate, includeInactive, includeExpired,
                groupByType, groupByLocation, null);
    }

    GeneratedReport generateReport(String reportType, String exportFormat, String startDate, String endDate,
            boolean includeInactive, boolean includeExpired, boolean groupByType, boolean groupByLocation,
            HttpServletRequest request);

    class GeneratedReport {
        private final byte[] content;
        private final String contentType;
        private final String fileName;

        public GeneratedReport(byte[] content, String contentType, String fileName) {
            this.content = content;
            this.contentType = contentType;
            this.fileName = fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
