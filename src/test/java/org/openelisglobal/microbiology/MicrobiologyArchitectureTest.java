package org.openelisglobal.microbiology;

import static org.junit.Assert.assertFalse;

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
    public void microbiologyFixturesDoNotBypassApplicationServices() throws IOException {
        Path repositoryRoot = Path.of(System.getProperty("user.dir"));
        Path microbiologyTests = repositoryRoot.resolve("src/test/java/org/openelisglobal/microbiology");
        try (Stream<Path> files = Files.walk(microbiologyTests)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals(getClass().getSimpleName() + ".java"))
                    .filter(path -> !path.getFileName().toString().endsWith("LiquibaseRollbackTest.java"))
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

    private void assertNoForbiddenFixtureAccess(Path file, List<String> forbiddenFragments) throws IOException {
        String source = Files.readString(file);
        for (String fragment : forbiddenFragments) {
            assertFalse(file + " bypasses the application service boundary with '" + fragment + "'",
                    source.contains(fragment));
        }
    }
}
