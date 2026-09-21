package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * A controller that wraps a gated service call in {@code catch (Exception e)}
 * re-labels the gate's {@code AccessDeniedException} — as a 500, or as a 200
 * with an empty body — instead of letting {@code ControllerSetup} answer 403.
 * That was unreachable while controllers carried their own role check ahead of
 * the try; with S011c it is the normal path. Four controllers on a failing CI
 * path were fixed by rethrowing {@code AccessDeniedException} before the broad
 * catch.
 *
 * <p>
 * This is a ratchet over the rest: per-file counts of "broad catch around a
 * gated call with no AccessDenied rethrow" may not grow, a file not in the
 * baseline must have none, and a fixed file must leave the baseline.
 */
public class ControllerDenialRelabelRatchetTest {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Pattern BROAD = Pattern
            .compile("catch\\s*\\(\\s*(?:Exception|RuntimeException|Throwable)\\s+\\w+\\s*\\)");
    private static final Pattern RETHROW = Pattern.compile("catch\\s*\\(\\s*AccessDeniedException\\b");

    /**
     * Controller → offending handlers when this ratchet was written. Only ever
     * lower.
     */
    static final Map<String, Integer> BASELINE = Map.ofEntries(Map.entry("AccessionValidationRestController", 2),
            Map.entry("AlertNotificationConfigRestController", 2), Map.entry("AlertRestController", 2),
            Map.entry("AnalyzerResultsController", 1), Map.entry("BoxSampleRestController", 8),
            Map.entry("CalculatedValueRestController", 1), Map.entry("CatalogImportRestController", 1),
            Map.entry("ClinicalCollectionDictionaryRestController", 1), Map.entry("ComplianceReportRestController", 3),
            Map.entry("ComplianceStandardRestController", 1), Map.entry("CorrectiveActionRestController", 7),
            Map.entry("DictionaryRestController", 1), Map.entry("EQAAlertRestController", 1),
            Map.entry("EQADistributionRestController", 1), Map.entry("EQAEnrollmentRestController", 3),
            Map.entry("EQAMyProgramsRestController", 3), Map.entry("EQAProgramRestController", 3),
            Map.entry("EQAResultRestController", 3), Map.entry("EQASubmissionRestController", 2),
            Map.entry("ElectronicSignatureRestController", 1), Map.entry("FhirQueryRestController", 1),
            Map.entry("FhirTransformationController", 1), Map.entry("FreezerAuditTrailController", 3),
            Map.entry("FreezerDeviceController", 1), Map.entry("FreezerReportController", 1),
            Map.entry("FreezerReportDataController", 1), Map.entry("GenericSampleOrderRestController", 4),
            Map.entry("InventoryItemRestController", 13), Map.entry("InventoryLotRestController", 18),
            Map.entry("InventoryLotStorageRestController", 6), Map.entry("InventoryManagementRestController", 2),
            Map.entry("InventoryTransactionRestController", 5), Map.entry("InventoryUsageRestController", 5),
            Map.entry("LabUnitManagementRestController", 6), Map.entry("LocalizationRestController", 5),
            Map.entry("LogbookResultsController", 1), Map.entry("LogbookResultsRestController", 2),
            Map.entry("LogoUploadController", 1), Map.entry("LogoUploadRestController", 1),
            Map.entry("ManualEntryFieldMapRestController", 3), Map.entry("ManualEntryRestController", 2),
            Map.entry("MassIndexerRestController", 1), Map.entry("NceEnhancementRestController", 3),
            Map.entry("NotificationRestController", 1), Map.entry("OrderAttachmentRestController", 1),
            Map.entry("OrderSearchRestController", 5), Map.entry("OrganizationRestController", 2),
            Map.entry("PatientSearchPopulateRestController", 1), Map.entry("ProviderRestController", 2),
            Map.entry("QCAlertRestController", 4), Map.entry("QCChartDataRestController", 2),
            Map.entry("QCRestController", 13), Map.entry("ReferenceLabResultsRestController", 6),
            Map.entry("ReportDefinitionRestController", 4), Map.entry("ReportNonConformEventsRestController", 3),
            Map.entry("RequestorRestController", 1), Map.entry("ResultEntryRestController", 3),
            Map.entry("SampleAcceptanceRestController", 15), Map.entry("SampleEditRestController", 1),
            Map.entry("SampleEntryByProjectController", 2), Map.entry("SampleItemController", 1),
            Map.entry("SampleManagementRestController", 4), Map.entry("SamplePatientEntryController", 1),
            Map.entry("SamplePatientEntryRestController", 2), Map.entry("SampleQaChecklistRestController", 4),
            Map.entry("SampleRestController", 2), Map.entry("SampleStorageRestController", 5),
            Map.entry("SampleTbEntryController", 1), Map.entry("SampleTypeManagementRestController", 2),
            Map.entry("SampleTypeRequestRestController", 2), Map.entry("ShippingBoxRestController", 17),
            Map.entry("SiteBrandingRestController", 3), Map.entry("StorageLocationRestController", 42),
            Map.entry("StudyElectronicOrdersController", 1), Map.entry("SupportedLocaleRestController", 5),
            Map.entry("TestCatalogEditorRestController", 3), Map.entry("TestMethodRestController", 2),
            Map.entry("TestModifyEntryController", 1), Map.entry("TestModifyEntryRestController", 1),
            Map.entry("TestNotificationConfigMenuController", 1),
            Map.entry("TestNotificationConfigMenuRestController", 1), Map.entry("TestReflexRuleRestController", 1),
            Map.entry("TestRestController", 1), Map.entry("TestSectionEditRestController", 1),
            Map.entry("UnassignedSampleRestController", 6), Map.entry("UnitOfMeasureRestController", 1),
            Map.entry("VectorDeconvolutionRestController", 12), Map.entry("VectorDictionaryRestController", 9),
            Map.entry("VectorIdentificationRestController", 9), Map.entry("VectorSampleTypeRestController", 2),
            Map.entry("VectorSamplingSiteRestController", 4), Map.entry("VectorSpeciesRestController", 5),
            Map.entry("VectorSurveillanceRestController", 2), Map.entry("VectorTrapTypeRestController", 4));

