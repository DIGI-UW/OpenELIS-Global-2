package org.openelisglobal.questionnaire.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.questionnaire.valueholder.Questionnaire.QuestionnaireStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The FHIR store is optional: everything must work from the local database, and
 * when a store is configured it is read first and written alongside.
 */
public class QuestionnaireStorageServiceTest extends BaseWebContextSensitiveTest {

    private static final String FHIR_STORE = "http://fhir.test/fhir";
    private static final String IDENTIFIER_SYSTEM = "http://openelis-global.org/notebook_questionare";
    private static final String[] TABLES = { "questionnaire_response_answer", "questionnaire_response_item",
            "questionnaire_response", "questionnaire_item_initial", "questionnaire_answer_option", "questionnaire_item",
            "questionnaire" };

    @Autowired
    private QuestionnaireStorageService storage;
    @Autowired
    private QuestionnaireService questionnaireService;
    @Autowired
    private QuestionnaireResponseService questionnaireResponseService;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private DataSource dataSource;

    @Before
    public void setUp() throws Exception {
        cleanRowsInCurrentConnection(TABLES);
        reset(fhirConfig, fhirUtil);
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("");
    }

    @After
    public void tearDown() throws Exception {
        reset(fhirConfig, fhirUtil);
        cleanRowsInCurrentConnection(TABLES);
    }

    @Test
    public void saveQuestionnaire_withoutFhirStore_persistsLocallyAndRoundTrips() {
        String uuid = UUID.randomUUID().toString();
        storage.saveQuestionnaire(programmeQuestionnaire(uuid, "Vector field survey", PublicationStatus.ACTIVE));

        org.openelisglobal.questionnaire.valueholder.Questionnaire row = questionnaireService
                .getMatch("fhirUuid", UUID.fromString(uuid)).orElseThrow();
        assertEquals("VectorFieldSurvey", row.getQuestionnaireName());
        assertEquals(QuestionnaireStatus.ACTIVE, row.getStatus());
        assertEquals(IDENTIFIER_SYSTEM, row.getIdentifierSystem());
        assertTrue(row.isHasItem());
        assertTrue(row.getResourceJson().contains("\"linkId\":\"species\""));

        Questionnaire loaded = storage.getQuestionnaire(uuid).orElseThrow();
        assertEquals(uuid, loaded.getIdElement().getIdPart());
        assertEquals("Vector field survey", loaded.getTitle());
        assertEquals("species", loaded.getItemFirstRep().getLinkId());
        assertEquals(QuestionnaireItemType.CHOICE, loaded.getItemFirstRep().getType());
        assertEquals("Aedes", loaded.getItemFirstRep().getAnswerOptionFirstRep().getValueCoding().getCode());
    }

    @Test
    public void saveQuestionnaire_existingUuid_updatesTheSameRow() {
        String uuid = UUID.randomUUID().toString();
        storage.saveQuestionnaire(programmeQuestionnaire(uuid, "First title", PublicationStatus.DRAFT));
        storage.saveQuestionnaire(programmeQuestionnaire(uuid, "Second title", PublicationStatus.ACTIVE));

        List<org.openelisglobal.questionnaire.valueholder.Questionnaire> rows = questionnaireService.getAll();
        assertEquals(1, rows.size());
        assertEquals(QuestionnaireStatus.ACTIVE, rows.get(0).getStatus());
        assertEquals("Second title", storage.getQuestionnaire(uuid).orElseThrow().getTitle());
    }

    @Test
    public void getQuestionnaire_unknownUuid_isEmpty() {
        assertTrue(storage.getQuestionnaire(UUID.randomUUID().toString()).isEmpty());
        assertTrue(storage.getQuestionnaire((UUID) null).isEmpty());
        assertTrue(storage.getQuestionnaire("").isEmpty());
    }

    @Test
    public void getQuestionnaire_withFhirStore_prefersTheStoreCopyAndCopiesItLocally() {
        String uuid = UUID.randomUUID().toString();
        storage.saveQuestionnaire(programmeQuestionnaire(uuid, "Local title", PublicationStatus.ACTIVE));
        Questionnaire remote = programmeQuestionnaire(uuid, "Store title", PublicationStatus.ACTIVE);
        String remoteOnly = UUID.randomUUID().toString();
        Questionnaire remoteOnlyQuestionnaire = programmeQuestionnaire(remoteOnly, "Only in store",
                PublicationStatus.ACTIVE);

        IGenericClient client = clientServing(Questionnaire.class,
                Map.of(uuid, remote, remoteOnly, remoteOnlyQuestionnaire), null);
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(FHIR_STORE);
        when(fhirUtil.getLocalFhirClient()).thenReturn(client);

        assertEquals("Store title", storage.getQuestionnaire(uuid).orElseThrow().getTitle());
        assertEquals("Only in store", storage.getQuestionnaire(remoteOnly).orElseThrow().getTitle());
        assertTrue("store-only questionnaire is copied into the local database",
                questionnaireService.getMatch("fhirUuid", UUID.fromString(remoteOnly)).isPresent());
    }

