package org.openelisglobal.organization.locations;

import java.util.List;
import java.util.Map;
import org.openelisglobal.organization.locations.LocationsApi.Usage;
import org.openelisglobal.organization.service.OrganizationChangeService.FieldChange;

/**
 * OGC-1363 (sections F, G, M): the shapes of the CSV import preview and apply.
 * A preview is the outcome, not an estimate: every row is evaluated against the
 * current data without saving anything.
 */
public final class LocationsImportApi {

    private LocationsImportApi() {
    }

    public static final String MODE_MERGE = "merge";
    public static final String MODE_REPLACE = "replace";

    public static final String AREA_ORGANIZATIONS = "organizations";
    public static final String AREA_LEVELS = "levels";
    public static final String AREA_VALUES = "values";

    public static final String OUTCOME_NEW = "new";
    public static final String OUTCOME_UPDATED = "updated";
    public static final String OUTCOME_UNCHANGED = "unchanged";
    public static final String OUTCOME_REACTIVATED = "reactivated";
    public static final String OUTCOME_DECISION = "decision";
    public static final String OUTCOME_RENAME = "rename";
    public static final String OUTCOME_REJECTED = "rejected";
    public static final String OUTCOME_SKIPPED = "skipped";

    public static final String CHOICE_NEW = "new";
    public static final String CHOICE_SKIP = "skip";
    public static final String CHOICE_USE = "use:";
    public static final String RENAME_SAME = "same";
    public static final String RENAME_DIFFERENT = "different";

    public static final String ALIAS_TYPE = "ORGANIZATION";

    public record Candidate(String id, String name, String code, String parent, boolean active, int inUse) {
    }

    public record PlanRow(String file, int line, String outcome, String type, String code, String name, String parent,
            String reason, List<FieldChange> diffs, List<Candidate> candidates, Candidate pair, boolean registry,
            String targetId) {
    }

    public record Deactivation(String id, String name, String code, String location, Usage inUse) {
    }

    public record Scope(String text, List<String> types, List<String> untouched, List<String> wardParents) {
    }

    /**
     * {@code ignoredColumns} names the columns of the files that the importer does
     * not know and left out.
     */
    public record Plan(String importRunId, String mode, Scope scope, Map<String, Integer> counts, List<PlanRow> rows,
            List<Deactivation> deactivations, int unresolvedCount, List<String> errors, List<String> ignoredColumns) {
    }

    public record Decision(String choice, boolean remember) {
    }

    public record Options(String mode, Map<String, Decision> decisions, Map<String, String> renames) {

        public static Options merge() {
            return new Options(MODE_MERGE, Map.of(), Map.of());
        }

        public boolean replace() {
            return MODE_REPLACE.equalsIgnoreCase(mode);
        }

        public Decision decisionFor(int line) {
            return decisions == null ? null : decisions.get(String.valueOf(line));
        }

        public String renameFor(int line) {
            return renames == null ? null : renames.get(String.valueOf(line));
        }
    }

    /**
     * {@code files} names the run's files; {@code applied} is false for a preview.
     */
    public record RecentRun(String id, String startedAt, String finishedAt, String user, String mode, String summary,
            String status, List<String> files, boolean applied) {
    }
}
