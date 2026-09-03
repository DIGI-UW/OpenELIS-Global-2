package org.openelisglobal.eqa.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.openelisglobal.eqa.controller.rest.EQAGuards;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-609 — the EQA authorization model as invariants rather than an endpoint
 * census. Four rules hold across the package:
 *
 * <ol>
 * <li>Every controller carries the {@link EQAGuards#READ} umbrella at class
 * level, with one documented exception.
 * <li>Every state-mutating handler declares its own guard. Spring replaces the
 * class annotation rather than ANDing it, so a write that omits a method-level
 * guard silently runs under the read umbrella.
 * <li>No read declares its own guard, for the same reason: it would opt out of
 * the umbrella instead of tightening it.
 * <li>Every authority named in a guard is registered <em>and</em> granted by a
 * liquibase changeset that base-changelog.xml includes, so a guard cannot
 * reference a permission no migration creates — which fails closed and looks
 * identical to a working guard.
 * </ol>
 *
 * Expected values come from {@link EQAGuards}, so this test pins which lane
 * each endpoint belongs to — a downgrade from the provider tier to the
 * participant tier is a real regression no invariant would catch — without
 * re-typing the expressions and drifting from them.
 */
public class EQARestGuardMatrixTest {

    private static final String EQA_REST_PACKAGE = "org.openelisglobal.eqa.controller.rest";

    /**
     * Encoded so it cannot be "corrected" by accident; rationale on the class
     * itself.
     */
    private static final Map<String, String> CLASS_GUARD_EXCEPTIONS = Map.of("EQAAlertRestController",
            EQAGuards.LAB_WIDE_ALERTS);

    /** Every state-mutating handler and the guard it must declare. */
    private static final Map<String, String> WRITE_GUARDS = new HashMap<>();
    static {
        // Lab-wide alert acknowledgement rides the exception above.
        WRITE_GUARDS.put("EQAAlertRestController#acknowledgeAlert", EQAGuards.LAB_WIDE_ALERTS);
        // Cycle lifecycle
        WRITE_GUARDS.put("EQACycleRestController#createCycle", EQAGuards.MANAGE);
        WRITE_GUARDS.put("EQACycleRestController#createMyCycle", EQAGuards.PARTICIPANT);
        WRITE_GUARDS.put("EQACycleRestController#transition", EQAGuards.MANAGE);
        // OGC-934: commentary on a signed report is a scoring-lane act.
        WRITE_GUARDS.put("EQACycleRestController#attachReportComments", EQAGuards.MANAGE);
        WRITE_GUARDS.put("EQACycleRestController#detachReportComment", EQAGuards.MANAGE);
        WRITE_GUARDS.put("EQAFollowupRestController#escalate", EQAGuards.MANAGE);
        WRITE_GUARDS.put("EQAFollowupRestController#dismiss", EQAGuards.MANAGE);
        // Provider-round management
        WRITE_GUARDS.put("EQADistributionRestController#createDistribution", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQADistributionRestController#updateDistribution", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQADistributionRestController#advanceStatus", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQADistributionRestController#generateBarcodes", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAEnrollmentRestController#createEnrollments", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAEnrollmentRestController#updateEnrollmentStatus", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAProgramRestController#createProgram", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAProgramRestController#updateProgram", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAProgramRestController#updateTestAssignments", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAProgramRestController#updateAnalysts", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQASubmissionRestController#approveLateSubmission", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQACycleRestController#createProviderCycle", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAResultRestController#takeInCycleResults", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQASubmissionRestController#intakeScoresCsv", EQAGuards.MANAGE);
        WRITE_GUARDS.put("EQAResultRestController#importCycleResultsCsv", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAShipmentRestController#savePrep", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAShipmentRestController#saveShipment", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAShipmentRestController#ship", EQAGuards.PROVIDER);
        // T-26: receipt monitoring, reprovisioning and score return are provider work
        WRITE_GUARDS.put("EQAShipmentRestController#markDelivered", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAShipmentRestController#sendRepeat", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAShipmentRestController#distributeScores", EQAGuards.PROVIDER);
        // Scoring writes verdicts and advances the cycle, as intakeScores does
        WRITE_GUARDS.put("EQAShipmentRestController#score", EQAGuards.MANAGE);
        // T-27: the provider follow-up register's triage
        WRITE_GUARDS.put("EQAFollowupRestController#triage", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAFollowupRestController#notifyParticipant", EQAGuards.PROVIDER);
        WRITE_GUARDS.put("EQAFollowupRestController#repeat", EQAGuards.PROVIDER);
        // Score intake is the provider's verdict coming back (FR-V2.2-08)
        WRITE_GUARDS.put("EQASubmissionRestController#intakeScores", EQAGuards.MANAGE);
        // Participant lane — bench work, so the legacy roles still admit it
        WRITE_GUARDS.put("EQAMyProgramsRestController#createMyProgram", EQAGuards.PARTICIPANT);
        WRITE_GUARDS.put("EQAMyProgramsRestController#updateMyProgram", EQAGuards.PARTICIPANT);
        WRITE_GUARDS.put("EQAMyProgramsRestController#deleteMyProgram", EQAGuards.PARTICIPANT);
        WRITE_GUARDS.put("EQAPanelReceiptRestController#recordReceipt", EQAGuards.PARTICIPANT);
        WRITE_GUARDS.put("EQASubmissionRestController#submitManually", EQAGuards.PARTICIPANT);
        WRITE_GUARDS.put("EQAParticipantResultRestController#createDraft", EQAGuards.PARTICIPANT);
        WRITE_GUARDS.put("EQAParticipantResultRestController#transition", EQAGuards.PARTICIPANT);
        WRITE_GUARDS.put("EQAResultRestController#submitResult", EQAGuards.PARTICIPANT);
        WRITE_GUARDS.put("EQAResultRestController#batchImportResults", EQAGuards.PARTICIPANT);
        WRITE_GUARDS.put("EQASubmissionRestController#submitViaFhir", EQAGuards.PARTICIPANT);
        // Panel lifecycle, and the separate privilege that reveals sealed targets
        WRITE_GUARDS.put("EQAPanelRestController#createPanel", EQAGuards.MANAGE);
        WRITE_GUARDS.put("EQAPanelRestController#seal", EQAGuards.MANAGE);
        WRITE_GUARDS.put("EQAPanelRestController#distribute", EQAGuards.MANAGE);
        WRITE_GUARDS.put("EQAPanelRestController#sealAndDistribute", EQAGuards.MANAGE);
        WRITE_GUARDS.put("EQAPanelRestController#unblind", EQAGuards.UNBLIND);
        WRITE_GUARDS.put("EQAParticipantResultRestController#score", EQAGuards.MANAGE);
    }

    /** Authority → the changeset that must both register and grant it. */
    private static final Map<String, String> AUTHORITY_CHANGESETS = Map.of("qa.view.eqa",
            "liquibase/qa/004-add-qa-permission-model.xml", "qa.eqa.participant",
            "liquibase/qa/019-add-eqa-v2-menus-permissions.xml", "qa.eqa.provider",
            "liquibase/qa/019-add-eqa-v2-menus-permissions.xml", "qa.eqa.inhouse.unblind",
            "liquibase/qa/019-add-eqa-v2-menus-permissions.xml", "qa.manage.eqa",
            "liquibase/qa/023-add-eqa-manage-permission.xml");

    /**
     * Compat grant that replaced the legacy-role clause in the participant guard.
     */
    private static final String BENCH_ROLE_GRANT = "liquibase/qa/026-grant-eqa-participant-to-bench-roles.xml";

    private List<Class<?>> eqaRestControllers() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(EQA_REST_PACKAGE)) {
            classes.add(Class.forName(bd.getBeanClassName()));
        }
        return classes;
    }

    private boolean isHandler(Method m) {
        return AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class);
    }

    private boolean isRead(Method m) {
        return AnnotatedElementUtils.hasAnnotation(m, GetMapping.class);
    }

    @Test
    public void everyControllerCarriesTheReadUmbrellaUnlessDocumented() throws Exception {
        List<Class<?>> controllers = eqaRestControllers();
        assertFalse("the package scan found no controllers at all", controllers.isEmpty());
        for (Class<?> controller : controllers) {
            PreAuthorize guard = controller.getAnnotation(PreAuthorize.class);
            assertNotNull(controller.getSimpleName() + " must carry a class-level guard", guard);
            String expected = CLASS_GUARD_EXCEPTIONS.getOrDefault(controller.getSimpleName(), EQAGuards.READ);
            assertEquals(controller.getSimpleName() + " class-level guard", expected, guard.value());
        }
    }

    @Test
    public void everyWriteDeclaresItsOwnGuardAndItIsTheExpectedTier() throws Exception {
        Set<String> seen = new TreeSet<>();
        for (Class<?> controller : eqaRestControllers()) {
            for (Method m : controller.getDeclaredMethods()) {
                if (!isHandler(m) || isRead(m)) {
                    continue;
                }
                String key = controller.getSimpleName() + "#" + m.getName();
                PreAuthorize guard = m.getAnnotation(PreAuthorize.class);
                // Inheriting is only acceptable where the class guard is itself
                // not the read umbrella — that is, the documented exception.
                // Everywhere else a missing method guard means the write runs
                // under a read permission, because Spring replaces the class
                // annotation rather than ANDing it.
                boolean classGuardIsUmbrella = !CLASS_GUARD_EXCEPTIONS.containsKey(controller.getSimpleName());
                if (classGuardIsUmbrella) {
                    assertNotNull(key + " mutates state, so it must declare its own @PreAuthorize rather than"
                            + " inherit the read umbrella", guard);
                }
                String effective = guard != null ? guard.value() : controller.getAnnotation(PreAuthorize.class).value();
                String expected = WRITE_GUARDS.get(key);
                assertNotNull("new write endpoint " + key + " must be assigned a tier in this test deliberately",
                        expected);
                assertEquals(key + " guard", expected, effective);
                seen.add(key);
            }
        }
        assertEquals("entries with no matching handler — renamed or deleted write endpoint?",
                new TreeSet<>(WRITE_GUARDS.keySet()), seen);
    }

    @Test
    public void readsInheritTheClassGuardRatherThanRedeclaringIt() throws Exception {
        for (Class<?> controller : eqaRestControllers()) {
            for (Method m : controller.getDeclaredMethods()) {
                if (!isHandler(m) || !isRead(m)) {
                    continue;
                }
                // A GET that declares its own guard silently opts out of the
                // class umbrella, because Spring replaces rather than ANDs.
                // EQAPanelRestController#getSamples is the precedent: a read
                // whose visibility varies by privilege resolves that in the
                // service, not in a per-GET annotation.
                assertNull(
                        controller.getSimpleName() + "#" + m.getName() + " is a read and must rely on the"
                                + " class-level guard instead of declaring its own",
                        m.getAnnotation(PreAuthorize.class));
            }
        }
    }

    @Test
    public void everyGuardedAuthorityIsRegisteredGrantedAndWiredIntoTheChangelog() throws Exception {
        // Read off the annotations themselves, not off this test's maps: the
        // failure mode is a guard naming a permission no migration creates,
        // which fails closed and looks identical to a working guard.
        Set<String> named = new TreeSet<>();
        for (Class<?> controller : eqaRestControllers()) {
            named.addAll(authoritiesIn(controller.getAnnotation(PreAuthorize.class)));
            for (Method m : controller.getDeclaredMethods()) {
                named.addAll(authoritiesIn(m.getAnnotation(PreAuthorize.class)));
            }
        }
        assertEquals("every authority a guard names needs a migration mapped here",
                new TreeSet<>(AUTHORITY_CHANGESETS.keySet()), named);

        // Asserted against the changeset SOURCE, not system_module rows: dbUnit
        // fixtures truncate that table in full-suite runs, so row assertions are
        // suite-order coin flips.
        String changelog = classpathResource("liquibase/base-changelog.xml");
        for (Map.Entry<String, String> entry : AUTHORITY_CHANGESETS.entrySet()) {
            String authority = entry.getKey();
            String file = entry.getValue();
            String changeset = classpathResource(file);
            assertTrue(authority + " is not registered by " + file,
                    changeset.contains("value=\"" + authority + "\"/>"));
            assertTrue(authority + " is registered but no role grant selects it in " + file,
                    grantsAuthority(changeset, authority));
            // A changeset nobody includes never runs, and the guard above then
            // refuses every caller in a fresh deployment.
            assertTrue(file + " is not included by base-changelog.xml", changelog.contains(file));
        }

        // EQAGuards.PARTICIPANT dropped its legacy-role clause, so bench access
        // to the participant lane is now this grant and nothing else.
        String benchGrant = classpathResource(BENCH_ROLE_GRANT);
        assertTrue(BENCH_ROLE_GRANT + " is not included by base-changelog.xml", changelog.contains(BENCH_ROLE_GRANT));
        assertTrue("the bench grant must select the participant tier",
                benchGrant.contains("m.name =" + " 'qa.eqa.participant'"));
        assertTrue("the bench grant must name Reception and Results",
                benchGrant.contains("r.name IN ('Reception', 'Results')"));
    }

    private Set<String> authoritiesIn(PreAuthorize guard) {
        Set<String> found = new TreeSet<>();
        if (guard == null) {
            return found;
        }
        Matcher matcher = Pattern.compile("hasAuthority\\('([^']+)'\\)").matcher(guard.value());
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    /**
     * The grant is an INSERT into system_role_module that selects the module by
     * name, either singly or as part of an IN list. Requiring that SQL shape keeps
     * the registration insert in the same file from satisfying the assertion.
     */
    private boolean grantsAuthority(String changeset, String authority) {
        return changeset.contains("m.name = '" + authority + "'")
                || changeset.matches("(?s).*m\\.name IN \\([^)]*'" + Pattern.quote(authority) + "'.*");
    }

    private String classpathResource(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(path + " must exist on the classpath", in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
