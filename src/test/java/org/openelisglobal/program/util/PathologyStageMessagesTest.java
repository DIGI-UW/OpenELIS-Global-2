package org.openelisglobal.program.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;

/**
 * FR-2.6 makes {@link PathologyStatus}'s declaration order the served source of
 * the bench stage list; FR-16 makes en.json the rendered text for it. Nothing
 * else keeps the two from drifting apart once either one changes on its own, so
 * this pins that every stage's {@link PathologyStages#displayKey} resolves in
 * the English bundle to that stage's own display text, and that the bundle
 * carries no {@code pathology.stage.*} key that does not belong to a stage.
 *
 * <p>
 * Plain JUnit, no Spring, no database. Maven Surefire runs with the project
 * root as the working directory, which is why a bare relative path already
 * works for other tests reading files outside {@code src/test/resources} (for
 * example {@code OrderEntryDomainActionsLiquibaseRollbackTest} reading a
 * Liquibase changeset, and {@code MicrobiologyArchitectureTest} resolving paths
 * under {@code frontend/}), so {@code frontend/src/languages/en.json} is read
 * directly from there.
 */
public class PathologyStageMessagesTest {

    private static final String EN_JSON_PATH = "frontend/src/languages/en.json";

    private static final String STAGE_KEY_PREFIX = "pathology.stage.";

    private static Map<String, String> messages;

    @BeforeClass
    public static void loadEnglishBundle() throws IOException {
        Path path = Path.of(EN_JSON_PATH);
        if (!Files.exists(path)) {
            fail("expected the English message bundle at " + path.toAbsolutePath());
        }
        String json = Files.readString(path);
        // Every value in this bundle is a string; a future non-string value should
        // fail this test loudly rather than be silently coerced.
        messages = new ObjectMapper().readValue(json, new TypeReference<Map<String, String>>() {
        });
    }

    @Test
    public void everyStage_hasItsLabelInTheEnglishBundle() {
        for (PathologyStatus status : PathologyStages.ordered()) {
            String key = PathologyStages.displayKey(status);

            assertTrue("en.json must carry a label for " + key, messages.containsKey(key));
            assertEquals("the bundle's text for " + key + " must name the stage " + status, status.getDisplay(),
                    messages.get(key));
        }
    }

    @Test
    public void theBundle_hasNoStageKeyWithoutAStage() {
        Set<String> keysAStageOwns = PathologyStages.ordered().stream().map(PathologyStages::displayKey)
                .collect(Collectors.toSet());

        for (String key : messages.keySet()) {
            if (key.startsWith(STAGE_KEY_PREFIX)) {
                assertTrue(key + " is a pathology.stage.* key with no PathologyStatus constant behind it (orphan)",
                        keysAStageOwns.contains(key));
            }
        }
    }
}
