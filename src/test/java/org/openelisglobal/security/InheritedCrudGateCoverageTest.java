package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Third structural blind spot in the RBAC coverage scans, after interfaces-only
 * (T1's {@code @Service} classes) and mock-annotation copying: a service
 * interface gated <em>per method</em> that extends {@code BaseObjectService}
 * inherits {@code get/getAll/insert/update/delete/save…} with <b>no gate at
 * all</b>. Only a TYPE-level {@code @PreAuthorize} covers inherited methods
 * ({@code ClassLevelPreAuthorizeSemanticsTest} pins that). With the
 * controller-level role checks removed by S011c, those inherited writes are
 * reachable by any authenticated user wherever a controller calls them. This is
 * how {@code DELETE /rest/alerts/{id}} returned 204 for a Reception user.
 *
 * <p>
 * This is a ratchet, not a fix. The baseline below is every offender at the
 * time it was written (see
 * specs/017-rbac-open-items/t6-inherited-crud-ungated.md). Two things may never
 * happen: a NEW method-gated-only {@code BaseObjectService} descendant appears
 * (gate it, do not extend the list), and a fixed one stays in the list (remove
 * it, so the list only shrinks).
 *
 * <p>
 * "Covered" means: a type-level gate, or all eight inherited write methods
 * redeclared with a gate ({@code AlertService} is the worked example).
 * Inherited reads on the baseline services are also ungated; they are in scope
 * for T6 but not tracked by this ratchet.
 */
public class InheritedCrudGateCoverageTest {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Pattern TYPE_LEVEL = Pattern.compile(
            "@PreAuthorize\\(.*?\\)\\s*(?:\\n\\s*(?:@\\w+(?:\\([^)]*\\))?|//.*)\\s*)*\\n\\s*public\\s+interface\\s+(\\w+)",
            Pattern.DOTALL);
    private static final Pattern EXTENDS_BASE = Pattern
            .compile("public\\s+interface\\s+(\\w+)\\s+extends\\s+([^{]+)\\{");
    private static final String[] WRITES = { "insert", "insertAll", "save", "saveAll", "update", "updateAll", "delete",
            "deleteAll" };
    private static final Pattern GATED_WRITE = Pattern
            .compile("@PreAuthorize\\([^\\n]*\\)\\s*(?:@Override\\s*)?[\\w<>\\[\\], .]+\\s+(" + String.join("|", WRITES)
                    + ")\\s*\\(");

    /**
     * Known offenders when this ratchet was written. Only ever remove from this
     * list.
     */
    static final Set<String> BASELINE = new TreeSet<>(Set.of("AnalysisService", "AnalyzerPluginConfigService",
            "AnalyzerProfileBindingService", "AnalyzerService", "AnalyzerTypeService", "BarcodeLabelInfoService",
            "CorrectiveActionService", "ComplianceStandardService", "ComplianceThresholdService",
            "ParameterGroupService", "ConfigurationImportRunService", "ReferenceAliasService",
            "UnresolvedReferenceService", "ElectronicOrderService", "DictionaryService", "DictionaryCategoryService",
            "EQADistributionService", "EQALabProgramEnrollmentService", "EQAProgramEnrollmentService",
            "EQAProgramService", "EQAResultService", "SampleEQAService", "ElectronicSignatureService",
            "CertificateAuthenticationDataService", "ExternalConnectionService", "HistoryService", "ImageService",
            "InventoryItemService", "InventoryLotService", "InventoryTransactionService", "InventoryUsageService",
            "LocalizationService", "SupportedLocaleService", "MenuService", "MethodService", "NoteBookSampleService",
            "NoteBookService", "AnalysisNotificationConfigService", "NotificationLogService",
            "NotificationTriggerConfigService", "TestNotificationConfigService", "OrganizationService",
            "OrganizationTypeService", "PanelItemService", "PanelTerminologyMappingService", "PatientIdDocumentService",
            "PatientPhotoService", "PatientService", "ImmunohistochemistrySampleService", "PathologySampleService",
            "ProgramSampleService", "CytologySampleService", "ProviderService", "SampleQaChecklistService",
            "NCEventService", "NceActionLogService", "NceAttachmentService", "NceCategoryService", "NceHistoryService",
            "NceSpecimenService", "NceTypeService", "QCControlLotService", "QCResultService", "QCStatisticsService",
            "WestgardRuleConfigService", "ReferenceTablesService", "ReferralService", "RenameMethodService",
            "RenameTestSectionService", "ReportService", "ReportDefinitionService", "ManualEntryFieldMapService",
            "RequesterTypeService", "ResultLimitService", "RoleService", "OrderAttachmentService",
            "SampleOrderOverrideService", "SampleComplianceStandardService", "SampleAcceptanceRecordService",
            "SampleHumanService", "SampleItemService", "SampleTypeRequestService",
            "SampleTypeTerminologyMappingService", "ScriptletService", "SiteBrandingService", "SiteInformationService",
            "StorageBoxService", "StorageDeviceService", "StorageRackService", "StorageRoomService",
            "StorageShelfService", "SystemModuleService", "SystemUserService", "SystemUserSectionService",
            "TestSectionService", "TestService", "TestAlertRuleService", "ResultCalculationService",
            "TestQcTargetService", "TestCodeTypeService", "TestReflexService", "UnitOfMeasureService",
            "UserRoleService", "VectorMolecularRecordService", "VectorSpecimenIdentificationService",
            "VectorPoolService", "VectorSamplingSiteService", "VectorSpeciesService", "VectorTrapTypeService"));

    static List<String> uncoveredNow() throws IOException {
        List<String> out = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path f : files.filter(p -> p.toString().endsWith("Service.java")).toList()) {
                String src = Files.readString(f);
                Matcher decl = EXTENDS_BASE.matcher(src);
                if (!decl.find() || !decl.group(2).contains("BaseObjectService") || !src.contains("@PreAuthorize")) {
                    continue;
                }
                if (TYPE_LEVEL.matcher(src).find()) {
                    continue; // type-level gate covers inherited methods
                }
                Set<String> gatedWrites = new TreeSet<>();
                int gatedWriteDeclarations = 0;
                Matcher w = GATED_WRITE.matcher(src);
                while (w.find()) {
                    gatedWrites.add(w.group(1));
                    gatedWriteDeclarations++;
                }
                if (gatedWrites.containsAll(Set.of(WRITES)) && gatedWriteDeclarations >= 10) {
                    continue; // all inherited writes redeclared with a gate
                }
                out.add(decl.group(1));
            }
        }
        return out;
    }

    @Test
    public void noNewMethodGatedOnlyBaseObjectServiceDescendants() throws IOException {
        Set<String> now = new TreeSet<>(uncoveredNow());
        assertTrue("Scan found nothing — the tree moved or the pattern broke; a silent pass is not a pass",
                now.size() > 50);
        Set<String> added = new TreeSet<>(now);
        added.removeAll(BASELINE);
        assertTrue("New service interface(s) gated per method but extending BaseObjectService, so their"
                + " inherited get/getAll/insert/update/delete are UNGATED. Add a type-level @PreAuthorize or"
                + " redeclare the inherited writes with a gate (see AlertService). Do NOT add to BASELINE: " + added,
                added.isEmpty());
    }

    @Test
    public void baselineOnlyShrinks() throws IOException {
        Set<String> now = new TreeSet<>(uncoveredNow());
        Set<String> fixed = new TreeSet<>(BASELINE);
        fixed.removeAll(now);
        assertTrue("These are now covered — remove them from BASELINE so the ratchet tightens: " + fixed,
                fixed.isEmpty());
    }
}
