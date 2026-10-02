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

/**
 * OGC-1363: the Locations and Organizations menu. Every record is an
 * Organization; the service derives its kind from its types, keeps the
 * reporting code, the sampling-site record and the display lists in step with
 * each change, and writes an entry to the record's change history.
 */
public interface LocationsService {

    Page list(Query query);

    Detail get(String id);

    SaveResult save(SaveRequest request, String sysUserId);

    /**
     * As {@link #save(SaveRequest, String)}, recording {@code actor} (an import
     * run) in the history.
     */
    SaveResult save(SaveRequest request, String sysUserId, String actor);

    UsageDetail usage(String id);

    ActiveResult setActive(ActiveRequest request, String sysUserId);

    ActiveResult setActive(ActiveRequest request, String sysUserId, String actor);

    List<Ward> wards(String organizationId, boolean includeInactive);

    Ward saveWard(String organizationId, WardRequest request, String sysUserId);

    Ward saveWard(String organizationId, WardRequest request, String sysUserId, String actor);

    Ward moveWard(String wardId, String newParentId, String sysUserId);

    List<HistoryEntry> history(String id);

    Lists lists();

    List<AreaLevel> areaLevels();

    List<Area> areaChildren(String parentId, String status);

    List<Area> searchAreas(String text, String status);

    Area saveArea(AreaRequest request, String sysUserId);

    Area setAreaActive(String id, boolean active, String sysUserId);
}
