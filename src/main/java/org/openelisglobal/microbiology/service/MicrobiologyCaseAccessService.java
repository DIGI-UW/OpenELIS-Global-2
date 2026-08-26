package org.openelisglobal.microbiology.service;

public interface MicrobiologyCaseAccessService {

    boolean canAccessCase(String caseId, String systemUserId, boolean administrator);

    boolean canAccessSampleItem(String sampleItemId, String systemUserId, boolean administrator);
}
