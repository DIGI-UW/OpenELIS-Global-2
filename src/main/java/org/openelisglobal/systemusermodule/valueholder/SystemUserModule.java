/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.systemusermodule.valueholder;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us) bugzilla 2203 fix to
 *         LazyInitializer error 11/07/2007
 */
@Entity
@Table(name = "system_user_module", schema = "clinlims")
@SequenceGenerator(name = "system_user_module_seq_gen", sequenceName = "system_user_module_seq", schema = "clinlims", allocationSize = 1)
public class SystemUserModule extends PermissionModule {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_user_id")
    private SystemUser systemUser;

    @Transient
    private String systemUserId;

    public SystemUserModule() {
        super();
    }

    public void setSystemUser(SystemUser systemUser) {
        this.systemUser = systemUser;
    }

    public SystemUser getSystemUser() {
        return systemUser;
    }

    public void setSystemUserId(String systemUserId) {
        this.systemUserId = systemUserId;
    }

    public String getSystemUserId() {
        if (systemUserId != null) {
            return systemUserId;
        }
        return systemUser != null ? systemUser.getId() : null;
    }

    @Override
    public String getPermissionAgentId() {
        return getSystemUserId();
    }

    @Override
    public PermissionAgent getPermissionAgent() {
        return getSystemUser();
    }

    @Override
    public void setPermissionAgent(PermissionAgent agent) {
        setSystemUser((SystemUser) agent);
    }
}
