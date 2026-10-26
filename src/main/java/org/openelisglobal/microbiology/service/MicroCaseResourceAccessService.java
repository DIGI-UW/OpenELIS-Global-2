package org.openelisglobal.microbiology.service;

public interface MicroCaseResourceAccessService {
    String resolveCaseId(String kind, String id);

    void requireAccess(String caseId, String actor, String role, boolean read);
}
