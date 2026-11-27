package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.program.dao.PathologyConsultationDAO;
import org.openelisglobal.program.util.DictionaryEntryNames;
import org.openelisglobal.program.util.EnumNames;
import org.openelisglobal.program.valueholder.pathology.ConsultationOutcome;
import org.openelisglobal.program.valueholder.pathology.ConsultationStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologyConsultation;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes second opinions through their own DAO and leaves the case alone: no
 * lock on it, no change to it, no case history row. The three events are the
 * history of a second opinion; the auditable base service would write a row of
 * its own beside them.
 */
@Service
public class PathologyConsultationServiceImpl implements PathologyConsultationService {

    private final PathologyConsultationDAO pathologyConsultationDAO;
    private final PathologySampleService pathologySampleService;
    private final SystemUserService systemUserService;
    private final UserRoleService userRoleService;
    private final DictionaryService dictionaryService;

    public PathologyConsultationServiceImpl(PathologyConsultationDAO pathologyConsultationDAO,
            PathologySampleService pathologySampleService, SystemUserService systemUserService,
            UserRoleService userRoleService, DictionaryService dictionaryService) {
        this.pathologyConsultationDAO = pathologyConsultationDAO;
        this.pathologySampleService = pathologySampleService;
        this.systemUserService = systemUserService;
        this.userRoleService = userRoleService;
        this.dictionaryService = dictionaryService;
    }