    @Test
    public void getQuestionnaire_whenFhirStoreIsDown_fallsBackToLocalCopy() {
        String uuid = UUID.randomUUID().toString();
        storage.saveQuestionnaire(programmeQuestionnaire(uuid, "Local title", PublicationStatus.ACTIVE));

        IGenericClient client = clientServing(Questionnaire.class, Map.of(),
                new ResourceNotFoundException("store unavailable"));
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(FHIR_STORE);
        when(fhirUtil.getLocalFhirClient()).thenReturn(client);

        assertEquals("Local title", storage.getQuestionnaire(uuid).orElseThrow().getTitle());
    }

    @Test
    public void saveQuestionnaire_whenFhirStoreWriteFails_keepsTheLocalCopy() {
        String uuid = UUID.randomUUID().toString();
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(FHIR_STORE);

        storage.saveQuestionnaire(programmeQuestionnaire(uuid, "Kept locally", PublicationStatus.ACTIVE));

        assertTrue(questionnaireService.getMatch("fhirUuid", UUID.fromString(uuid)).isPresent());
    }

    @Test
    public void storeQuestionnaireResponseLocally_withoutSubjectOrLocalQuestionnaire_persistsAnswers() {
        String responseUuid = UUID.randomUUID().toString();
        String questionnaireUuid = UUID.randomUUID().toString();
        QuestionnaireResponse response = answers(responseUuid, questionnaireUuid, "Aedes");

        storage.storeQuestionnaireResponseLocally(response);

        org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse row = questionnaireResponseService
                .getMatch("fhirUuid", UUID.fromString(responseUuid)).orElseThrow();
        assertNull(row.getSubjectReference());
        assertNull(row.getQuestionnaire());
        assertEquals(UUID.fromString(questionnaireUuid), row.getQuestionnaireFhirUuid());
        assertEquals(
                org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED,
                row.getStatus());

        QuestionnaireResponse loaded = storage.getQuestionnaireResponse(responseUuid).orElseThrow();
        assertEquals("Aedes", loaded.getItemFirstRep().getAnswerFirstRep().getValueStringType().getValue());
        assertEquals("Questionnaire/" + questionnaireUuid, loaded.getQuestionnaire());
    }

    @Test
    public void saveQuestionnaireResponse_linksTheLocalQuestionnaireAndKeepsTheSubject() {
        String questionnaireUuid = UUID.randomUUID().toString();
        storage.saveQuestionnaire(programmeQuestionnaire(questionnaireUuid, "Linked", PublicationStatus.ACTIVE));
        String responseUuid = UUID.randomUUID().toString();
        QuestionnaireResponse response = answers(responseUuid, questionnaireUuid, "Culex");
        response.setSubject(new Reference("Patient/" + UUID.randomUUID()));

        storage.saveQuestionnaireResponse(response);
        storage.saveQuestionnaireResponse(answers(responseUuid, questionnaireUuid, "Anopheles"));

        List<org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse> rows = questionnaireResponseService
                .getAll();
        assertEquals(1, rows.size());
        assertEquals(UUID.fromString(questionnaireUuid), rows.get(0).getQuestionnaireFhirUuid());
        assertEquals(questionnaireUuid,
                new JdbcTemplate(dataSource).queryForObject("SELECT q.fhir_uuid::text FROM clinlims.questionnaire q"
                        + " JOIN clinlims.questionnaire_response r ON r.questionnaire_id = q.id"
                        + " WHERE r.fhir_uuid = ?::uuid", String.class, responseUuid));
        assertEquals("Anopheles", storage.getQuestionnaireResponse(responseUuid).orElseThrow().getItemFirstRep()
                .getAnswerFirstRep().getValueStringType().getValue());
    }

