package org.openelisglobal.module.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * The kit against a module that breaks the contract: an unguarded endpoint, a
 * controller outside the module prefix, changesets without the module's id and
 * author, and a message bundle that overrides a core key. Each check names its
 * problem; the well-formed fixture next to it passes every check (see
 * {@link KitFixtureContractTest}).
 */
public class ContractFailuresTest {

    public static class BadFixtureContract extends ModuleContractTest {
        @Override
        protected String manifestResource() {
            return "/bad-module.json";
        }
    }

    @Test
    public void everyBrokenRuleIsNamed() {
        Result result = JUnitCore.runClasses(BadFixtureContract.class);
        List<String> messages = new ArrayList<>();
        for (Failure failure : result.getFailures()) {
            messages.add(failure.getDescription().getMethodName() + ": " + failure.getMessage());
        }
        String all = String.join("\n", messages);

        assertEquals(all, 3, result.getFailureCount());
        assertTrue(all, all.contains("changeset '001-thing' must have an id starting with 'kitfixture-'"));
        assertTrue(all, all.contains("must have author 'kitfixture', not 'someone'"));
        assertTrue(all, all.contains("OpenController.open has no @PreAuthorize"));
        assertTrue(all, all.contains(
                "ElsewhereController.things maps '/rest/elsewhere/things' outside /rest/modules/kitfixture/"));
        assertTrue(all,
                all.contains("BareController.bare maps '/rest/modules/kitfixture' outside /rest/modules/kitfixture/"));
        assertTrue(all, all.contains("message key 'sidenav.label.home' is outside mod.kitfixture."));
    }
}
