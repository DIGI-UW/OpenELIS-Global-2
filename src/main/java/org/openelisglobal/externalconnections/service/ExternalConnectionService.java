package org.openelisglobal.externalconnections.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ExternalConnectionService extends BaseObjectService<ExternalConnection, Integer> {

    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_MANAGE')")
    void createNewExternalConnection(Map<AuthType, ExternalConnectionAuthenticationData> externalConnectionAuthData,
            List<ExternalConnectionContact> externalConnectionContacts, ExternalConnection externalConnection);

    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_MANAGE')")
    void updateExternalConnection(Map<AuthType, ExternalConnectionAuthenticationData> externalConnectionAuthData,
            List<ExternalConnectionContact> externalConnectionContacts, ExternalConnection externalConnection);

    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_MANAGE')")
    void updateExternalConnectionFields(Integer id, String sysUserId, Boolean active,
            ProgrammedConnection programmedConnection, AuthType authType, URI uri, String nameValue,
            String descriptionValue, String basicUsername, String basicPassword);

    /**
     * True iff at least one {@code external_connection} row exists for the given
     * programmed connection type with {@code active = true}. Used as the
     * channel-availability gate by the notification trigger system.
     */
    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_VIEW')")
    boolean isActive(ProgrammedConnection programmedConnection);
}
