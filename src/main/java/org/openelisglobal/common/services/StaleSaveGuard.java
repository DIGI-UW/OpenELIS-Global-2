package org.openelisglobal.common.services;

import jakarta.persistence.OptimisticLockException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.StaleStateException;
import org.openelisglobal.audittrail.dao.HistoryDAO;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Optimistic-concurrency check for editors that save a whole record, following
 * Results Entry's stale save (OGC-1376). The editor sends the version token it
 * loaded; a save against a newer version is refused with 409, naming who saved
 * last and when, and nothing is written. The stale editor always loses.
 */
@Component
public class StaleSaveGuard {

    private final HistoryDAO historyDAO;
    private final SystemUserService systemUserService;
    private final ReferenceTablesService referenceTablesService;

    public StaleSaveGuard(HistoryDAO historyDAO, SystemUserService systemUserService,
            ReferenceTablesService referenceTablesService) {
        this.historyDAO = historyDAO;
        this.systemUserService = systemUserService;
        this.referenceTablesService = referenceTablesService;
    }

    /** The token an editor keeps from its load: the version as epoch millis. */
    public static String token(Timestamp lastupdated) {
        return lastupdated == null ? null : String.valueOf(lastupdated.getTime());
    }

    /**
     * True when the editor sent a token and it is not the stored version. A missing
     * token is not stale, so a caller that never loaded one keeps working. Epoch
     * millis and {@link Timestamp#toString()} forms are both read; anything else is
     * stale.
     */
    public static boolean isStale(String clientToken, Timestamp current) {
        if (current == null || GenericValidator.isBlankOrNull(clientToken)) {
            return false;
        }
        return parse(clientToken.trim()) != current.getTime();
    }

    /**
     * True when {@code e}, or anything that caused it, is Hibernate's or JPA's
     * optimistic-lock failure: the save lost a race with another one after the
     * token check.
     */
    public static boolean isOptimisticLockFailure(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (cause instanceof OptimisticLockException || cause instanceof StaleStateException
                    || cause instanceof OptimisticLockingFailureException) {
                return true;
            }
        }
        return false;
    }

    private static long parse(String token) {
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException notEpoch) {
            try {
                return Timestamp.valueOf(token).getTime();
            } catch (IllegalArgumentException notTimestamp) {
                return Long.MIN_VALUE;
            }
        }
    }

    /**
     * The 409 for a stale save. {@code messageKey} and {@code messageArgs} are what
     * the screens translate ("updated by {0} at {1}"); {@code lastupdated} is the
     * current token.
     */
    public ResponseEntity<Map<String, Object>> conflict(String messageKey, String tableName, String recordId,
            Timestamp current) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictBody(messageKey, tableName, recordId, current));
    }

    /**
     * The body of {@link #conflict}, for endpoints that answer with their own DTO.
     */
    public Map<String, Object> conflictBody(String messageKey, String tableName, String recordId, Timestamp current) {
        String modifiedBy = lastModifier(tableName, recordId);
        String modifiedAt = current == null ? "" : DateUtil.convertTimestampToStringDateAndConfiguredHourTime(current);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("0", modifiedBy);
        args.put("1", modifiedAt);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", messageKey);
        body.put("messageKey", messageKey);
        body.put("messageArgs", args);
        body.put("modifiedBy", modifiedBy);
        body.put("modifiedAt", modifiedAt);
        body.put("lastupdated", token(current));
        return body;
    }

    /** Display name of whoever wrote the record's latest audit row. */
    public String lastModifier(String tableName, String recordId) {
        try {
            ReferenceTables table = referenceTablesService.getReferenceTableByName(tableName);
            if (table != null && !GenericValidator.isBlankOrNull(recordId)) {
                List<History> rows = historyDAO.getHistoryByRefIdAndRefTableId(recordId, table.getId());
                History latest = null;
                for (History row : rows) {
                    if (latest == null || (row.getTimestamp() != null && latest.getTimestamp() != null
                            && row.getTimestamp().after(latest.getTimestamp()))) {
                        latest = row;
                    }
                }
                if (latest != null) {
                    SystemUser user = systemUserService.getUserById(latest.getSysUserId());
                    if (user != null) {
                        return user.getDisplayName();
                    }
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
        return MessageUtil.getMessage("label.results.anotherUser");
    }
}
