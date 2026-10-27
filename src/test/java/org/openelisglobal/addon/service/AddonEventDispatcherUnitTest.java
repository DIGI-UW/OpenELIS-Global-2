package org.openelisglobal.addon.service;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.Test;
import org.openelisglobal.addon.dao.AddonOutboxDAO.Claim;

public class AddonEventDispatcherUnitTest {

    @Test
    public void theBodyIsACloudEventWithTheStoredData() throws Exception {
        Claim claim = new Claim(42, "hello", 0, "result.validated", "{\"accessionNumber\":\"DEV0001\"}",
                Instant.parse("2026-10-19T03:00:00Z"));
        JsonNode body = new ObjectMapper().readTree(AddonEventDispatcher.envelope(claim));
        assertEquals("1.0", body.get("specversion").asText());
        assertEquals("42", body.get("id").asText());
        assertEquals("result.validated", body.get("type").asText());
        assertEquals("2026-10-19T03:00:00Z", body.get("time").asText());
        assertEquals("DEV0001", body.get("data").get("accessionNumber").asText());
    }

    @Test
    public void retriesBackOffFromFiveSecondsToAnHour() {
        assertEquals(Duration.ofSeconds(5), AddonOutboxService.backoff(1));
        assertEquals(Duration.ofSeconds(10), AddonOutboxService.backoff(2));
        assertEquals(Duration.ofSeconds(40), AddonOutboxService.backoff(4));
        assertEquals(Duration.ofSeconds(2560), AddonOutboxService.backoff(10));
        assertEquals(Duration.ofHours(1), AddonOutboxService.backoff(11));
    }
}
