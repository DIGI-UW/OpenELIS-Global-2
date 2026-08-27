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
import org.openelisglobal.microbiology.controller.rest.MicroCaseNonconformanceRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseProtocolRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseReadinessRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseTimelineRestController;
import org.openelisglobal.microbiology.controller.rest.MicroIsolateRestController;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyReferenceRestController;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyUatScenarioRestController;
import org.springframework.transaction.annotation.Transactional;

public class MicrobiologyArchitectureTest {

    @Test
    public void microbiologyControllersDoNotDeclareTransactions() {
        Class<?>[] controllers = { MicroCaseRestController.class, MicroIsolateRestController.class,
                MicroAstRestController.class, MicroCaseReadinessRestController.class,
                MicroCaseInoculationRestController.class, MicroCaseNonconformanceRestController.class,
                MicroCaseProtocolRestController.class, MicroCaseTimelineRestController.class,
                MicrobiologyReferenceRestController.class, MicrobiologyUatScenarioRestController.class };
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
                    .filter(path -> !path.getFileName().toString().endsWith("LiquibaseRollbackTest.java")).toList()) {
                assertNoForbiddenFixtureAccess(file, List.of("JdbcTemplate", "javax.sql.DataSource",
                        "java.sql.Connection", "createNativeQuery", "INSERT INTO", "DELETE FROM", "nextval("));
            }
        }

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
