package org.openelisglobal.reports.service;

public class MicroWhonetExportResult {

    private final String fileName;
    private final byte[] content;

    public MicroWhonetExportResult(String fileName, byte[] content) {
        this.fileName = fileName;
        this.content = content.clone();
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getContent() {
        return content.clone();
    }
}
