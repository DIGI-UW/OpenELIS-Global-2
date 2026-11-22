package org.openelisglobal.questionnaire.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.questionnaire.valueholder.Questionnaire.QuestionnaireStatus;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Local-first storage for programme questionnaires.
 *
 * <p>
 * Writes go to the {@code questionnaire} / {@code questionnaire_response}
 * tables (canonical JSON plus derived metadata columns) and, when a FHIR store
 * is configured, are mirrored to it; a FHIR failure is logged and never undoes
 * the local write. Reads consult the FHIR store first when configured so
 * deployments that already hold their forms there keep working, and a resource
 * that only exists remotely is copied locally on first read.
 */
@Service
public class QuestionnaireStorageServiceImpl implements QuestionnaireStorageService {

    private static final int TEXT_LENGTH = 255;
    private static final int CODE_LENGTH = 10;
    private static final int REFERENCE_LENGTH = 100;

    @Autowired
    private QuestionnaireService questionnaireService;
    @Autowired
    private QuestionnaireResponseService questionnaireResponseService;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    private final IParser parser = FhirContext.forR4Cached().newJsonParser();

    @Override
    public boolean isFhirStoreConfigured() {
        return StringUtils.isNotBlank(fhirConfig.getLocalFhirStorePath());
    }

    @Override
    @Transactional
    public Questionnaire saveQuestionnaire(Questionnaire questionnaire) {
        storeQuestionnaireLocally(questionnaire);
        pushToFhirStore(questionnaire);
        return questionnaire;
    }

