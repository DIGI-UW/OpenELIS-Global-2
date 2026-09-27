package org.openelisglobal.reports.dataexport.form;

public record ExportJobView(String id, String state, String submittedAt, String startedAt, String completedAt,
        String expiresAt, Long rowCount, Long fileSize, String failureCode, String parentId, ExportSnapshot request) {
}
