package org.openelisglobal.reports.service;

public class MicroWhonetExportResult {

    public final String runId;
    public final String fileName;
    public final byte[] content;

    public MicroWhonetExportResult(String runId, String fileName, byte[] content) {
        this.runId = runId;
        this.fileName = fileName;
        this.content = content.clone();
    }
}
