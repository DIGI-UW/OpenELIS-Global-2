package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerTypeCatalogServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BridgeProfileCatalogService bridgeCatalogService;

    @Mock
    private AnalyzerProfileBindingDAO bindingDAO;

    private AnalyzerTypeCatalogService service;

    @Before
    public void setUp() {
        service = new AnalyzerTypeCatalogServiceImpl(bridgeCatalogService, bindingDAO);
    }

    @Test
    public void getCatalogComposesPortableMetadataWithLocalUsageAndHonestCompleteness() throws Exception {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setId("41");
        binding.setProfileId("site.mock-hematology");
        binding.setProfileRevision(3);
        when(bindingDAO.getAll()).thenReturn(List.of(binding));
        when(bindingDAO.countAnalyzersByBindingId("41")).thenReturn(3L);
        when(bridgeCatalogService.getCatalog()).thenReturn(catalog());

        AnalyzerTypeCatalogView result = service.getCatalog();

        assertEquals("1.0", result.schemaVersion());
        assertEquals(2, result.summary().total());
        assertEquals(1, result.summary().inUse());
        assertEquals(1, result.summary().needsAttention());
        assertEquals(1, result.summary().deactivated());

        AnalyzerTypeCatalogView.TypeSummary active = result.types().get(0);
        assertEquals("site.mock-hematology", active.profileId());
        assertEquals(3, active.revision());
        assertEquals("Mock Hematology", active.displayName());
        assertEquals("OpenELIS", active.manufacturer());
        assertEquals("Mock H", active.model());
        assertEquals("SHIPPED", active.source());
        assertEquals("ACTIVE", active.status());
        assertEquals("ASTM", active.protocol());
        assertEquals(2, active.testMappings().total());
        assertEquals(0, active.testMappings().mapped());
        assertEquals("NOT_STARTED", active.testMappings().state());
        assertEquals(2, active.resultMappings().total());
        assertEquals(0, active.resultMappings().mapped());
        assertEquals("NOT_STARTED", active.resultMappings().state());
        assertEquals(3L, active.usedBy());
        assertEquals("NEEDS_LOCAL_MAPPING", active.readiness());

        AnalyzerTypeCatalogView.TypeSummary inactive = result.types().get(1);
        assertEquals("site.retired-file", inactive.profileId());
        assertEquals("SITE", inactive.source());
        assertEquals("INACTIVE", inactive.status());
        assertEquals("DEACTIVATED", inactive.readiness());
        assertEquals("site.file-base", inactive.parentProfileId());
        assertEquals(Integer.valueOf(1), inactive.parentRevision());
        assertEquals("NOT_APPLICABLE", inactive.resultMappings().state());
        assertNull(active.parentProfileId());
    }

    private BridgeProfileCatalog catalog() throws Exception {
        JsonNode active = objectMapper.readTree("""
                {
                  "schemaVersion":"1.0",
                  "profileId":"site.mock-hematology",
                  "revision":3,
                  "revisionFingerprint":"sha256:1111111111111111111111111111111111111111111111111111111111111111",
                  "displayName":"Mock Hematology",
                  "source":"SHIPPED",
                  "status":"ACTIVE",
                  "protocol":"ASTM",
                  "identity":{"manufacturer":"OpenELIS","model":"Mock H"},
                  "tests":[
                    {"sourceRowKey":"wbc","resultType":"NUMERIC","resultValues":[]},
                    {"sourceRowKey":"flag","resultType":"QUALITATIVE","resultValues":[{},{}]}
                  ]
                }
                """);
        JsonNode inactive = objectMapper.readTree("""
                {
                  "schemaVersion":"1.0",
                  "profileId":"site.retired-file",
                  "revision":2,
                  "revisionFingerprint":"sha256:2222222222222222222222222222222222222222222222222222222222222222",
                  "displayName":"Retired File Analyzer",
                  "source":"SITE",
                  "status":"INACTIVE",
                  "protocol":"FILE",
                  "lineage":{"parentProfileId":"site.file-base","parentRevision":1},
                  "tests":[{"sourceRowKey":"result","resultType":"NUMERIC","resultValues":[]}]
                }
                """);
        JsonNode publication = objectMapper.createObjectNode().put("action", "CREATED");
        return new BridgeProfileCatalog("1.0",
                "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                List.of(new BridgeProfileCatalog.ProfileRevision(active, publication),
                        new BridgeProfileCatalog.ProfileRevision(inactive, publication)));
    }
}