    @Override
    public Optional<Questionnaire> getQuestionnaire(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return Optional.empty();
        }
        Optional<Questionnaire> remote = readFromFhirStore(Questionnaire.class, uuid);
        if (remote.isPresent()) {
            backfillLocally(uuid, findLocalQuestionnaire(uuid).isEmpty(),
                    () -> storeQuestionnaireLocally(remote.get()));
            return remote;
        }
        return findLocalQuestionnaire(uuid).map(this::toFhir);
    }

    @Override
    public Optional<Questionnaire> getQuestionnaire(UUID uuid) {
        return uuid == null ? Optional.empty() : getQuestionnaire(uuid.toString());
    }

    @Override
    public List<Questionnaire> getActiveQuestionnairesByIdentifierSystem(String identifierSystem) {
        Map<String, Questionnaire> byId = new LinkedHashMap<>();
        for (org.openelisglobal.questionnaire.valueholder.Questionnaire entity : questionnaireService
                .getAllMatching("identifierSystem", identifierSystem)) {
            if (entity.getStatus() == QuestionnaireStatus.ACTIVE) {
                Questionnaire questionnaire = toFhir(entity);
                byId.put(idOf(questionnaire), questionnaire);
            }
        }
        if (isFhirStoreConfigured()) {
            try {
                for (Questionnaire questionnaire : searchFhirStoreByIdentifierSystem(identifierSystem)) {
                    byId.put(idOf(questionnaire), questionnaire);
                }
            } catch (RuntimeException e) {
                LogEvent.logWarn(getClass().getSimpleName(), "getActiveQuestionnairesByIdentifierSystem",
                        "FHIR store search failed, serving local questionnaires only: " + e.getMessage());
            }
        }
        return new ArrayList<>(byId.values());
    }

    @Override
    @Transactional
    public QuestionnaireResponse saveQuestionnaireResponse(QuestionnaireResponse response) {
        storeQuestionnaireResponseLocally(response);
        pushToFhirStore(response);
        return response;
    }

    @Override
    @Transactional
    public QuestionnaireResponse storeQuestionnaireResponseLocally(QuestionnaireResponse response) {
        String id = requireId(response);
        org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse entity = findLocalResponse(id)
                .orElseGet(org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse::new);
        entity.setFhirUuid(toStorageUuid(id));
        entity.setStatus(toLocalStatus(response.getStatus()));
        entity.setSubjectReference(
                response.hasSubject() ? StringUtils.left(response.getSubject().getReference(), REFERENCE_LENGTH)
                        : null);
        entity.setAuthored(response.hasAuthored() ? new Timestamp(response.getAuthored().getTime()) : null);
        UUID questionnaireUuid = questionnaireUuidOf(response);
        entity.setQuestionnaireFhirUuid(questionnaireUuid);
        entity.setQuestionnaire(questionnaireUuid == null ? null
                : questionnaireService.getMatch("fhirUuid", questionnaireUuid).orElse(null));
        entity.setResourceJson(parser.encodeResourceToString(response));
        if (entity.getId() == null) {
            questionnaireResponseService.insert(entity);
        } else {
            questionnaireResponseService.update(entity);
        }
        return response;
    }

    @Override
    public Optional<QuestionnaireResponse> getQuestionnaireResponse(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return Optional.empty();
        }
        Optional<QuestionnaireResponse> remote = readFromFhirStore(QuestionnaireResponse.class, uuid);
        if (remote.isPresent()) {
            backfillLocally(uuid, findLocalResponse(uuid).isEmpty(),
                    () -> storeQuestionnaireResponseLocally(remote.get()));
            return remote;
        }
        return findLocalResponse(uuid).map(this::toFhir);
    }

    @Override
    public Optional<QuestionnaireResponse> getQuestionnaireResponse(UUID uuid) {
        return uuid == null ? Optional.empty() : getQuestionnaireResponse(uuid.toString());
    }

    private void storeQuestionnaireLocally(Questionnaire questionnaire) {
        String id = requireId(questionnaire);
        org.openelisglobal.questionnaire.valueholder.Questionnaire entity = findLocalQuestionnaire(id)
                .orElseGet(org.openelisglobal.questionnaire.valueholder.Questionnaire::new);
        entity.setFhirUuid(toStorageUuid(id));
        entity.setQuestionnaireName(StringUtils
                .left(StringUtils.firstNonBlank(questionnaire.getName(), questionnaire.getTitle(), id), TEXT_LENGTH));
        entity.setDescription(StringUtils.left(questionnaire.getDescription(), TEXT_LENGTH));
        entity.setPurpose(StringUtils.left(questionnaire.getPurpose(), TEXT_LENGTH));
        entity.setStatus(toLocalStatus(questionnaire.getStatus()));
        entity.setCode(
                questionnaire.hasCode() ? StringUtils.left(questionnaire.getCodeFirstRep().getCode(), CODE_LENGTH)
                        : null);
        entity.setHasItem(questionnaire.hasItem());
        entity.setIdentifierSystem(questionnaire.hasIdentifier()
                ? StringUtils.left(questionnaire.getIdentifierFirstRep().getSystem(), TEXT_LENGTH)
                : null);
        entity.setIdentifierValue(questionnaire.hasIdentifier()
                ? StringUtils.left(questionnaire.getIdentifierFirstRep().getValue(), TEXT_LENGTH)
                : null);
        entity.setResourceJson(parser.encodeResourceToString(questionnaire));
        if (entity.getId() == null) {
            questionnaireService.insert(entity);
        } else {
            questionnaireService.update(entity);
        }
    }

    private Optional<org.openelisglobal.questionnaire.valueholder.Questionnaire> findLocalQuestionnaire(String id) {
        return questionnaireService.getMatch("fhirUuid", toStorageUuid(id));
    }

    private Optional<org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse> findLocalResponse(String id) {
        return questionnaireResponseService.getMatch("fhirUuid", toStorageUuid(id));
    }

    private Questionnaire toFhir(org.openelisglobal.questionnaire.valueholder.Questionnaire entity) {
        if (StringUtils.isNotBlank(entity.getResourceJson())) {
            return parser.parseResource(Questionnaire.class, entity.getResourceJson());
        }
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(entity.getFhirUuid().toString());
        questionnaire.setName(entity.getQuestionnaireName());
        questionnaire.setTitle(entity.getQuestionnaireName());
        questionnaire.setDescription(entity.getDescription());
        questionnaire.setPurpose(entity.getPurpose());
        questionnaire.setStatus(toFhirStatus(entity.getStatus()));
        return questionnaire;
    }

    private QuestionnaireResponse toFhir(org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse entity) {
        if (StringUtils.isNotBlank(entity.getResourceJson())) {
            return parser.parseResource(QuestionnaireResponse.class, entity.getResourceJson());
        }
        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setId(entity.getFhirUuid().toString());
        response.setStatus(toFhirStatus(entity.getStatus()));
        if (entity.getQuestionnaireFhirUuid() != null) {
            response.setQuestionnaire("Questionnaire/" + entity.getQuestionnaireFhirUuid());
        }
        return response;
    }

    private <T extends IBaseResource> Optional<T> readFromFhirStore(Class<T> type, String id) {
        if (!isFhirStoreConfigured()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(fhirUtil.getLocalFhirClient().read().resource(type).withId(id).execute());
        } catch (RuntimeException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "readFromFhirStore", type.getSimpleName() + "/" + id
                    + " not read from the FHIR store, using the local database: " + e.getMessage());
            return Optional.empty();
        }
    }

    private List<Questionnaire> searchFhirStoreByIdentifierSystem(String identifierSystem) {
        List<Questionnaire> found = new ArrayList<>();
        IGenericClient client = fhirUtil.getLocalFhirClient();
        Bundle bundle = client.search().forResource(Questionnaire.class)
                .where(Questionnaire.IDENTIFIER.hasSystemWithAnyCode(identifierSystem))
                .where(Questionnaire.STATUS.exactly().code("active")).returnBundle(Bundle.class).execute();
        while (bundle != null) {
            for (BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Questionnaire) {
                    found.add((Questionnaire) entry.getResource());
                }
            }
            bundle = bundle.getLink(IBaseBundle.LINK_NEXT) != null ? client.loadPage().next(bundle).execute() : null;
        }
        return found;
    }

    private void pushToFhirStore(Resource resource) {
        if (!isFhirStoreConfigured()) {
            return;
        }
        try {
            fhirPersistanceService.updateFhirResourceInFhirStore(resource);
        } catch (FhirLocalPersistingException | RuntimeException e) {
            LogEvent.logError(getClass().getSimpleName(), "pushToFhirStore", resource.fhirType() + "/" + idOf(resource)
                    + " saved locally but not in the FHIR store: " + e.getMessage());
        }
    }

    /**
     * Copies a resource that only exists in the FHIR store into the local database.
     * Skipped inside a caller's transaction so a failed copy can never poison the
     * caller's work; the plain controller reads are where it runs.
     */
    private void backfillLocally(String id, boolean missingLocally, Runnable store) {
        if (!missingLocally || TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        try {
            store.run();
        } catch (RuntimeException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "backfillLocally",
                    id + " read from the FHIR store but not copied locally: " + e.getMessage());
        }
    }

    private static String requireId(Resource resource) {
        String id = idOf(resource);
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException(resource.fhirType() + " must carry an id before it can be stored");
        }
        return id;
    }

    private static String idOf(Resource resource) {
        return resource.getIdElement().getIdPart();
    }

    /**
     * FHIR ids are normally UUIDs; anything else maps to a stable name-based UUID.
     */
    static UUID toStorageUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(id.getBytes());
        }
    }

    private static UUID questionnaireUuidOf(QuestionnaireResponse response) {
        if (!response.hasQuestionnaire()) {
            return null;
        }
        String reference = response.getQuestionnaire();
        String tail = reference.substring(reference.lastIndexOf('/') + 1);
        try {
            return UUID.fromString(tail);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static QuestionnaireStatus toLocalStatus(PublicationStatus status) {
        if (status == null) {
            return QuestionnaireStatus.UNKNOWN;
        }
        switch (status) {
        case DRAFT:
            return QuestionnaireStatus.DRAFT;
        case ACTIVE:
            return QuestionnaireStatus.ACTIVE;
        case RETIRED:
            return QuestionnaireStatus.RETIRED;
        default:
            return QuestionnaireStatus.UNKNOWN;
        }
    }

    private static PublicationStatus toFhirStatus(QuestionnaireStatus status) {
        if (status == null) {
            return PublicationStatus.UNKNOWN;
        }
        switch (status) {
        case DRAFT:
            return PublicationStatus.DRAFT;
        case ACTIVE:
            return PublicationStatus.ACTIVE;
        case RETIRED:
            return PublicationStatus.RETIRED;
        default:
            return PublicationStatus.UNKNOWN;
        }
    }

    private static QuestionnaireResponseStatus toLocalStatus(QuestionnaireResponse.QuestionnaireResponseStatus status) {
        if (status == null || status == QuestionnaireResponse.QuestionnaireResponseStatus.NULL) {
            return null;
        }
        for (QuestionnaireResponseStatus candidate : QuestionnaireResponseStatus.values()) {
            if (candidate.getValue().equals(status.toCode())) {
                return candidate;
            }
        }
        return null;
    }

    private static QuestionnaireResponse.QuestionnaireResponseStatus toFhirStatus(QuestionnaireResponseStatus status) {
        return status == null ? null : QuestionnaireResponse.QuestionnaireResponseStatus.fromCode(status.getValue());
    }
}
