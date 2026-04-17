package org.openelisglobal.action.service;

import org.openelisglobal.action.valueholder.Action;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;

@CrossDomainService(callers = "action logging — called across all write operations, cross-domain audit")
public interface ActionService extends BaseObjectService<Action, String> {
}
