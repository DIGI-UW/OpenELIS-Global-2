package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerTransportMode;

public class AnalyzerActivationCandidateFactoryTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-23T20:00:00Z"), ZoneOffset.UTC);
    private static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String BINDING_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String RECOGNITION_FINGERPRINT = "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    @Test
    public void buildsSchemaValidImmutableCandidateBoundToExactRegistration() throws Exception {
        CandidateFixture fixture = fixture();
        AnalyzerActivationCandidateFactory factory = new AnalyzerActivationCandidateFactory(CLOCK);

        AnalyzerActivationDocuments documents = factory.create(fixture.analyzer, fixture.siteBinding,
                fixture.confirmation, fixture.registration,
                new BridgeRegisteredCandidate("42", "site.mock", 2, fixture.registrationFingerprint));

        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(JSON.readTree(Path
                .of("specs/015-ogc-1054-analyzer-contract-migration/contracts/openelis-analyzer-candidate.schema.json")
                .toFile()));
        assertTrue(schema.validate(documents.candidate()).toString(), schema.validate(documents.candidate()).isEmpty());
        assertEquals("42", documents.candidate().path("oeAnalyzerId").asText());
        assertEquals(List.of("3", "8"), JSON.convertValue(documents.candidate().path("instance").path("labUnitIds"),
                JSON.getTypeFactory().constructCollectionType(List.class, String.class)));
        assertEquals("1",
                documents.candidate().path("siteBinding").path("tests").path("RAW-A").path("openelisTestId").asText());
        assertEquals("11", documents.candidate().path("siteBinding").path("tests").path("RAW-A").path("resultValues")
                .path("POS").path("openelisResultOptionId").asText());
        assertEquals("EXCLUDED",
                documents.candidate().path("siteBinding").path("tests").path("RAW-B").path("status").asText());
        assertEquals("Not offered by this laboratory",
                documents.candidate().path("siteBinding").path("tests").path("RAW-B").path("reason").asText());
        assertEquals(fixture.registrationFingerprint,
                documents.candidate().path("desiredRegistrationFingerprint").asText());
        assertEquals(fixture.registration, documents.registration());
        assertFalse(documents.candidate().toString().contains("AnalyzerQcRule"));
        assertFalse(documents.candidate().has("profileDocument"));

        fixture.analyzer.setName("Changed draft");
        fixture.registration.put("name", "Changed registration");
        assertEquals("Laboratory analyzer A", documents.candidate().path("instance").path("name").asText());
        assertEquals("Laboratory analyzer A", documents.registration().path("name").asText());
    }

    @Test
    public void rejectsARegistrationOrAcknowledgementThatDoesNotMatchTheVerifiedCandidate() throws Exception {
        CandidateFixture fixture = fixture();
        AnalyzerActivationCandidateFactory factory = new AnalyzerActivationCandidateFactory(CLOCK);

        fixture.registration.put("name", "Tampered without recomputing fingerprint");
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(fixture.analyzer, fixture.siteBinding, fixture.confirmation, fixture.registration,
                        new BridgeRegisteredCandidate("42", "site.mock", 2, fixture.registrationFingerprint)));

        CandidateFixture exact = fixture();
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(exact.analyzer, exact.siteBinding, exact.confirmation, exact.registration,
                        new BridgeRegisteredCandidate("42", "site.mock", 3, exact.registrationFingerprint)));
    }

    @Test
    public void rejectsVerificationThatDoesNotCoverTheExactCandidateRows() throws Exception {
        CandidateFixture missingRow = fixture();
        AnalyzerActivationCandidateFactory factory = new AnalyzerActivationCandidateFactory(CLOCK);
        missingRow.confirmation.setConfirmedRowsJson("[]");

        assertThrows(IllegalArgumentException.class,
                () -> factory.create(missingRow.analyzer, missingRow.siteBinding, missingRow.confirmation,
                        missingRow.registration,
                        new BridgeRegisteredCandidate("42", "site.mock", 2, missingRow.registrationFingerprint)));

        CandidateFixture missingAudit = fixture();
        missingAudit.confirmation.setAuditEventId(null);
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(missingAudit.analyzer, missingAudit.siteBinding, missingAudit.confirmation,
                        missingAudit.registration,
                        new BridgeRegisteredCandidate("42", "site.mock", 2, missingAudit.registrationFingerprint)));
    }

    private static CandidateFixture fixture() throws Exception {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setId("31");
        profile.setProfileId("site.mock");
        profile.setProfileRevision(2);
        profile.setProfileFingerprint(PROFILE_FINGERPRINT);

        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("51");
        binding.setProfileBinding(profile);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("61");
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(4);
        revision.setBindingFingerprint(BINDING_FINGERPRINT);

        AnalyzerSiteBindingTest bound = test(revision, "RAW-A", AnalyzerSiteBindingMappingState.BOUND, "1");
        AnalyzerSiteBindingTest excluded = test(revision, "RAW-B", AnalyzerSiteBindingMappingState.EXCLUDED, null);
        AnalyzerSiteBindingResult result = new AnalyzerSiteBindingResult();
        result.setId(new AnalyzerSiteBindingResultPK("61", "RAW-A", "POS"));
        result.setSiteBindingRevision(revision);
        result.setMappingState(AnalyzerSiteBindingMappingState.BOUND);
        result.setTestResultId("11");
        AnalyzerSiteBindingSnapshot siteBinding = new AnalyzerSiteBindingSnapshot(binding, revision,
                List.of(bound, excluded), List.of(result));

        AnalyzerSiteBindingConfirmation confirmation = new AnalyzerSiteBindingConfirmation();
        confirmation.setId("71");
        confirmation.setSiteBindingRevision(revision);
        confirmation.setProfileId("site.mock");
        confirmation.setProfileRevision(2);
        confirmation.setProfileRevisionFingerprint(PROFILE_FINGERPRINT);
        confirmation.setBindingFingerprint(BINDING_FINGERPRINT);
        confirmation.setRecognitionFingerprint(RECOGNITION_FINGERPRINT);
        confirmation
                .setConfirmedRowsJson(JSON.writeValueAsString(List.of(new AnalyzerSiteBindingSourceRow("RAW-A", null),
                        new AnalyzerSiteBindingSourceRow("RAW-A", "POS"))));
        confirmation
                .setExcludedRowsJson(JSON.writeValueAsString(List.of(new AnalyzerSiteBindingSourceRow("RAW-B", null))));
        confirmation.setConfirmedBy("17");
        confirmation.setConfirmedAt(Timestamp.from(Instant.parse("2026-08-23T19:55:00Z")));
        confirmation.setAuditEventId("91");

        Analyzer analyzer = new Analyzer();
        analyzer.setId("42");
        analyzer.setName("Laboratory analyzer A");
        analyzer.setTestUnitIds(List.of("8", "3"));
        analyzer.setSiteBindingRevision(revision);
        analyzer.setTransportMode(AnalyzerTransportMode.TCP);
        analyzer.setIpAddress("192.0.2.10");
        analyzer.setPort(5000);

        ObjectNode registration = JSON.createObjectNode();
        registration.put("sourceId", "192.0.2.10");
        registration.put("name", "Laboratory analyzer A");
        ObjectNode profileRef = registration.putObject("profileRef");
        profileRef.put("profileId", "site.mock");
        profileRef.put("revision", 2);
        registration.put("protocol", "ASTM");
        registration.put("dataFlow", "RESULTS_ONLY");
        registration.put("desiredStatus", "ACTIVE");
        ObjectNode connection = registration.putObject("connection");
        connection.put("mode", "TCP");
        connection.put("role", "INITIATOR");
        ObjectNode settings = connection.putObject("settings");
        settings.put("remoteHost", "192.0.2.10");
        settings.put("remotePort", 5000);
        String registrationFingerprint = fingerprint(registration);
        registration.put("desiredStateFingerprint", registrationFingerprint);
        return new CandidateFixture(analyzer, siteBinding, confirmation, registration, registrationFingerprint);
    }

    private static AnalyzerSiteBindingTest test(AnalyzerSiteBindingRevision revision, String sourceRow,
            AnalyzerSiteBindingMappingState state, String testId) {
        AnalyzerSiteBindingTest test = new AnalyzerSiteBindingTest();
        test.setId(new AnalyzerSiteBindingTestPK(revision.getId(), sourceRow));
        test.setSiteBindingRevision(revision);
        test.setMappingState(state);
        test.setTestId(testId);
        return test;
    }

    private static String fingerprint(JsonNode value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = JSON.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
        return "sha256:" + HexFormat.of().formatHex(digest.digest(bytes));
    }

    private record CandidateFixture(Analyzer analyzer, AnalyzerSiteBindingSnapshot siteBinding,
            AnalyzerSiteBindingConfirmation confirmation, ObjectNode registration, String registrationFingerprint) {
    }
}
