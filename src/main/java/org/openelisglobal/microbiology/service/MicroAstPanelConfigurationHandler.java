package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CatalogReferenceResolver;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroAstPanelAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAntibioticAdminForm;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.stereotype.Component;

/**
 * A laboratory's AST panels: one row per panel drug, grouped by panel name. A
 * changed panel is published as a new version; an unchanged one is left alone.
 * {@code defaultFor} lists, separated by semicolons, the organisms the panel is
 * the default for: a species name, a genus, or a laboratory organism group from
 * the organism groups file. When several panels, in any panel file, claim an
 * organism, the most specific entry wins (species, then genus, then group); two
 * equally specific entries from different panels fail the load.
 */
@Component
public class MicroAstPanelConfigurationHandler extends AbstractCatalogCsvHandler {
    private final MicrobiologyReferenceAdminService admin;
    private final MicroAstPanelDAO panels;
    private final MicroOrganismDAO organisms;
    private final CatalogReferenceResolver resolver;

    public MicroAstPanelConfigurationHandler(MicrobiologyReferenceAdminService admin, MicroAstPanelDAO panels,
            MicroOrganismDAO organisms, CatalogReferenceResolver resolver) {
        this.admin = admin;
        this.panels = panels;
        this.organisms = organisms;
        this.resolver = resolver;
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
        transaction.run(() -> {
            Map<String, Integer> claimLines = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            for (List<CsvRow> panelRows : byPanel.values()) {
                try {
                    LoadedRow<String> outcome = loadPanel(panelRows);
                    for (CsvRow row : panelRows) {
                        summary.record(outcome, getClass().getSimpleName(), row.lineNumber());
                        String behavior = row.get("reportBehavior").toUpperCase(Locale.ROOT);
                        if (!behavior.isEmpty() && !behavior.equals("ALWAYS")) {
                            summary.warned(getClass().getSimpleName(), row.lineNumber(), "reportBehavior " + behavior
                                    + " is not read from panel files: set it as a rule on the AST rules page");
                        }
                    }
                    String claims = blankToNull(sameValue(panelRows, "defaultFor", ""));
                    if (claims != null) {
                        MicroAstPanel current = panels
                                .findCurrentByNameIgnoreCase(required(panelRows.get(0), "panelName"));
                        claimLines.put(current.getId(), panelRows.get(0).lineNumber());
                        claim(current, claims, fileName);
                    }
                } catch (IllegalArgumentException e) {
                    errors.add(rowError(panelRows.get(0).lineNumber(), e));
                } finally {
                    flushUnresolvedReferences(fileName, panelRows.get(0).lineNumber());
                }
            }
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(String.join("; ", errors));
            }
            for (MicroAstPanel panel : currentPanels()) {
                if (fileName.equals(panel.getDefaultForFile()) && !claimLines.containsKey(panel.getId())) {
                    claim(panel, null, null);
                }
            }
            assignDefaults(claimLines);
            return null;
        });
    }

    private void claim(MicroAstPanel panel, String defaultFor, String fileName) {
        if (!Objects.equals(defaultFor, panel.getDefaultFor())
                || !Objects.equals(fileName, panel.getDefaultForFile())) {
            panel.setDefaultFor(defaultFor);
            panel.setDefaultForFile(fileName);
            panels.update(panel);
        }
    }

    private List<MicroAstPanel> currentPanels() {
        return panels.getAll().stream().filter(p -> "Y".equals(p.getIsCurrent())).toList();
    }

    /** Every entry of a panel in {@code lines} (panel id to line) must match. */
    public void assignDefaults(Map<String, Integer> lines) {
        List<MicroAstPanel> claimants = currentPanels().stream()
                .filter(p -> "Y".equals(p.getIsActive()) && p.getDefaultFor() != null)
                .sorted(Comparator.comparing(MicroAstPanel::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        Map<MicroOrganism, MicroAstPanel> winners = new LinkedHashMap<>();
        Set<String> matched = new HashSet<>();
        List<String> errors = new ArrayList<>();
        for (var organism : organisms.getActiveOrganisms()) {
            Map<MicroAstPanel, String> bestEntries = new LinkedHashMap<>();
            int bestRank = 0;
            for (var panel : claimants) {
                for (String entry : entries(panel)) {
                    int rank = specificity(entry, organism.getDisplayName(), organism.getLabGroups());
                    if (rank > 0) {
                        matched.add(panel.getId() + "\n" + entry);
                    }
                    if (rank > bestRank) {
                        bestRank = rank;
                        bestEntries.clear();
                    }
                    if (rank > 0 && rank == bestRank) {
                        bestEntries.putIfAbsent(panel, entry);
                    }
                }
            }
            if (bestEntries.isEmpty()) {
                continue;
            }
            var tied = new ArrayList<>(bestEntries.entrySet());
            if (tied.size() > 1) {
                errors.add(line(lines, bestEntries.keySet().toArray(MicroAstPanel[]::new)) + organism.getDisplayName()
                        + " is claimed equally specifically by " + tied
                                .stream().map(claim -> "\"" + claim.getKey().getName() + "\" (\"" + claim.getValue()
                                        + "\", " + source(lines, claim.getKey()) + ")")
                                .collect(Collectors.joining(", ")));
                continue;
            }
            MicroAstPanel best = tied.get(0).getKey();
            String bestEntry = tied.get(0).getValue();
            // Identification assigns the default to every organism except NTM, and
            // refuses a panel whose protocol differs from the organism's.
            if (!"NTM".equals(organism.getSusceptibilityProtocol())
                    && !Objects.equals(organism.getSusceptibilityProtocol(), best.getSusceptibilityProtocol())) {
                errors.add(line(lines, best) + "panel \"" + best.getName() + "\" uses the "
                        + best.getSusceptibilityProtocol() + " protocol but its defaultFor \"" + bestEntry
                        + "\" matches " + organism.getDisplayName() + ", which uses "
                        + organism.getSusceptibilityProtocol());
            } else {
                winners.put(organism, best);
            }
        }
        for (var panel : claimants) {
            if (lines.containsKey(panel.getId())) {
                for (String entry : entries(panel)) {
                    if (!matched.contains(panel.getId() + "\n" + entry)) {
                        errors.add(line(lines, panel) + "Panel \"" + panel.getName() + "\": defaultFor \"" + entry
                                + "\" matches no organism");
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
        winners.forEach((organism, panel) -> {
            if (!panel.getId().equals(organism.getDefaultAstPanelId())) {
                organism.setDefaultAstPanelId(panel.getId());
                organism.setLastUpdatedBy(SYS_USER_ID);
                organisms.update(organism);
            }
        });
    }

    private static List<String> entries(MicroAstPanel panel) {
        return List.of(panel.getDefaultFor().split("\\s*;\\s*"));
    }

    private static String source(Map<String, Integer> lines, MicroAstPanel panel) {
        return lines.containsKey(panel.getId()) ? "line " + lines.get(panel.getId()) : panel.getDefaultForFile();
    }

    private static String line(Map<String, Integer> lines, MicroAstPanel... panels) {
        List<String> numbers = Stream.of(panels).map(panel -> lines.get(panel.getId())).filter(Objects::nonNull)
                .sorted().map(String::valueOf).toList();
        return numbers.isEmpty() ? ""
                : (numbers.size() == 1 ? "Line " : "Lines ") + String.join(" and ", numbers) + ": ";
    }

    /**
     * 3 for the species itself, 2 for its genus, 1 for one of its laboratory
     * groups, 0 otherwise.
     */
    static int specificity(String entry, String organismName, Set<String> labGroups) {
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
        return labGroups.stream().anyMatch(entry::equalsIgnoreCase) ? 1 : 0;
    }

    private LoadedRow<String> loadPanel(List<CsvRow> rows) {
        CsvRow first = rows.get(0);
        MicroAstPanelAdminForm form = new MicroAstPanelAdminForm();
        form.name = required(first, "panelName");
        form.susceptibilityProtocol = sameValue(rows, "susceptibilityProtocol", "GENERAL").toUpperCase(Locale.ROOT);
        List<String> errors = new ArrayList<>();
        for (CsvRow row : rows) {
            try {
                String code = required(row, "antibioticWhonetCode");
                var antibiotic = resolver.resolveAntibiotic(code, getDomainName());
                if (antibiotic == null) {
                    throw new IllegalArgumentException("Line " + row.lineNumber() + ": unknown antibiotic " + code);
                }
                MicroAstPanelAntibioticAdminForm drug = new MicroAstPanelAntibioticAdminForm();
                drug.antibioticId = antibiotic.getId();
                drug.diskContent = blankToNull(row.get("diskContent"));
                form.antibiotics.add(drug);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        String wanted = content(form);
        MicroAstPanel current = panels.findCurrentByNameIgnoreCase(form.name);
        if (current == null) {
            return LoadedRow.created(applied(admin.createPanel(form, SYS_USER_ID).id, wanted));
        }
        MicroAstPanelAdminForm existing = admin.getAstPanel(current.getId());
        if (sameContent(existing, form)) {
            applied(current.getId(), wanted);
            return LoadedRow.skipped("Panel unchanged");
        }
        if (current.getConfigurationContent() != null && !current.getConfigurationContent().equals(content(existing))) {
            return LoadedRow.skipped("Panel edited locally; package version not applied");
        }
        return LoadedRow.updated(applied(admin.publishPanelVersion(current.getId(), form, SYS_USER_ID).id, wanted));
    }

    private String applied(String panelId, String content) {
        MicroAstPanel panel = panels.get(panelId).orElseThrow();
        if (!content.equals(panel.getConfigurationContent())) {
            panel.setConfigurationContent(content);
            panels.update(panel);
        }
        return panelId;
    }

    private static String content(MicroAstPanelAdminForm form) {
        StringBuilder content = new StringBuilder(Objects.toString(form.susceptibilityProtocol, "")).append('|')
                .append(Objects.toString(form.organismGroup, ""));
        for (var drug : form.antibiotics) {
            content.append('|').append(drug.antibioticId).append(':').append(Objects.toString(drug.diskContent, ""));
        }
        return content.toString();
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
            if (!Objects.equals(a.antibioticId, b.antibioticId) || !Objects.equals(a.diskContent, b.diskContent)) {
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
