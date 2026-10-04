package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * The pre-login configuration endpoint must stay bound to the method that
 * builds the map.
 *
 * <p>
 * {@code GET /rest/open-configuration-properties} is the only configuration a
 * browser can read before it has a session, and the login page needs it:
 * Login.jsx renders the username and password fields only when
 * {@code useFormLogin == "true"} comes back from it.
 *
 * <p>
 * A Javadoc block inserted between {@code @GetMapping}/{@code @ResponseBody}
 * and {@code getOpenConfigurationProperties} silently rebound those annotations
 * to the next method declared after it - a private String helper - so the
 * endpoint answered {@code ""} instead of the map. Nothing failed: the response
 * was still 200, the page still rendered its heading, and there were no console
 * errors. The only symptom was a login form with no fields, and nobody could
 * sign in.
 *
 * <p>
 * This pins the binding rather than the Javadoc: whatever comment sits above
 * the handler, the mapping annotations must be the last thing before
 * {@code getOpenConfigurationProperties}.
 */
public class OpenConfigurationStaysOnItsHandlerTest {

    private static final Path CONTROLLER = Paths
            .get("src/main/java/org/openelisglobal/common/rest/DisplayListController.java");

    @Test
    public void theOpenConfigMappingSitsOnTheMethodThatBuildsTheMap() throws IOException {
        String body = new String(Files.readAllBytes(CONTROLLER), StandardCharsets.UTF_8);

        Matcher mapping = Pattern.compile("@GetMapping\\(value = \"open-configuration-properties\"[^)]*\\)")
                .matcher(body);
        assertTrue("the pre-login endpoint GET /rest/open-configuration-properties is gone from " + CONTROLLER
                + "; the login page cannot render its fields without it", mapping.find());

        String afterMapping = body.substring(mapping.end());
        int handler = afterMapping.indexOf("private Map<String, Object> getOpenConfigurationProperties()");
        assertTrue("getOpenConfigurationProperties must follow its own @GetMapping", handler >= 0);

        String between = afterMapping.substring(0, handler);
        assertTrue(
                "only @ResponseBody may sit between the mapping and getOpenConfigurationProperties."
                        + " Anything else - a Javadoc block, another method - rebinds the endpoint to whatever is"
                        + " declared next, which answers the wrong body and leaves the login form with no fields."
                        + " Found between them: <<<" + between.trim() + ">>>",
                between.replace("@ResponseBody", "").trim().isEmpty());
    }

    @Test
    public void theHelperThatSwallowsDenialsIsNotItselfAnEndpoint() throws IOException {
        String body = new String(Files.readAllBytes(CONTROLLER), StandardCharsets.UTF_8);

        int helper = body.indexOf("private String customCriticalMessageOrBlank()");
        assertTrue("customCriticalMessageOrBlank not found in " + CONTROLLER, helper >= 0);

        // Walk back over its Javadoc to whatever precedes it.
        String before = body.substring(0, helper);
        int javadoc = before.lastIndexOf("/**");
        String preceding = javadoc >= 0 ? before.substring(0, javadoc) : before;

        assertTrue(
                "customCriticalMessageOrBlank is a private helper, not a handler; a @GetMapping"
                        + " immediately above it means an endpoint's annotations have slipped onto it",
                !preceding.stripTrailing().endsWith("@ResponseBody"));
    }
}