    @Test
    public void getQuestionnaireResponse_whenFhirStoreIsDown_fallsBackToLocalCopy() {
        String responseUuid = UUID.randomUUID().toString();
        storage.storeQuestionnaireResponseLocally(answers(responseUuid, UUID.randomUUID().toString(), "Aedes"));

        IGenericClient client = clientServing(QuestionnaireResponse.class, Map.of(),
                new ResourceNotFoundException("store unavailable"));
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(FHIR_STORE);
        when(fhirUtil.getLocalFhirClient()).thenReturn(client);

        Optional<QuestionnaireResponse> loaded = storage.getQuestionnaireResponse(responseUuid);
        assertEquals("Aedes",
                loaded.orElseThrow().getItemFirstRep().getAnswerFirstRep().getValueStringType().getValue());
    }

    @Test
    public void getActiveQuestionnairesByIdentifierSystem_withoutFhirStore_listsLocalActiveOnes() {
        String active = UUID.randomUUID().toString();
        storage.saveQuestionnaire(programmeQuestionnaire(active, "Active notebook form", PublicationStatus.ACTIVE));
        storage.saveQuestionnaire(
                programmeQuestionnaire(UUID.randomUUID().toString(), "Retired form", PublicationStatus.RETIRED));
        Questionnaire otherSystem = programmeQuestionnaire(UUID.randomUUID().toString(), "Other system",
                PublicationStatus.ACTIVE);
        otherSystem.getIdentifier().clear();
        otherSystem.addIdentifier(new Identifier().setSystem("http://elsewhere").setValue("x"));
        storage.saveQuestionnaire(otherSystem);

        List<Questionnaire> found = storage.getActiveQuestionnairesByIdentifierSystem(IDENTIFIER_SYSTEM);

        assertEquals(1, found.size());
        assertEquals(active, found.get(0).getIdElement().getIdPart());
        assertFalse(storage.isFhirStoreConfigured());
    }

    @Test
    public void toStorageUuid_acceptsUuidsAndMapsOtherIdsDeterministically() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, QuestionnaireStorageServiceImpl.toStorageUuid(uuid.toString()));
        assertEquals(QuestionnaireStorageServiceImpl.toStorageUuid("generic-sample-logbook"),
                QuestionnaireStorageServiceImpl.toStorageUuid("generic-sample-logbook"));
        assertNotEquals(QuestionnaireStorageServiceImpl.toStorageUuid("a"),
                QuestionnaireStorageServiceImpl.toStorageUuid("b"));
    }

    /**
     * A FHIR client whose {@code read().resource(type).withId(id).execute()}
     * returns the mapped resource, throws {@code failure} when given, and otherwise
     * reports the id as not found.
     */
    @SuppressWarnings("unchecked")
    private static <T extends IBaseResource> IGenericClient clientServing(Class<T> type, Map<String, T> byId,
            RuntimeException failure) {
        IGenericClient client = mock(IGenericClient.class);
        IRead read = mock(IRead.class);
        IReadTyped<T> typed = mock(IReadTyped.class);
        when(client.read()).thenReturn(read);
        when(read.resource(type)).thenReturn(typed);
        when(typed.withId(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            IReadExecutable<T> executable = mock(IReadExecutable.class);
            if (failure != null) {
                when(executable.execute()).thenThrow(failure);
            } else if (byId.containsKey(id)) {
                when(executable.execute()).thenReturn(byId.get(id));
            } else {
                when(executable.execute()).thenThrow(new ResourceNotFoundException(type.getSimpleName() + "/" + id));
            }
            return executable;
        });
        return client;
    }

    private static Questionnaire programmeQuestionnaire(String uuid, String title, PublicationStatus status) {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(uuid);
        questionnaire.setName("VectorFieldSurvey");
        questionnaire.setTitle(title);
        questionnaire.setStatus(status);
        questionnaire.setDescription("Additional order entry questions");
        questionnaire.addIdentifier(new Identifier().setSystem(IDENTIFIER_SYSTEM).setValue(title));
        questionnaire.addItem().setLinkId("species").setText("Species").setType(QuestionnaireItemType.CHOICE)
                .addAnswerOption().setValue(new Coding().setCode("Aedes").setDisplay("Aedes"));
        return questionnaire;
    }

    private static QuestionnaireResponse answers(String responseUuid, String questionnaireUuid, String species) {
        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setId(responseUuid);
        response.setStatus(QuestionnaireResponseStatus.COMPLETED);
        response.setQuestionnaire("Questionnaire/" + questionnaireUuid);
        response.addItem().setLinkId("species").addAnswer().setValue(new StringType(species));
        return response;
    }
}
