package org.openelisglobal.program.valueholder.pathology;

import java.sql.Timestamp;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * A stored report version as the case view lists it: never the file itself,
 * which is read at its own address.
 *
 * @param state {@code CURRENT}, {@code SUPERSEDED} or {@code VOIDED}, worked
 *              out from the case's rows and never stored
 */
public record ReportView(Integer id, Integer version, String type, String state, String generatedAt, String generatedBy,
        String fileType) {

    public static final String CURRENT = "CURRENT";
    public static final String SUPERSEDED = "SUPERSEDED";
    public static final String VOIDED = "VOIDED";

    /**
     * Called inside the transaction that loaded the row, since its author is an
     * association.
     */
    public static ReportView of(PathologyReport row, String state) {
        return new ReportView(row.getId(), row.getVersionNumber(), row.getReportType().name(), state,
                formatted(row.getGeneratedAt()), nameOf(row.getGeneratedBy()), row.getFileType());
    }

    private static String formatted(Timestamp timestamp) {
        return timestamp == null ? null : DateUtil.convertTimestampToStringDateAndConfiguredHourTime(timestamp);
    }

    private static String nameOf(SystemUser user) {
        return user == null ? "" : user.getDisplayName();
    }
}
