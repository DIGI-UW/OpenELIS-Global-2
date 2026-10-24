package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroAstPanelAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAntibioticAdminForm;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.springframework.stereotype.Component;

/**
 * A laboratory's AST panels: one row per panel drug, grouped by panel name. A
 * changed panel is published as a new version; an unchanged one is left alone.
 * {@code defaultFor} lists, separated by semicolons, the organisms the panel is
 * the default for: a species name, a genus, or an organism group. When several
 * panels claim an organism, the most specific entry wins (species, then genus,
 * then group).
 */
@Component
public class MicroAstPanelConfigurationHandler extends AbstractCatalogCsvHandler {
    private final MicrobiologyReferenceAdminService admin;
    private final MicroAstPanelDAO panels;
    private final MicroAntibioticDAO antibiotics;
    private final MicroOrganismDAO organisms;

    public MicroAstPanelConfigurationHandler(MicrobiologyReferenceAdminService admin, MicroAstPanelDAO panels,
            MicroAntibioticDAO antibiotics, MicroOrganismDAO organisms) {
        this.admin = admin;
        this.panels = panels;
        this.antibiotics = antibiotics;
        this.organisms = organisms;
    }

    @Override
    public String getDomainName() {
        return "microbiology-ast-panels";
    }

    @Override
    public int getLoadOrder() {
        return 340;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "panelName", "antibioticWhonetCode" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        Map<String, List<CsvRow>> byPanel = new LinkedHashMap<>();
        for (CsvRow row : rows) {
            byPanel.computeIfAbsent(required(row, "panelName").toLowerCase(Locale.ROOT), key -> new ArrayList<>())
                    .add(row);
        }
        Map<String, List<String>> defaults = new LinkedHashMap<>();
        for (List<CsvRow> panelRows : byPanel.values()) {
            LoadedRow<String> outcome = transaction.run(() -> loadPanel(panelRows));
            for (CsvRow row : panelRows) {
                summary.record(outcome, getClass().getSimpleName(), row.lineNumber());
            }
            flushUnresolvedReferences(fileName, panelRows.get(0).lineNumber());
            String claims = sameValue(panelRows, "defaultFor", "");
            if (!claims.isBlank()) {
                MicroAstPanel current = panels.findCurrentByNameIgnoreCase(required(panelRows.get(0), "panelName"));
                if (current != null) {
                    defaults.put(current.getId(), List.of(claims.split("\\s*;\\s*")));
                }
            }
        }
        if (!defaults.isEmpty()) {
            transaction.run(() -> assignDefaults(defaults));
        }
    }

    private int assignDefaults(Map<String, List<String>> claimsByPanel) {
        int changed = 0;
        for (var organism : organisms.getActiveOrganisms()) {
            String best = null;
            int bestRank = 0;
            for (var claim : claimsByPanel.entrySet()) {
                for (String entry : claim.getValue()) {
                    int rank = specificity(entry, organism.getDisplayName(), organism.getOrganismGroup());
                    if (rank > bestRank) {
                        bestRank = rank;
                        best = claim.getKey();
                    }
                }
            }
            if (best != null && !best.equals(organism.getDefaultAstPanelId())) {
                organism.setDefaultAstPanelId(best);
                organism.setLastUpdatedBy(SYS_USER_ID);
                organisms.update(organism);
                changed++;
            }
        }
        return changed;
    }

    /**
     * 3 for the species itself, 2 for its genus, 1 for its organism group, 0
     * otherwise.
     */
    static int specificity(String entry, String organismName, String organismGroup) {
        if (entry.isBlank() || organismName == null) {
            return 0;
        }
        String name = organismName.trim();
        if (name.equalsIgnoreCase(entry)) {
            return 3;
        }
        if (name.toLowerCase(Locale.ROOT).startsWith(entry.toLowerCase(Locale.ROOT) + " ")) {
            return 2;
        }
        return organismGroup != null && organismGroup.equalsIgnoreCase(entry) ? 1 : 0;
    }

    private LoadedRow<String> loadPanel(List<CsvRow> rows) {
        CsvRow first = rows.get(0);
        MicroAstPanelAdminForm form = new MicroAstPanelAdminForm();
        form.name = required(first, "panelName");
        form.susceptibilityProtocol = sameValue(rows, "susceptibilityProtocol", "GENERAL").toUpperCase(Locale.ROOT);
        for (CsvRow row : rows) {
            String code = required(row, "antibioticWhonetCode");
            var antibiotic = antibiotics.findByWhonetCodeIgnoreCase(code).orElseThrow(
                    () -> new IllegalArgumentException("Line " + row.lineNumber() + ": unknown antibiotic " + code));
            MicroAstPanelAntibioticAdminForm drug = new MicroAstPanelAntibioticAdminForm();
            drug.antibioticId = antibiotic.getId();
            drug.diskContent = blankToNull(row.get("diskContent"));
            drug.tier = row.isBlank("tier") ? 1 : row.integer("tier");
            drug.reportBehavior = row.isBlank("reportBehavior") ? "ALWAYS"
                    : row.get("reportBehavior").trim().toUpperCase(Locale.ROOT);
            form.antibiotics.add(drug);
        }

        MicroAstPanel current = panels.findCurrentByNameIgnoreCase(form.name);
        if (current == null) {
            return LoadedRow.created(admin.createPanel(form, SYS_USER_ID).id);
        }
        if (sameContent(admin.getAstPanel(current.getId()), form)) {
            return LoadedRow.skipped("Panel unchanged");
        }
        return LoadedRow.updated(admin.publishPanelVersion(current.getId(), form, SYS_USER_ID).id);
    }

    private static boolean sameContent(MicroAstPanelAdminForm existing, MicroAstPanelAdminForm wanted) {
        if (!Objects.equals(existing.susceptibilityProtocol, wanted.susceptibilityProtocol)
                || !Objects.equals(existing.organismGroup, wanted.organismGroup) || !existing.active
                || existing.antibiotics.size() != wanted.antibiotics.size()) {
            return false;
        }
        for (int i = 0; i < wanted.antibiotics.size(); i++) {
            var a = existing.antibiotics.get(i);
            var b = wanted.antibiotics.get(i);
            if (!Objects.equals(a.antibioticId, b.antibioticId) || !Objects.equals(a.diskContent, b.diskContent)
                    || !Objects.equals(a.tier, b.tier) || !Objects.equals(a.reportBehavior, b.reportBehavior)) {
                return false;
            }
        }
        return true;
    }

    /** Panel-level columns may repeat on every row but must agree. */
    private static String sameValue(List<CsvRow> rows, String column, String fallback) {
        String value = null;
        for (CsvRow row : rows) {
            String cell = row.isBlank(column) ? null : row.get(column).trim();
            if (cell == null) {
                continue;
            }
            if (value != null && !value.equalsIgnoreCase(cell)) {
                throw new IllegalArgumentException(
                        "Line " + row.lineNumber() + ": " + column + " differs within panel " + row.get("panelName"));
            }
            value = cell;
        }
        return value == null ? fallback : value;
    }

    private static String required(CsvRow row, String column) {
        if (row.isBlank(column)) {
            throw new IllegalArgumentException("Line " + row.lineNumber() + ": " + column + " is required");
        }
        return row.get(column).trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
