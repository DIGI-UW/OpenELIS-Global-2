package org.openelisglobal.eqa.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
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
 * census. Three rules hold across the package:
 *
 * <ol>
 * <li>Every controller carries the {@code qa.view.eqa} read umbrella at class
 * level, with one documented exception.
 * <li>Every state-mutating handler declares its own guard. Spring replaces the
 * class annotation rather than ANDing it, so a write that omits a method-level
 * guard silently runs under the read umbrella.
 * <li>Every authority named in a guard is registered <em>and</em> granted by a
 * liquibase changeset, so a guard cannot reference a permission no migration
 * creates — which fails closed and looks identical to a working guard.
 * </ol>
 *
 * Write guards are still enumerated, because a downgrade from the provider tier
 * to the participant tier is a real regression that no invariant would catch.
 * Reads are asserted by rule, so adding a GET does not mean editing this test.
 */
public class EQARestGuardMatrixTest {

    private static final String EQA_REST_PACKAGE = "org.openelisglobal.eqa.controller.rest";

    private static final String READ = "hasAuthority('qa.view.eqa') or hasRole('GLOBAL_ADMIN')";
    private static final String PARTICIPANT = "hasAuthority('qa.eqa.participant') or hasAnyRole('RECEPTION',"
            + " 'RESULTS', 'GLOBAL_ADMIN')";
    private static final String PROVIDER = "hasAuthority('qa.eqa.provider') or hasRole('GLOBAL_ADMIN')";
    private static final String MANAGE = "hasAuthority('qa.manage.eqa') or hasRole('GLOBAL_ADMIN')";
    private static final String UNBLIND = "hasAuthority('qa.eqa.inhouse.unblind') or hasRole('GLOBAL_ADMIN')";
    private static final String LEGACY_ROLES = "hasAnyRole('RECEPTION', 'RESULTS')";

    /**
     * EQAAlertRestController is mapped at /rest, not /rest/eqa, and both of its
     * GETs return alertService.getAll() unfiltered — it is the lab-wide alerts
     * dashboard that happens to live in this package. It keeps the legacy role
     * guard so an EQA permission is not required to read freezer or cold-chain
     * alerts. Its sibling AlertRestController serves the same list at GET
     * /rest/alerts with no guard at all, so tightening only this class would change
     * nothing an attacker cares about.
     */
    private static final Map<String, String> CLASS_GUARD_EXCEPTIONS = Map.of("EQAAlertRestController", LEGACY_ROLES);

    /** Every state-mutating handler and the guard it must declare. */
    private static final Map<String, String> WRITE_GUARDS = new HashMap<>();
    static {
        // Lab-wide alert acknowledgement rides the exception above.
        WRITE_GUARDS.put("EQAAlertRestController#acknowledgeAlert", LEGACY_ROLES);
        // Cycle lifecycle
        WRITE_GUARDS.put("EQACycleRestController#transition", MANAGE);
        // Provider-round management
        WRITE_GUARDS.put("EQADistributionRestController#createDistribution", PROVIDER);
        WRITE_GUARDS.put("EQADistributionRestController#updateDistribution", PROVIDER);
        WRITE_GUARDS.put("EQADistributionRestController#advanceStatus", PROVIDER);
        WRITE_GUARDS.put("EQADistributionRestController#generateBarcodes", PROVIDER);
        WRITE_GUARDS.put("EQAEnrollmentRestController#createEnrollments", PROVIDER);
        WRITE_GUARDS.put("EQAEnrollmentRestController#updateEnrollmentStatus", PROVIDER);
        WRITE_GUARDS.put("EQAProgramRestController#createProgram", PROVIDER);
        WRITE_GUARDS.put("EQAProgramRestController#updateProgram", PROVIDER);
        WRITE_GUARDS.put("EQAProgramRestController#updateTestAssignments", PROVIDER);
        WRITE_GUARDS.put("EQASubmissionRestController#approveLateSubmission", PROVIDER);
        // Participant lane — bench work, so the legacy roles still admit it
        WRITE_GUARDS.put("EQAMyProgramsRestController#createMyProgram", PARTICIPANT);
        WRITE_GUARDS.put("EQAMyProgramsRestController#updateMyProgram", PARTICIPANT);
        WRITE_GUARDS.put("EQAMyProgramsRestController#deleteMyProgram", PARTICIPANT);
        WRITE_GUARDS.put("EQAPanelReceiptRestController#recordReceipt", PARTICIPANT);
        WRITE_GUARDS.put("EQAParticipantResultRestController#createDraft", PARTICIPANT);
        WRITE_GUARDS.put("EQAParticipantResultRestController#transition", PARTICIPANT);
        WRITE_GUARDS.put("EQAResultRestController#submitResult", PARTICIPANT);
        WRITE_GUARDS.put("EQAResultRestController#batchImportResults", PARTICIPANT);
        WRITE_GUARDS.put("EQASubmissionRestController#submitViaFhir", PARTICIPANT);
        // Panel lifecycle, and the separate privilege that reveals sealed targets
        WRITE_GUARDS.put("EQAPanelRestController#seal", MANAGE);
        WRITE_GUARDS.put("EQAPanelRestController#distribute", MANAGE);
        WRITE_GUARDS.put("EQAPanelRestController#unblind", UNBLIND);
        WRITE_GUARDS.put("EQAParticipantResultRestController#score", MANAGE);
    }

