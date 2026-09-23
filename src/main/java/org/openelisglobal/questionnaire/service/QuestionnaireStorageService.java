package org.openelisglobal.questionnaire.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Single entry point for reading and writing FHIR {@link Questionnaire} and
 * {@link QuestionnaireResponse} resources used by programme forms.
 *
 * <p>
 * The OpenELIS database is the system of record: every write lands there first.
 * When a FHIR store is configured ({@code org.openelisglobal.fhirstore.uri})
 * the resource is also pushed to it, and reads consult the store first and fall
 * back to the local copy. Without a FHIR store everything works from the local
 * database alone.
 */
public interface QuestionnaireStorageService {

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    boolean isFhirStoreConfigured();

    /** Stores the questionnaire locally and, when configured, in the FHIR store. */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Questionnaire saveQuestionnaire(Questionnaire questionnaire);

    /** FHIR store first (when configured), then the local database. */
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    Optional<Questionnaire> getQuestionnaire(String uuid);

    /** Null-safe variant of {@link #getQuestionnaire(String)}. */
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    Optional<Questionnaire> getQuestionnaire(UUID uuid);

    /**
     * Active questionnaires carrying an identifier with the given system, merged
     * from the local database and, when configured, the FHIR store.
     */
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<Questionnaire> getActiveQuestionnairesByIdentifierSystem(String identifierSystem);

    /** Stores the response locally and, when configured, in the FHIR store. */
    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    QuestionnaireResponse saveQuestionnaireResponse(QuestionnaireResponse response);

    /**
     * Stores the response in the local database only. For callers whose FHIR write
     * already happens elsewhere (the asynchronous order-entry transform).
     */
    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    QuestionnaireResponse storeQuestionnaireResponseLocally(QuestionnaireResponse response);

    /** FHIR store first (when configured), then the local database. */
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    Optional<QuestionnaireResponse> getQuestionnaireResponse(String uuid);

    /** Null-safe variant of {@link #getQuestionnaireResponse(String)}. */
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    Optional<QuestionnaireResponse> getQuestionnaireResponse(UUID uuid);
}
