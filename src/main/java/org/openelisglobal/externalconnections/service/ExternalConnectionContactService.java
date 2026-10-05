package org.openelisglobal.externalconnections.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_VIEW')")
public interface ExternalConnectionContactService extends BaseObjectService<ExternalConnectionContact, Integer> {
}
