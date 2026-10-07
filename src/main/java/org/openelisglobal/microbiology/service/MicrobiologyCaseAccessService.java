package org.openelisglobal.microbiology.service;

public interface MicrobiologyCaseAccessService {
    MicrobiologyWorklistAccess getWorklistAccess(String systemUserId);

    boolean canReadCase(String caseId, String systemUserId);

    boolean canViewOnWorklist(String caseId, String systemUserId);

    boolean canEnterResults(String caseId, String systemUserId);

    boolean canValidateResults(String caseId, String systemUserId);

    boolean canEnterResultsInUnit(String testSectionId, String systemUserId);

    void requireResults(String caseId, String systemUserId);

    void requireValidation(String caseId, String systemUserId);
}
