package org.openelisglobal.reports.service;

public class MicroWhonetExportResult {

    public final String fileName;
    public final byte[] content;

    public MicroWhonetExportResult(String fileName, byte[] content) {
        this.fileName = fileName;
        this.content = content.clone();
    }
}
