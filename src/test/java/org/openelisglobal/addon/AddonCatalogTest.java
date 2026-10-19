package org.openelisglobal.addon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

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
    public void aBadPageNameIsDropped() {
        assertEquals(Map.of(), AddonCatalog.filterContext(Map.of("page", "<script>"), List.of()));
    }
}
