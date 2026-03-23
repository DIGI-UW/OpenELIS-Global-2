package org.openelisglobal.inventory.service;

public interface InventoryReportService {

    GeneratedReport generateReport(String reportType, String exportFormat, String startDate, String endDate,
            boolean includeInactive, boolean includeExpired, boolean groupByType, boolean groupByLocation);

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
