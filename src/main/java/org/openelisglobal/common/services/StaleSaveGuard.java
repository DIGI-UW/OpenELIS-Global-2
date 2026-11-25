package org.openelisglobal.common.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.OptimisticLockException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HexFormat;
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

    private static final ObjectMapper CANONICAL = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .findAndAddModules().build();

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
     * The version token of something an editor saves as a whole list (components,
     * mappings, members), where no single row carries a version: a digest of what
     * the editor's load returned. Built from the same view before a save, it
     * differs when anyone changed the list since that load (OGC-1463). Lists are
     * compared as sets, so a load that returns rows in another order is not a
     * change; a move shows in the rows' own order fields.
     */
    public static String versionOf(Object loaded) {
        try {
            String canonical = CANONICAL.writeValueAsString(unordered(CANONICAL.valueToTree(loaded)));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("cannot compute a version token", e);
        }
    }

    private static JsonNode unordered(JsonNode node) throws JsonProcessingException {
        if (node.isArray()) {
            List<String> elements = new ArrayList<>();
            for (JsonNode element : node) {
                elements.add(CANONICAL.writeValueAsString(unordered(element)));
            }
            elements.sort(null);
            ArrayNode sorted = CANONICAL.createArrayNode();
            for (String element : elements) {
                sorted.add(CANONICAL.readTree(element));
            }
            return sorted;
        }
        if (node.isObject()) {
            ObjectNode copy = CANONICAL.createObjectNode();
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.sort(null);
            for (String name : names) {
                copy.set(name, unordered(node.get(name)));
            }
            return copy;
        }
        return node;
    }

    /**
     * True when the editor sent a list version and it is not the current one. A
     * missing version is not stale, as for {@link #isStale}.
     */
    public static boolean isStaleVersion(String clientVersion, String currentVersion) {
        if (GenericValidator.isBlankOrNull(clientVersion)) {
            return false;
        }
        return !clientVersion.trim().equals(currentVersion);
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