    /** Pure over one controller's source and the set of gated type names. */
    static int relabelingHandlers(String src, Set<String> gatedTypes) {
        Set<String> fields = new HashSet<>();
        Matcher fm = Pattern.compile("\\b(\\w+)\\s+(\\w+)\\s*;").matcher(src);
        while (fm.find()) {
            if (gatedTypes.contains(fm.group(1))) {
                fields.add(fm.group(2));
            }
        }
        if (fields.isEmpty()) {
            return 0;
        }
        int n = 0;
        Matcher tm = Pattern.compile("try\\s*\\{").matcher(src);
        while (tm.find()) {
            int i = tm.end(), depth = 1;
            while (depth > 0 && i < src.length()) {
                char c = src.charAt(i++);
                depth += c == '{' ? 1 : c == '}' ? -1 : 0;
            }
            String body = src.substring(tm.end(), i - 1);
            String after = src.substring(i, Math.min(src.length(), i + 400));
            Matcher b = BROAD.matcher(after);
            boolean callsGated = false;
            for (String f : fields) {
                if (Pattern.compile("\\b" + f + "\\.\\w+\\(").matcher(body).find()) {
                    callsGated = true;
                    break;
                }
            }
            if (callsGated && b.find() && !RETHROW.matcher(after.substring(0, b.start())).find()) {
                n++;
            }
        }
        return n;
    }

    @Test
    public void detection_countsRelabelingHandlers_andAcceptsARethrow() {
        Set<String> gated = Set.of("FooService");
        String bad = "class C { private FooService foo; void a() { try { foo.x(); } catch (Exception e) { } } }";
        String fixed = "class C { private FooService foo; void a() { try { foo.x(); } catch (AccessDeniedException e) { throw e; } catch (Exception e) { } } }";
        String unrelated = "class C { private Bar bar; void a() { try { bar.x(); } catch (Exception e) { } } }";
        assertEquals(1, relabelingHandlers(bad, gated));
        assertEquals(0, relabelingHandlers(fixed, gated));
        assertEquals(0, relabelingHandlers(unrelated, gated));
    }

    private static Map<String, Integer> scanTree() throws IOException {
        Set<String> gated = new HashSet<>();
        Map<String, String> controllers = new TreeMap<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String src = Files.readString(p);
                String name = p.getFileName().toString().replace(".java", "");
                if (src.contains("@PreAuthorize")) {
                    gated.add(name);
                }
                if (name.contains("Controller")) {
                    controllers.put(name, src);
                }
            }
        }
        assertTrue("suspiciously few gated types indexed", gated.size() > 200);
        Map<String, Integer> counts = new TreeMap<>();
        for (Map.Entry<String, String> e : controllers.entrySet()) {
            int n = relabelingHandlers(e.getValue(), gated);
            if (n > 0) {
                counts.put(e.getKey(), n);
            }
        }
        return counts;
    }

    @Test
    public void noControllerGainsAHandlerThatRelabelsADenial() throws IOException {
        Map<String, Integer> now = scanTree();
        List<String> grew = new ArrayList<>();
        for (Map.Entry<String, Integer> e : now.entrySet()) {
            int allowed = BASELINE.getOrDefault(e.getKey(), 0);
            if (e.getValue() > allowed) {
                grew.add(e.getKey() + ": " + e.getValue() + " > " + allowed);
            }
        }
        assertTrue("Controller handler(s) wrap a gated service call in a broad catch with no AccessDeniedException"
                + " rethrow, so a denial becomes a 500/200 instead of 403. Add `catch (AccessDeniedException e) { throw e; }`"
                + " ahead of the broad catch. Do NOT raise BASELINE: " + grew, grew.isEmpty());
    }

    @Test
    public void baselineOnlyShrinks() throws IOException {
        Map<String, Integer> now = scanTree();
        List<String> stale = new ArrayList<>();
        for (Map.Entry<String, Integer> e : BASELINE.entrySet()) {
            int current = now.getOrDefault(e.getKey(), 0);
            if (current < e.getValue()) {
                stale.add(e.getKey() + ": baseline " + e.getValue() + ", now " + current);
            }
        }
        assertTrue("Lower these BASELINE entries so the ratchet tightens: " + stale, stale.isEmpty());
    }
}
