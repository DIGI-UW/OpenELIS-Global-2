package org.openelisglobal.organization.locations;

import java.util.List;
import org.openelisglobal.organization.locations.LocationsApi.ActiveRequest;
import org.openelisglobal.organization.locations.LocationsApi.ActiveResult;
import org.openelisglobal.organization.locations.LocationsApi.Area;
import org.openelisglobal.organization.locations.LocationsApi.AreaLevel;
import org.openelisglobal.organization.locations.LocationsApi.AreaRequest;
import org.openelisglobal.organization.locations.LocationsApi.Detail;
import org.openelisglobal.organization.locations.LocationsApi.HistoryEntry;
import org.openelisglobal.organization.locations.LocationsApi.Lists;
import org.openelisglobal.organization.locations.LocationsApi.Page;
import org.openelisglobal.organization.locations.LocationsApi.Query;
import org.openelisglobal.organization.locations.LocationsApi.SaveRequest;
import org.openelisglobal.organization.locations.LocationsApi.SaveResult;
import org.openelisglobal.organization.locations.LocationsApi.UsageDetail;
import org.openelisglobal.organization.locations.LocationsApi.Ward;
import org.openelisglobal.organization.locations.LocationsApi.WardRequest;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * OGC-1363: the Locations and Organizations menu. Every record is an
 * Organization; the service derives its kind from its types, keeps the
 * reporting code, the sampling-site record and the display lists in step with
 * each change, and writes an entry to the record's change history.
 */
public interface LocationsService {

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Page list(Query query);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Detail get(String id);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    SaveResult save(SaveRequest request, String sysUserId);

    /**
     * As {@link #save(SaveRequest, String)}, recording {@code actor} (an import
     * run) in the history.
     */
    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    SaveResult save(SaveRequest request, String sysUserId, String actor);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    UsageDetail usage(String id);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    ActiveResult setActive(ActiveRequest request, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    ActiveResult setActive(ActiveRequest request, String sysUserId, String actor);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Ward> wards(String organizationId, boolean includeInactive);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    Ward saveWard(String organizationId, WardRequest request, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    Ward saveWard(String organizationId, WardRequest request, String sysUserId, String actor);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    Ward moveWard(String wardId, String newParentId, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<HistoryEntry> history(String id);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Lists lists();

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<AreaLevel> areaLevels();

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Area> areaChildren(String parentId, String status);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Area> searchAreas(String text, String status);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    Area saveArea(AreaRequest request, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    Area setAreaActive(String id, boolean active, String sysUserId);
}
