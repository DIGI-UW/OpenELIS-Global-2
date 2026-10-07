package org.openelisglobal.orderentry.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.orderentry.dao.PossibleMatchCandidateDAO;
import org.openelisglobal.orderentry.dao.PossibleMatchOverrideDAO;
import org.openelisglobal.orderentry.service.PossibleMatchScorer.Scored;
import org.openelisglobal.orderentry.valueholder.PossibleMatchOverride;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PossibleMatchServiceImpl implements PossibleMatchService {

    static final int CANDIDATE_LIMIT = 200;
    private static final Set<String> KINDS = Set.of(PATIENT, FACILITY, PROVIDER);

    @Autowired
    private PossibleMatchCandidateDAO candidateDAO;

    @Autowired
    private PossibleMatchOverrideDAO overrideDAO;

    @Override
    @Transactional(readOnly = true)
    public List<Scored> patients(String firstName, String lastName, LocalDate birthDate, String identifier) {
        return PossibleMatchScorer.patients(
                candidateDAO.patientCandidates(firstName, lastName, birthDate, identifier, CANDIDATE_LIMIT), birthDate,
                identifier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scored> facilities(String name, String code) {
        if (GenericValidator.isBlankOrNull(name) && GenericValidator.isBlankOrNull(code)) {
            return List.of();
        }
        return PossibleMatchScorer.facilities(candidateDAO.facilityCandidates(), name, code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scored> providers(String firstName, String lastName) {
        return PossibleMatchScorer.providers(candidateDAO.providerCandidates(firstName, lastName, CANDIDATE_LIMIT));
    }

    @Override
    @Transactional
    public Integer recordOverride(String kind, String enteredJson, String matchesShownJson, String sysUserId) {
        String normalizedKind = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
        if (!KINDS.contains(normalizedKind)) {
            throw new OrderEntryRequestRefusedException("unknown record kind: " + kind);
        }
        PossibleMatchOverride override = new PossibleMatchOverride();
        override.setRecordKind(normalizedKind);
        override.setEnteredValues(enteredJson);
        override.setMatchesShown(matchesShownJson);
        override.setRecordedById(GenericValidator.isInt(sysUserId) ? Integer.valueOf(sysUserId) : null);
        override.setSysUserId(sysUserId);
        return overrideDAO.insert(override);
    }
}
