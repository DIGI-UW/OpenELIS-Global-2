package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.openelisglobal.microbiology.controller.rest.MicroAstRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseInoculationRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseReadinessRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseTimelineRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCriticalCommunicationRestController;
import org.openelisglobal.microbiology.controller.rest.MicroIsolateRestController;
import org.openelisglobal.microbiology.controller.rest.MicroReportReleaseRestController;
import org.openelisglobal.microbiology.controller.rest.MicroWhonetReadinessRestController;
import org.openelisglobal.microbiology.controller.rest.MicroWorklistRestController;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyReferenceRestController;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyUatScenarioRestController;
import org.openelisglobal.microbiology.service.MicroAstAnalyzerEventService;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroCaseAmendmentService;
import org.openelisglobal.microbiology.service.MicroCaseInoculationService;
import org.openelisglobal.microbiology.service.MicroCaseNonconformanceService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseTimelineService;
import org.openelisglobal.microbiology.service.MicroCriticalCommunicationService;
import org.openelisglobal.microbiology.service.MicroCultureAnalyzerEventService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.springframework.transaction.annotation.Transactional;

public class MicrobiologyArchitectureTest {

    @Test
    public void microbiologyControllersDoNotDeclareTransactions() {
        Class<?>[] controllers = { MicroCaseRestController.class, MicroCaseInoculationRestController.class,
                MicroCaseTimelineRestController.class, MicroIsolateRestController.class, MicroAstRestController.class,
                MicroCaseReadinessRestController.class, MicrobiologyReferenceRestController.class,
                MicroWorklistRestController.class, MicroCriticalCommunicationRestController.class,
                MicroReportReleaseRestController.class, MicroWhonetReadinessRestController.class,
                MicrobiologyUatScenarioRestController.class };
        for (Class<?> controller : controllers) {
            assertFalse(controller.isAnnotationPresent(Transactional.class));
            for (Method method : controller.getDeclaredMethods()) {
                assertFalse(method.isAnnotationPresent(Transactional.class));
            }
        }
    }

    @Test
    public void userFacingMicrobiologyServicesRequireBenchOrSupervisorPrivileges() {
        // The bench/supervisor boundary moved off the controllers onto the services
        // (S011c): BENCH_ACCESS (ADMIN/RESULTS/VALIDATION) became micro:view +
        // micro:bench, SUPERVISOR_ACCESS (ADMIN/VALIDATION) became micro:supervise,
        // granted to those same roles in Liquibase 012-004d. Asserting on the
        // services keeps the invariant this test was written for — every
        // microbiology write is privilege-gated, and none of them degrades to
        // "any authenticated account".
        Class<?>[] services = { MicroCaseService.class, MicroCaseInoculationService.class,
                MicroCaseTimelineService.class, MicroCaseNonconformanceService.class, MicroIsolateService.class,
                MicroAstService.class, MicroCriticalCommunicationService.class, MicroReportReleaseService.class,
                MicroCaseAmendmentService.class };
        for (Class<?> service : services) {
            for (Method method : service.getDeclaredMethods()) {
                var authorization = method
                        .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
                assertNotNull(service.getName() + "." + method.getName() + " must declare a privilege boundary",
                        authorization);
                assertFalse(
                        service.getName() + "." + method.getName() + " must not authorize every authenticated account",
                        authorization.value().contains("isAuthenticated"));
            }
        }
    }

    @Test
    public void analyzerEventIngestionKeepsItsMachineBoundary() {
        // The boundary moved from hasRole('ANALYSER_IMPORT') on the controllers to
        // analyzer:import on the services (S011c). Asserted on the services because
        // that is now the only thing enforcing it: both are @Service CLASSES with no
        // interface, so ServicePrivilegeCoverageTest — which scans interfaces — does
        // not cover them, and removing the controller guard briefly left analyzer
        // result ingestion open to any authenticated user.
        Class<?>[] services = { MicroAstAnalyzerEventService.class, MicroCultureAnalyzerEventService.class };
        for (Class<?> service : services) {
            Method receive = Stream.of(service.getDeclaredMethods())
                    .filter(method -> method.getName().equals("receive")).findFirst().orElseThrow();
            var authorization = receive.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
            assertNotNull(service.getName() + ".receive must declare its machine-role boundary", authorization);
            assertEquals("hasAuthority('PRIV_ANALYZER_IMPORT')", authorization.value());
        }
    }

    @Test
    public void microbiologyFixturesDoNotBypassApplicationServices() throws IOException {
        Path repositoryRoot = Path.of(System.getProperty("user.dir"));
        Path microbiologyTests = repositoryRoot.resolve("src/test/java/org/openelisglobal/microbiology");
        try (Stream<Path> files = Files.walk(microbiologyTests)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals(getClass().getSimpleName() + ".java"))
                    .filter(path -> !isMigrationVerificationTest(path))
                    .filter(path -> !path.toString().contains("/qualification/")).toList()) {
                assertNoForbiddenFixtureAccess(file, List.of("JdbcTemplate", "javax.sql.DataSource",
                        "java.sql.Connection", "createNativeQuery", "INSERT INTO", "DELETE FROM", "nextval("));
            }
        }

        assertNoForbiddenFixtureAccess(repositoryRoot.resolve(
                "src/test/java/org/openelisglobal/testcatalog/controller/rest/TestCatalogEditorMicrobiologyTest.java"),
                List.of("JdbcTemplate", "javax.sql.DataSource", "java.sql.Connection", "createNativeQuery",
                        "INSERT INTO", "DELETE FROM", "nextval("));
        assertNoForbiddenFixtureAccess(
                repositoryRoot.resolve(
                        "src/main/java/org/openelisglobal/microbiology/service/MicrobiologyUatScenarioService.java"),
                List.of(".dao.", "JdbcTemplate", "javax.sql.DataSource", "java.sql.Connection", "createNativeQuery",
                        "INSERT INTO", "DELETE FROM", "nextval("));
        assertNoForbiddenFixtureAccess(repositoryRoot.resolve("frontend/playwright/helpers/seed-microbiology-data.ts"),
                List.of("child_process", "execFile", "docker", "psql", "INSERT INTO", "DELETE FROM", "nextval("));
    }

    @Test
    public void fixtureGuardOnlyExemptsMigrationVerificationTests() {
        assertTrue(isMigrationVerificationTest(Path.of("MicrobiologyM10LiquibaseRollbackTest.java")));
        assertTrue(isMigrationVerificationTest(Path.of("MicrobiologyCulturePurposeLiquibaseRollbackTest.java")));
        assertTrue(isMigrationVerificationTest(Path.of("MicrobiologyWhonetExportSelectionLiquibaseTest.java")));
        assertTrue(isMigrationVerificationTest(Path.of("MicrobiologyCulturePurposeLiquibaseTest.java")));
        assertFalse(isMigrationVerificationTest(Path.of("MicrobiologyLiquibaseFixtureTest.java")));
    }

    private boolean isMigrationVerificationTest(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith("LiquibaseRollbackTest.java")
                || fileName.equals("MicrobiologyWhonetExportSelectionLiquibaseTest.java")
                || fileName.equals("MicrobiologyCulturePurposeLiquibaseTest.java");
    }

    private void assertNoForbiddenFixtureAccess(Path file, List<String> forbiddenFragments) throws IOException {
        String source = Files.readString(file);
        for (String fragment : forbiddenFragments) {
            assertFalse(file + " bypasses the application service boundary with '" + fragment + "'",
                    source.contains(fragment));
        }
    }
}
