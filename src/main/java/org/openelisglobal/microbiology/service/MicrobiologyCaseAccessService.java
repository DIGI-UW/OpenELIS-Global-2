package org.openelisglobal.microbiology.service;

import java.util.Set;

public interface MicrobiologyCaseAccessService {
    boolean canAccessCase(String caseId, String systemUserId, boolean administrator);

    boolean canAccessSampleItem(String sampleItemId, String systemUserId, boolean administrator);

    Set<String> permittedLabUnitIds(String systemUserId, String roleName);

    boolean hasLabUnitRole(String systemUserId, String labUnitId, String roleName);

    boolean canReadLabUnit(String systemUserId, String labUnitId);

}