    /** Authority → the changeset that must both register and grant it. */
    private static final Map<String, String> AUTHORITY_CHANGESETS = Map.of("qa.view.eqa",
            "liquibase/qa/004-add-qa-permission-model.xml", "qa.eqa.participant",
            "liquibase/qa/019-add-eqa-v2-menus-permissions.xml", "qa.eqa.provider",
            "liquibase/qa/019-add-eqa-v2-menus-permissions.xml", "qa.eqa.inhouse.unblind",
            "liquibase/qa/019-add-eqa-v2-menus-permissions.xml", "qa.manage.eqa",
            "liquibase/qa/023-add-eqa-manage-permission.xml");

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
            String expected = CLASS_GUARD_EXCEPTIONS.getOrDefault(controller.getSimpleName(), READ);
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
                // class umbrella; EQAPanelRestController#getSamples proves the
                // sealed-target rule belongs in the service, not in a per-GET
                // annotation.
                assertNotNull(controller.getSimpleName() + "#" + m.getName() + " is a read and should rely on the"
                        + " class-level guard", controller.getAnnotation(PreAuthorize.class));
            }
        }
    }

    @Test
    public void everyGuardedAuthorityIsRegisteredAndGrantedByItsMigration() throws Exception {
        // Asserted against the changeset SOURCE, not system_module rows: dbUnit
        // fixtures truncate that table in full-suite runs, so row assertions are
        // suite-order coin flips.
        Pattern authorityPattern = Pattern.compile("hasAuthority\\('([^']+)'\\)");
        Set<String> named = new HashSet<>();
        List<String> allGuards = new ArrayList<>(WRITE_GUARDS.values());
        allGuards.add(READ);
        for (String guard : allGuards) {
            Matcher matcher = authorityPattern.matcher(guard);
            while (matcher.find()) {
                named.add(matcher.group(1));
            }
        }
        assertEquals("the guards should use exactly the registered EQA authorities", AUTHORITY_CHANGESETS.keySet(),
                named);

        for (Map.Entry<String, String> entry : AUTHORITY_CHANGESETS.entrySet()) {
            String authority = entry.getKey();
            String changeset;
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(entry.getValue())) {
                assertNotNull(entry.getValue() + " must exist on the classpath", in);
                changeset = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertTrue(authority + " is not registered by " + entry.getValue(),
                    changeset.contains("value=\"" + authority + "\"/>"));
            // The grant is an INSERT into system_role_module selecting the module
            // by name, so require the SQL shape rather than any quoted mention —
            // the registration insert itself would satisfy a bare contains().
            assertTrue(authority + " is registered but no role grant selects it in " + entry.getValue(),
                    changeset.contains("m.name = '" + authority + "'")
                            || changeset.matches("(?s).*m\\.name IN \\([^)]*'" + Pattern.quote(authority) + "'.*"));
        }
    }
}
