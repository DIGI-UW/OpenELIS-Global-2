package org.openelisglobal.inventory.imports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.CsvParsingUtil;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.inventory.imports.InventoryImportPlan.Outcome;
import org.openelisglobal.inventory.imports.InventoryImportPlan.RowPlan;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryImportServiceImpl implements InventoryImportService {

    private static final String NAME = "name";
    private static final String UNITS = "units";
    private static final String TAGS = "tags";
    private static final String UPC = "upc";
    private static final String MANUFACTURER = "manufacturer";
    private static final String CATALOG_NUMBER = "catalog number";
    private static final String REORDER_THRESHOLD = "reorder threshold";
    private static final String LEAD_TIME_DAYS = "lead time days";
    private static final String TRACK_LOTS = "track lots";

    /**
     * Name and units are the two an item cannot be defined without: units is NOT
     * NULL in the schema, and name is what a row is matched on when it carries no
     * UPC.
     */
    private static final List<String> REQUIRED_COLUMNS = List.of(NAME, UNITS);

    private static final List<String> COLUMNS = List.of(NAME, UNITS, TAGS, UPC, MANUFACTURER, CATALOG_NUMBER,
            REORDER_THRESHOLD, LEAD_TIME_DAYS, TRACK_LOTS);

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public String template() {
        return String.join(",", COLUMNS) + "\n"
                + "Malaria RDT (P.f/P.v),tests,\"RDT;Malaria\",00312345678906,Acme Diagnostics,MAL-100,25,14,Y\n";
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryImportPlan preview(String csv) {
        return run(csv, null, null);
    }

    /**
     * Not transactional itself: each row commits in its own transaction below.
     *
     * <p>
     * One transaction around the whole file could not survive a row the database
     * rejected. An over-long free-text cell or a negative number fails at flush,
     * which marks the transaction rollback-only; the loop went on recording the
     * remaining rows as written, and the commit at the end then threw and discarded
     * every one of them. The user saw a 500 naming no row, and the preview could
     * not warn about it because a preview never reaches the database.
     */
    @Override
    public InventoryImportPlan apply(String csv, String sysUserId) {
        return run(csv, sysUserId, new RowTransactionRunner(transactionManager));
    }

    /**
     * One pass over the file. The runner is the only difference between a preview
     * and a commit, so a row cannot be judged one way and written another.
     *
     * @param rowRunner a transaction per row when writing, null when previewing
     */
    private InventoryImportPlan run(String csv, String sysUserId, RowTransactionRunner rowRunner) {
        boolean write = rowRunner != null;
        List<RowPlan> rows = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int skipped = 0;

        List<String> lines = csv == null ? List.of() : Arrays.asList(csv.split("\\r?\\n"));
        Map<String, Integer> columns = null;
        int headerLine = 0;

        // A file can name the same item twice, and the second row would otherwise
        // race the first through the unique index — or quietly overwrite it.
        Set<String> seenKeys = new LinkedHashSet<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;
            if (CsvParsingUtil.isSkippableLine(line)) {
                continue;
            }
            String[] values = CsvParsingUtil.parseCsvLine(line);

            if (columns == null) {
                columns = CsvParsingUtil.createColumnMap(values);
                headerLine = lineNumber;
                List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !hasColumn(values, c))
                        .collect(Collectors.toList());
                if (!missing.isEmpty()) {
                    rows.add(new RowPlan(headerLine, "", Outcome.SKIP,
                            "The file needs a column for " + String.join(" and ", missing)));
                    return new InventoryImportPlan(0, 0, 0, 1, rows);
                }
                continue;
            }

            CsvRow row = new CsvRow(columns, values, lineNumber);
            try {
                RowPlan plan = write ? rowRunner.run(() -> evaluate(row, seenKeys, sysUserId, true))
                        : evaluate(row, seenKeys, sysUserId, false);
                rows.add(plan);
                switch (plan.outcome()) {
                case CREATE:
                    created++;
                    break;
                case UPDATE:
                    updated++;
                    break;
                case UNCHANGED:
                    unchanged++;
                    break;
                default:
                    skipped++;
                    break;
                }
            } catch (RuntimeException e) {
                rows.add(new RowPlan(lineNumber, row.get(NAME), Outcome.SKIP, reason(e)));
                skipped++;
            }
        }

        if (columns == null) {
            rows.add(new RowPlan(0, "", Outcome.SKIP, "The file has no header row"));
            skipped++;
        }
        return new InventoryImportPlan(created, updated, unchanged, skipped, rows);
    }

    private boolean hasColumn(String[] headers, String name) {
        return CsvParsingUtil.findColumn(headers, name) >= 0;
    }

    private RowPlan evaluate(CsvRow row, Set<String> seenKeys, String sysUserId, boolean write) {
        String name = row.get(NAME);
        if (name.isEmpty()) {
            return new RowPlan(row.lineNumber(), "", Outcome.SKIP, "A row needs a name");
        }
        String units = row.get(UNITS);
        if (units.isEmpty()) {
            // The column is NOT NULL, and letting it through turns a readable row
            // error into a constraint violation at flush with no line number on it.
            return new RowPlan(row.lineNumber(), name, Outcome.SKIP, "A row needs units");
        }

        // A blank UPC has to reach the column as null: the unique index is partial
        // on "not null", so a second empty string would collide with the first.
        String upc = row.get(UPC).trim();
        String key = upc.isEmpty() ? "name:" + name.toLowerCase() : "upc:" + upc;
        if (!seenKeys.add(key)) {
            return new RowPlan(row.lineNumber(), name, Outcome.SKIP, "The file already has a row for this item");
        }

        InventoryItem existing = upc.isEmpty() ? inventoryItemService.getByExactName(name)
                : inventoryItemService.getByUpc(upc);

        // Every row is read in full whether or not anything is written. Reading it
        // only on the way to a write would mean a badly typed number previewed as a
        // clean create and skipped on apply — the two answers a preview exists to
        // stop differing.
        InventoryItem candidate = existing == null ? new InventoryItem() : copyOf(existing);
        applyTo(candidate, row, name, units, upc);

        if (existing == null) {
            if (write) {
                candidate.setFhirUuid(UUID.randomUUID());
                candidate.setSysUserId(sysUserId);
                inventoryItemService.insert(candidate);
            }
            return new RowPlan(row.lineNumber(), name, Outcome.CREATE, "");
        }

        if (!differs(existing, candidate)) {
            return new RowPlan(row.lineNumber(), name, Outcome.UNCHANGED, "");
        }
        if (write) {
            applyTo(existing, row, name, units, upc);
            existing.setSysUserId(sysUserId);
            inventoryItemService.update(existing);
        }
        return new RowPlan(row.lineNumber(), name, Outcome.UPDATE, "");
    }

    /**
     * A row only ever sets the cells it actually fills. A column the file omits,
     * and a cell left empty, both leave that field alone rather than blanking it,
     * so a partial file is a partial update and not a demolition.
     *
     * <p>
     * Empty used to count as a value. Re-importing a corrected file whose reorder
     * threshold column was blank wiped the threshold on every item it touched,
     * which turns low-stock detection off for them silently — and the modal
     * promises the opposite, that re-importing updates the same items.
     */
    private void applyTo(InventoryItem item, CsvRow row, String name, String units, String upc) {
        item.setName(name);
        item.setUnits(units);
        if (!row.isBlank(UPC)) {
            item.setUpc(upc);
        }
        if (!row.isBlank(TAGS)) {
            item.setTags(parseTags(row.get(TAGS)));
        }
        if (!row.isBlank(MANUFACTURER)) {
            item.setManufacturer(row.get(MANUFACTURER));
        }
        if (!row.isBlank(CATALOG_NUMBER)) {
            item.setCatalogNumber(row.get(CATALOG_NUMBER));
        }
        if (!row.isBlank(REORDER_THRESHOLD)) {
            item.setLowStockThreshold(row.integer(REORDER_THRESHOLD));
        }
        if (!row.isBlank(LEAD_TIME_DAYS)) {
            item.setLeadTimeDays(row.integer(LEAD_TIME_DAYS));
        }
        if (!row.isBlank(TRACK_LOTS)) {
            item.setTrackLots(row.flag(TRACK_LOTS, false) ? "Y" : "N");
        }
    }

    /**
     * Semicolons, not commas: a comma would need the whole cell quoted, and a
     * hand-edited file is where this format is typed.
     */
    private Set<String> parseTags(String value) {
        return Arrays.stream(value.split(";")).map(String::trim).filter(tag -> !tag.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Enough of an item to tell whether a row would change anything. */
    private InventoryItem copyOf(InventoryItem source) {
        InventoryItem copy = new InventoryItem();
        copy.setName(source.getName());
        copy.setUnits(source.getUnits());
        copy.setUpc(source.getUpc());
        copy.setTags(source.getTags() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(source.getTags()));
        copy.setManufacturer(source.getManufacturer());
        copy.setCatalogNumber(source.getCatalogNumber());
        copy.setLowStockThreshold(source.getLowStockThreshold());
        copy.setLeadTimeDays(source.getLeadTimeDays());
        copy.setTrackLots(source.getTrackLots());
        return copy;
    }

    /**
     * Tags are compared case-insensitively because the service canonicalises them
     * on write: a row saying "rdt" against a stored "RDT" is not a change, and
     * reporting it as one would make every re-import look like an update.
     */
    private boolean differs(InventoryItem existing, InventoryItem candidate) {
        return !Objects.equals(existing.getName(), candidate.getName())
                || !Objects.equals(existing.getUnits(), candidate.getUnits())
                || !Objects.equals(existing.getUpc(), candidate.getUpc())
                || !Objects.equals(existing.getManufacturer(), candidate.getManufacturer())
                || !Objects.equals(existing.getCatalogNumber(), candidate.getCatalogNumber())
                || !Objects.equals(existing.getLowStockThreshold(), candidate.getLowStockThreshold())
                || !Objects.equals(existing.getLeadTimeDays(), candidate.getLeadTimeDays())
                || !Objects.equals(existing.getTrackLots(), candidate.getTrackLots())
                || !lowered(existing.getTags()).equals(lowered(candidate.getTags()));
    }

    private Set<String> lowered(Set<String> tags) {
        return tags == null ? Set.of() : tags.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    /** The deepest message in the chain, so a row shows the real complaint. */
    private String reason(Throwable e) {
        Throwable current = e;
        String message = null;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return message == null ? e.getClass().getSimpleName() : message;
    }
}
