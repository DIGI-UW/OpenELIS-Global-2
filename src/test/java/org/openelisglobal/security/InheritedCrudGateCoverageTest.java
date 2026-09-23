package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Inherited CRUD (T6). Every service extending {@code BaseObjectService}
 * inherits {@code get/getAll/insert/update/delete…}, gated once on
 * {@code BaseObjectService} itself via {@code CrudGate}, which resolves the
 * privilege from the descendant interface's {@code @CrudPrivileges}, else its
 * type-level {@code @PreAuthorize}, else leaves the call OPEN. This test keeps
 * that transitional "open" set from growing, and enforces the one shape that
 * must never appear.
 *
 * <p>
 * <b>Rule 1 (hard):</b> no descendant interface may redeclare a
 * {@code BaseObjectService} method with its own {@code @PreAuthorize}. Such a
 * gate is resolved against the most specific method — declared in
 * {@code BaseObjectServiceImpl}, whose hierarchy does not include the
 * descendant — and is therefore NOT enforced in production; a JDK-proxy stub in
 * a slice test makes it look enforced. {@code BaseObjectServiceCrudGateTest}
 * pins this. Declare {@code @CrudPrivileges} instead.
 *
 * <p>
 * <b>Rule 2 (ratchet):</b> a method-gated-only descendant with no
 * {@code @CrudPrivileges(write=…)} has open inherited writes. BASELINE is the
 * set when this was written; it may only shrink. Seven of them are open on
 * purpose for now because their inherited writes are called inside
 * Reception/Results workflows (order entry, result entry, audit trail) whose
 * roles do not hold the write privilege — see
 * specs/017-rbac-open-items/t6-inherited-crud-ungated.md.
 *
 * <p>
 * <b>Rule 3:</b> a declared {@code @CrudPrivileges} value must be a real
 * {@code PRIV_*} authority, or the gate denies everyone silently.
 */
public class InheritedCrudGateCoverageTest {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Pattern TYPE_LEVEL = Pattern.compile(
            "@PreAuthorize\\(.*?\\)\\s*(?:\\n\\s*(?:@\\w+(?:\\([^)]*\\))?|//.*)\\s*)*\\n\\s*public\\s+interface\\s+(\\w+)",
            Pattern.DOTALL);
    private static final Pattern EXTENDS_BASE = Pattern
            .compile("public\\s+interface\\s+(\\w+)\\s+extends\\s+([^{]+)\\{");
    private static final Pattern GENERICS = Pattern
            .compile("BaseObjectService\\s*<\\s*([\\w.]+)\\s*,\\s*([\\w.]+)\\s*>");
    private static final Pattern GATED_DECL = Pattern
            .compile("@PreAuthorize\\([^\\n]*\\)\\s*(?:@Override\\s*)?[\\w<>\\[\\], .?]+\\s+(\\w+)\\s*\\(([^)]*)\\)");
    private static final Pattern CRUD_PRIVILEGES = Pattern.compile("@CrudPrivileges\\(([^)]*)\\)");
    private static final Pattern PRIV_VALUE = Pattern.compile("(read|write)\\s*=\\s*\"([^\"]*)\"");

    /** BaseObjectService signatures with T/PK placeholders. */
    private static final Map<String, String[]> CRUD = new LinkedHashMap<>();
    static {
        CRUD.put("get", new String[] { "PK" });
        CRUD.put("getAll", new String[] {});
        CRUD.put("insert", new String[] { "T" });
        CRUD.put("insertAll", new String[] { "List<T>" });
        CRUD.put("save", new String[] { "T" });
        CRUD.put("saveAll", new String[] { "List<T>" });
        CRUD.put("update", new String[] { "T" });
        CRUD.put("updateAll", new String[] { "List<T>" });
        CRUD.put("delete", new String[] { "T" });
        CRUD.put("delete#2", new String[] { "PK", "String" });
        CRUD.put("deleteAll", new String[] { "List<T>" });
        CRUD.put("deleteAll#2", new String[] { "List<PK>", "String" });
        CRUD.put("getCount", new String[] {});
        CRUD.put("getPage", new String[] { "int" });
        CRUD.put("getNext", new String[] { "String" });
        CRUD.put("getPrevious", new String[] { "String" });
        CRUD.put("hasNext", new String[] { "String" });
        CRUD.put("hasPrevious", new String[] { "String" });
    }

    /**
     * Open-inherited-write services when this ratchet was (re)written. Only ever
     * remove.
     */
    static final Set<String> BASELINE = new TreeSet<>(Set.of("AnalysisNotificationConfigService", "AnalysisService",
            "AnalyzerPluginConfigService", "AnalyzerProfileBindingService", "AnalyzerService", "AnalyzerTypeService",
            "BarcodeLabelInfoService", "CertificateAuthenticationDataService", "ConfigurationImportRunService",
            "CorrectiveActionService", "CytologySampleService", "DictionaryCategoryService", "EQADistributionService",
            "EQALabProgramEnrollmentService", "EQAProgramEnrollmentService", "EQAProgramService", "EQAResultService",
            "ElectronicSignatureService", "HistoryService", "ImageService", "ImmunohistochemistrySampleService",
            "InventoryTransactionService", "InventoryUsageService", "ManualEntryFieldMapService", "MethodService",
            "NCEventService", "NceActionLogService", "NceAttachmentService", "NceCategoryService", "NceHistoryService",
            "NceSpecimenService", "NceTypeService", "NoteBookSampleService", "NoteBookService",
            "NotificationLogService", "NotificationTriggerConfigService", "OrderAttachmentService",
            "OrganizationService", "OrganizationTypeService", "PanelItemService", "PanelTerminologyMappingService",
            "PathologySampleService", "PatientIdDocumentService", "PatientPhotoService", "PatientService",
            "ProgramSampleService", "ProviderService", "QCControlLotService", "QCResultService", "QCStatisticsService",
            "ReferenceAliasService", "ReferenceTablesService", "ReferralService", "RenameMethodService",
            "RenameTestSectionService", "ReportDefinitionService", "ReportService", "RequesterTypeService",
            "ResultCalculationService", "ResultLimitService", "RoleService", "SampleAcceptanceRecordService",
            "SampleComplianceStandardService", "SampleEQAService", "SampleHumanService", "SampleItemService",
            "SampleOrderOverrideService", "SampleQaChecklistService", "SampleTypeRequestService",
            "SampleTypeTerminologyMappingService", "ScriptletService", "SiteBrandingService", "SiteInformationService",
            "StorageBoxService", "StorageDeviceService", "StorageRackService", "StorageRoomService",
            "StorageShelfService", "SystemModuleService", "SystemUserSectionService", "SystemUserService",
            "TestAlertRuleService", "TestCodeTypeService", "TestNotificationConfigService", "TestQcTargetService",
            "TestReflexService", "UnresolvedReferenceService", "UserRoleService", "VectorMolecularRecordService",
            "VectorPoolService", "VectorSpeciesService", "VectorSpecimenIdentificationService", "VectorTrapTypeService",
            "WestgardRuleConfigService"));

