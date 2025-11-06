package org.openelisglobal.systemusermodule.dao;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.systemusermodule.valueholder.SystemUserModule;

public interface SystemUserModuleDAO extends PermissionModuleDAO<SystemUserModule> {

    boolean duplicateSystemUserModuleExists(SystemUserModule systemUserModule) throws LIMSRuntimeException;
}
