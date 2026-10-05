package org.openelisglobal.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * {@code DisplayListService}'s cached reference lists are assembled by ~52
 * builders that read across ~13 services whose gates are admin-scoped
 * (dictionary:view, sample_type:view, panel:view, provider:view, program:view,
 * nce:view, patient:view, test:configure, …). No operational base role holds
 * that whole set.
 *
 * <p>
 * The cache is populated at {@code @PostConstruct}, but any later rebuild runs
 * on whichever request thread triggered it. When that is an ordinary user's
 * thread the gates deny them and the screen they were opening fails — a Results
 * user got 403 on {@code GET /rest/displayList/METHODS} and the result-entry
 * page never rendered (E2E, rbac_results persona, 2026-09-21). The caller's own
 * access is already gated at its endpoint, so the rebuild itself must not be.
 *
 * <p>
 * Asserted by source scan rather than by running a rebuild, because exercising
 * it needs the full Spring context plus a database; this catches the regression
 * at the cheap layer. Both entry points must delegate their body to a private
 * method through the system-context wrapper.
 */
public class DisplayListRebuildRunsAsSystemTest {

    private static final Path SOURCE = Paths
            .get("src/main/java/org/openelisglobal/common/services/DisplayListService.java");

    /** Entry point → the private method its body must have moved to. */
    private static final String[][] WRAPPED_ENTRY_POINTS = {
            { "public synchronized void refreshLists()", "refreshAllListsInternal" },
            { "public void refreshList(ListType listType)", "refreshListInternal" } };

    private static String source() throws IOException {
        String src = Files.readString(SOURCE);
        assertTrue("DisplayListService moved or shrank — a silent pass is not a pass", src.length() > 10_000);
        return src;
    }

    /** The body of the method whose signature line is {@code signature}. */
    private static String bodyOf(String src, String signature) {
        int start = src.indexOf(signature);
        assertTrue(signature + " not found in DisplayListService", start >= 0);
        int open = src.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < src.length(); i++) {
            depth += src.charAt(i) == '{' ? 1 : src.charAt(i) == '}' ? -1 : 0;
            if (depth == 0) {
                return src.substring(open, i);
            }
        }
        throw new AssertionError("unbalanced braces after " + signature);
    }

    @Test
    public void bothRebuildEntryPointsRunInSystemContext() throws IOException {
        String src = source();
        for (String[] entry : WRAPPED_ENTRY_POINTS) {
            String body = bodyOf(src, entry[0]);
            assertTrue(
                    entry[0] + " must run its rebuild through buildTestListAsSystem, or a cache miss on an"
                            + " ordinary user's request thread denies them: " + body.trim(),
                    body.contains("buildTestListAsSystem") && body.contains(entry[1]));
        }
    }

    /**
     * The delegation must be the whole body, not a wrapper around one branch: the
     * entry point may not call a list builder itself.
     */
    @Test
    public void theEntryPointsThemselvesBuildNothing() throws IOException {
        String src = source();
        Pattern builder = Pattern.compile("\\b(create\\w+List|create\\w+|refreshTestNames)\\s*\\(");
        List<String> leaked = new ArrayList<>();
        for (String[] entry : WRAPPED_ENTRY_POINTS) {
            Matcher m = builder.matcher(bodyOf(src, entry[0]));
            while (m.find()) {
                leaked.add(entry[0] + " calls " + m.group(1) + " outside the system-context wrapper");
            }
        }
        assertTrue(String.join("\n", leaked), leaked.isEmpty());
    }

    /**
     * Inversion guard: the private bodies must still contain the builders, so this
     * test cannot pass by the methods having been emptied or renamed away.
     */
    @Test
    public void theWrappedBodiesStillDoTheWork() throws IOException {
        String src = source();
        String all = bodyOf(src, "private void refreshAllListsInternal()");
        String one = bodyOf(src, "private void refreshListInternal(ListType listType)");
        assertTrue("refreshAllListsInternal should populate many lists, found: " + all.length(),
                all.split("typeToListMap\\.put").length > 30);
        assertTrue("refreshListInternal should still switch over list types",
                one.contains("switch (listType)") && one.split("case ").length > 20);
        assertFalse("the wrapper helper must not have been removed", src.indexOf("buildTestListAsSystem") < 0);
    }
}