    record Scan(List<String> uncovered, List<String> deadGates, List<String> badPrivileges) {
    }

    static Scan scan(Set<String> knownPrivileges) throws IOException {
        List<String> uncovered = new ArrayList<>(), dead = new ArrayList<>(), bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path f : files.filter(p -> p.toString().endsWith("Service.java")).toList()) {
                String src = Files.readString(f);
                Matcher decl = EXTENDS_BASE.matcher(src);
                if (!decl.find() || !decl.group(2).contains("BaseObjectService") || !src.contains("@PreAuthorize")) {
                    continue;
                }
                String name = decl.group(1);
                Matcher g = GENERICS.matcher(decl.group(2));
                String t = g.find() ? g.group(1) : "?", pk = g.find(0) ? g.group(2) : "?";
                Matcher gd = GATED_DECL.matcher(src);
                while (gd.find()) {
                    List<String> params = new ArrayList<>();
                    for (String raw : gd.group(2).split(",")) {
                        if (!raw.isBlank()) {
                            String[] parts = raw.trim().replaceAll("\\s+", " ").split(" ");
                            params.add(String.join("", java.util.Arrays.copyOf(parts, parts.length - 1)));
                        }
                    }
                    for (Map.Entry<String, String[]> e : CRUD.entrySet()) {
                        if (!e.getKey().split("#")[0].equals(gd.group(1))) {
                            continue;
                        }
                        List<String> expected = new ArrayList<>();
                        for (String x : e.getValue()) {
                            expected.add(x.replace("PK", pk).replace("T", t));
                        }
                        if (expected.equals(params)) {
                            dead.add(name + "#" + gd.group(1) + params);
                        }
                    }
                }
                Matcher cp = CRUD_PRIVILEGES.matcher(src);
                boolean writeDeclared = false;
                if (cp.find()) {
                    Matcher pv = PRIV_VALUE.matcher(cp.group(1));
                    while (pv.find()) {
                        if (!pv.group(2).isEmpty() && !knownPrivileges.contains(pv.group(2))) {
                            bad.add(name + ": @CrudPrivileges " + pv.group(1) + "=\"" + pv.group(2)
                                    + "\" is not a PRIV_* constant");
                        }
                        if (pv.group(1).equals("write") && pv.group(2).startsWith("PRIV_")) {
                            writeDeclared = true;
                        }
                    }
                }
                if (TYPE_LEVEL.matcher(src).find() || writeDeclared) {
                    continue;
                }
                uncovered.add(name);
            }
        }
        return new Scan(uncovered, dead, bad);
    }

    private static Scan scanTree() throws IOException {
        return scan(new TreeSet<>(SeededRoleAuthorities.allPrivilegeAuthorityNames()));
    }

    @Test
    public void rule1_noDescendantRedeclaresInheritedCrudWithItsOwnGate() throws IOException {
        List<String> dead = scanTree().deadGates();
        assertTrue("These @PreAuthorize gates sit on a redeclared BaseObjectService method and are NOT enforced in"
                + " production (see BaseObjectServiceCrudGateTest). Remove the redeclaration and declare"
                + " @CrudPrivileges on the interface instead: " + dead, dead.isEmpty());
    }

    @Test
    public void rule3_declaredCrudPrivilegesAreRealAuthorities() throws IOException {
        List<String> bad = scanTree().badPrivileges();
        assertTrue(String.join("\n", bad), bad.isEmpty());
    }

    @Test
    public void rule2_noNewOpenInheritedWrites() throws IOException {
        Set<String> now = new TreeSet<>(scanTree().uncovered());
        assertTrue("Scan found nothing — the tree moved or the pattern broke; a silent pass is not a pass",
                now.size() > 40);
        Set<String> added = new TreeSet<>(now);
        added.removeAll(BASELINE);
        assertTrue("New service interface(s) extending BaseObjectService with open inherited writes. Add"
                + " @CrudPrivileges(write = \"PRIV_…\") (or a type-level @PreAuthorize). Do NOT add to BASELINE: "
                + added, added.isEmpty());
    }

    @Test
    public void rule2_baselineOnlyShrinks() throws IOException {
        Set<String> now = new TreeSet<>(scanTree().uncovered());
        Set<String> fixed = new TreeSet<>(BASELINE);
        fixed.removeAll(now);
        assertTrue("These are now covered — remove them from BASELINE so the ratchet tightens: " + fixed,
                fixed.isEmpty());
    }
}