    @Override
    @Transactional
    public PathologyConsultation request(Integer pathologySampleId, ConsultationDraft draft, String sysUserId) {
        String givenId = draft == null ? null : StringUtils.trimToNull(draft.consultedUserId());
        if (givenId == null) {
            throw refusal("consultationColleagueRequired", "a second opinion names the pathologist asked");
        }
        String colleagueId = isIntId(givenId) ? String.valueOf(Integer.parseInt(givenId)) : givenId;
        if (draft.preTreatment() == null) {
            throw refusal("consultationPreTreatmentRequired",
                    "a second opinion says whether it is asked for before treatment");
        }
        PathologySample pathologySample = pathologySampleService.get(pathologySampleId);
        if (pathologySampleService.isSignedOut(pathologySample)) {
            throw new LocalizedValidationException("pathology.error.caseCompleted",
                    "the case is signed out, so no second opinion can be asked for");
        }
        if (!takesConsultations(pathologySample)) {
            throw refusal("consultationOutsideReview",
                    "a second opinion is asked for once the case has reached the pathologist");
        }
        if (colleagueId.equals(sysUserId)) {
            throw refusal("consultationSelf", "a pathologist cannot ask themselves for a second opinion");
        }
        SystemUser colleague = availablePathologist(colleagueId);
        for (PathologyConsultation consultation : pathologyConsultationDAO.getForCase(pathologySampleId)) {
            if (consultation.getStatus() == ConsultationStatus.REQUESTED
                    && colleagueId.equals(consultation.getConsultedUser().getId())) {
                throw new LocalizedValidationException("pathology.error.consultationAlreadyOpen",
                        "user " + colleagueId + " is already awaited on this case",
                        Map.of("name", colleague.getDisplayName()));
            }
        }
        String reasonId = StringUtils.trimToNull(draft.reasonId());
        if (reasonId != null) {
            refuseUnlessInUse(reasonId);
        }

        PathologyConsultation consultation = new PathologyConsultation();
        consultation.setPathologySampleId(pathologySample.getId());
        consultation.setRequestedBy(systemUserService.getUserById(sysUserId));
        consultation.setConsultedUser(colleague);
        consultation.setReasonDictionaryId(reasonId);
        consultation.setPreTreatment(draft.preTreatment());
        consultation.setStatus(ConsultationStatus.REQUESTED);
        consultation.setRequestedAt(now());
        consultation.setSysUserId(sysUserId);
        pathologyConsultationDAO.insert(consultation);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_CONSULTATION_REQUESTED);
        payload.put("consultedUserId", colleagueId);
        payload.put("preTreatment", consultation.isPreTreatment());
        pathologySampleService.recordRowEvent(REFERENCE_TABLE, consultation.getStringId(),
                IActionConstants.AUDIT_TRAIL_INSERT, payload, sysUserId);
        return consultation;
    }

    @Override
    @Transactional
    public PathologyConsultation answer(Integer pathologySampleId, Integer consultationId, ConsultationAnswer answer,
            String sysUserId) {
        ConsultationOutcome outcome = answer == null ? null
                : EnumNames.valueNamed(ConsultationOutcome.class, answer.outcome());
        if (outcome == null) {
            throw refusal("consultationOutcomeRequired", "an answer names AGREE, DISAGREE or MODIFIED");
        }
        String note = StringUtils.trimToNull(answer.note());
        if (note != null && note.length() > PathologyConsultation.NOTE_MAX_LENGTH) {
            throw refusal("consultationNoteTooLong",
                    "the note is longer than " + PathologyConsultation.NOTE_MAX_LENGTH + " characters");
        }
        pathologySampleService.get(pathologySampleId);
        PathologyConsultation consultation = awaitedOnCase(pathologySampleId, consultationId);
        SystemUser consulted = consultation.getConsultedUser();
        if (!Objects.equals(consulted.getId(), sysUserId)) {
            throw new LocalizedValidationException("pathology.error.consultationNotYours",
                    "only the pathologist asked records the outcome of second opinion " + consultationId,
                    Map.of("name", consulted.getDisplayName()));
        }

        consultation.setStatus(ConsultationStatus.ANSWERED);
        consultation.setOutcome(outcome);
        consultation.setOutcomeNote(note);
        consultation.setAnsweredAt(now());
        consultation.setSysUserId(sysUserId);
        PathologyConsultation stored = pathologyConsultationDAO.update(consultation);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_CONSULTATION_ANSWERED);
        payload.put("outcome", outcome.name());
        pathologySampleService.recordRowEvent(REFERENCE_TABLE, stored.getStringId(),
                IActionConstants.AUDIT_TRAIL_UPDATE, payload, sysUserId);
        return stored;
    }

    @Override
    @Transactional
    public PathologyConsultation withdraw(Integer pathologySampleId, Integer consultationId, String sysUserId) {
        pathologySampleService.get(pathologySampleId);
        PathologyConsultation consultation = awaitedOnCase(pathologySampleId, consultationId);

        consultation.setStatus(ConsultationStatus.WITHDRAWN);
        consultation.setSysUserId(sysUserId);
        PathologyConsultation stored = pathologyConsultationDAO.update(consultation);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_CONSULTATION_WITHDRAWN);
        pathologySampleService.recordRowEvent(REFERENCE_TABLE, stored.getStringId(),
                IActionConstants.AUDIT_TRAIL_UPDATE, payload, sysUserId);
        return stored;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologyConsultation> getForCase(Integer pathologySampleId) {
        return pathologyConsultationDAO.getForCase(pathologySampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean takesConsultations(PathologySample pathologySample) {
        return pathologySampleService.isReleasable(pathologySample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> suggestedFor(PathologySample pathologySample, List<PathologyConsultation> consultations) {
        if (!takesConsultations(pathologySample)
                || consultations.stream().anyMatch(row -> row.getStatus() == ConsultationStatus.REQUESTED
                        || row.getStatus() == ConsultationStatus.ANSWERED)) {
            return List.of();
        }
        List<String> prefixes = Stream
                .of(StringUtils.defaultString(ConfigurationProperties.getInstance()
                        .getPropertyValue(Property.PATHOLOGY_SECOND_OPINION_SUGGEST_CODES)).split(","))
                .map(String::trim).filter(StringUtils::isNotEmpty).map(prefix -> prefix.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
        if (prefixes.isEmpty()) {
            return List.of();
        }
        return pathologySample.getConclusions().stream().filter(row -> row.getType() == ConclusionType.DICTIONARY)
                .sorted(Comparator.comparing(PathologyConclusion::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .flatMap(row -> DictionaryEntryNames.conclusionEntryOf(dictionaryService, row.getValue()).stream())
                .filter(entry -> StringUtils.isNotBlank(entry.code()))
                .filter(entry -> prefixes.stream()
                        .anyMatch(prefix -> entry.code().toUpperCase(Locale.ROOT).startsWith(prefix)))
                .map(entry -> entry.code() + " " + entry.value()).collect(Collectors.toList());
    }

    /**
     * The active pathologist the id names. The id is checked to be an int first:
     * the role lookup parses it outside its own error handling, and the user lookup
     * by id answers null for none where the plain get would throw.
     */
    private SystemUser availablePathologist(String colleagueId) {
        SystemUser colleague = isIntId(colleagueId) ? systemUserService.getUserById(colleagueId) : null;
        if (colleague == null || !"Y".equals(colleague.getIsActive())
                || !userRoleService.userInRole(colleagueId, Constants.ROLE_PATHOLOGIST)) {
            throw refusal("consultationColleagueNotAvailable",
                    "user " + colleagueId + " is no active pathologist, so cannot be asked");
        }
        return colleague;
    }

    /**
     * The second opinion of that id on this case, still awaited. One of another
     * case, or an id that names none, is not on this case; one already answered or
     * withdrawn is a page out of date.
     */
    private PathologyConsultation awaitedOnCase(Integer pathologySampleId, Integer consultationId) {
        Optional<PathologyConsultation> stored = consultationId == null ? Optional.empty()
                : pathologyConsultationDAO.get(consultationId);
        PathologyConsultation consultation = stored
                .filter(found -> Objects.equals(found.getPathologySampleId(), pathologySampleId))
                .orElseThrow(() -> refusal("consultationNotOnCase",
                        "second opinion " + consultationId + " is not on case " + pathologySampleId));
        if (consultation.getStatus() != ConsultationStatus.REQUESTED) {
            throw new PathologyConsultationConflictException(consultationId, consultation.getStatus());
        }
        return consultation;
    }

    /**
     * Refuses an id that names no reason with the reload refusal, and a reason
     * taken out of use by its name. The same rule as the case's own dictionary
     * picks, written against the reasons category.
     */
    private void refuseUnlessInUse(String reasonId) {
        Dictionary entry = reasonOf(reasonId);
        if (entry == null) {
            throw new PathologyCaseRuleException(
                    "dictionary entry " + reasonId + " is not an entry of " + REASON_CATEGORY);
        }
        if (!"Y".equals(entry.getIsActive())) {
            throw new LocalizedValidationException("pathology.error.consultationReasonNotInUse",
                    "dictionary entry " + reasonId + " of " + REASON_CATEGORY + " is no longer in use",
                    Map.of("name", DictionaryEntryNames.localizedNameOf(dictionaryService, reasonId).orElse(reasonId)));
        }
    }

    /** The reason the id names, in use or not; null if none. */
    private Dictionary reasonOf(String reasonId) {
        if (!isIntId(reasonId)) {
            return null;
        }
        Dictionary entry = dictionaryService.getDataForId(reasonId);
        return entry != null && entry.getDictionaryCategory() != null
                && REASON_CATEGORY.equals(entry.getDictionaryCategory().getCategoryName()) ? entry : null;
    }

    /**
     * Whether the id is ASCII digits within the int range. A sign, a letter, any
     * other digit or digits past the range name no row and the lookups would throw
     * on them. Leading zeros pass, so a request turns the colleague's id into its
     * plain form before comparing it with the signed-in user's id, the colleagues
     * already awaited, or storing it.
     */
    private static boolean isIntId(String id) {
        if (StringUtils.isEmpty(id) || !id.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return false;
        }
        try {
            Integer.parseInt(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static LocalizedValidationException refusal(String key, String message) {
        return new LocalizedValidationException("pathology.error." + key, message);
    }

    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
