package org.openelisglobal.addon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class AddonCatalogTest {

    @Test
    public void everyEventNeedsAKnownScope() {
        for (AddonCatalog.EventType e : AddonCatalog.EVENTS) {
            assertEquals(e.type(), true, AddonCatalog.isScope(e.scope()));
        }
        assertEquals(AddonCatalog.RESULTS_READ, AddonCatalog.scopeForEvent("result.validated"));
        assertNull(AddonCatalog.scopeForEvent("sample.deleted"));
    }

    @Test
    public void theContextKeepsOnlyThePageAndPermittedLabNumbers() {
        Map<String, Object> context = Map.of("page", "result-lookup", "accessionNumbers", List.of("DEV0001", "DEV0002"),
                "patientName", "Jane Doe");
        assertEquals(Map.of("page", "result-lookup", "accessionNumbers", List.of("DEV0001", "DEV0002")),
                AddonCatalog.filterContext(context, List.of(AddonCatalog.ORDERS_READ)));
        assertEquals(Map.of("page", "result-lookup"),
                AddonCatalog.filterContext(context, List.of(AddonCatalog.RESULTS_READ)));
    }

    @Test
    public void malformedRepeatedAndExcessLabNumbersAreDropped() {
        List<String> many = new ArrayList<>();
        many.add("DEV0001");
        many.add("DEV0001");
        many.add("x' OR 1=1 --");
        for (int i = 0; i < 80; i++) {
            many.add("A" + i);
        }
        @SuppressWarnings("unchecked")
        List<String> kept = (List<String>) AddonCatalog
                .filterContext(Map.of("accessionNumbers", many), List.of(AddonCatalog.ORDERS_READ))
                .get("accessionNumbers");
        assertEquals(AddonCatalog.MAX_CONTEXT_ACCESSIONS, kept.size());
        assertEquals("DEV0001", kept.get(0));
        assertEquals("A0", kept.get(1));
        assertFalse(kept.contains("x' OR 1=1 --"));
    }

    @Test
    public void laboratoryWideScopesAreKnownAndIdentityIsMarked() {
        for (String scope : List.of("orders.search", "patient.identity", "stats.read", "catalog.read")) {
            assertEquals(scope, true, AddonCatalog.isScope(scope));
        }
        assertEquals(java.util.Set.of("patient.identity"), AddonCatalog.IDENTIFYING_SCOPES);
    }

    @Test
    public void theSettingsPageSlotIsForAdministratorsAndCarriesNoOrders() {
        AddonCatalog.Slot slot = AddonCatalog.slot("addon-settings-page-slot");
        assertEquals(java.util.Set.of("ADMIN"), slot.roles());
        assertEquals(false, slot.withOrders());
        Map<String, Object> context = Map.of("page", "extensions", "accessionNumbers", List.of("DEV0001"));
        assertEquals(Map.of("page", "extensions"), AddonCatalog.filterContext(context, List.of("orders.search")));
    }

    @Test
    public void aBadPageNameIsDropped() {
        assertEquals(Map.of(), AddonCatalog.filterContext(Map.of("page", "<script>"), List.of()));
    }

    @Test
    public void anAiContextAddonGetsTheOrdersInItsTokenButNotInItsFrame() {
        Map<String, Object> context = Map.of("page", "result-validation", "accessionNumbers", List.of("DEV0001"));
        List<String> ai = List.of(AddonCatalog.AI_CONTEXT);
        assertEquals(Map.of("page", "result-validation"), AddonCatalog.filterContext(context, ai));
        assertEquals(List.of("DEV0001"), AddonCatalog.contextOrders(context, ai));
        assertEquals(List.of(), AddonCatalog.contextOrders(context, List.of(AddonCatalog.RESULTS_READ)));
    }

    @Test
    public void egressHostsAreDnsNamesWithAnOptionalPort() {
        for (String ok : List.of("openrouter.ai", "api.deepseek.com", "api.openai.com:443",
                "host.containers.internal:4190")) {
            assertTrue(ok, AddonCatalog.isEgressHost(ok));
        }
        for (String bad : List.of("", "localhost", "10.0.1.12", "10.0.1.12:4190", "*.openai.com", "OpenRouter.ai",
                "https://openrouter.ai", "openrouter.ai/api", "a..b", "api.openai.com:0", "api.openai.com:70000",
                "-bad.example.org")) {
            assertFalse(bad, AddonCatalog.isEgressHost(bad));
        }
    }

    @Test
    public void theHeaderSlotIsOpenToEverySignedInUser() {
        assertTrue(AddonCatalog.slot(AddonCatalog.SLOT_TOP_NAV_ACTIONS).roles().isEmpty());
        assertTrue(AddonCatalog.isScope(AddonCatalog.AI_CONTEXT));
    }
}
