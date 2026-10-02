package org.openelisglobal.organization.locations;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.organization.service.OrganizationChangeService.FieldChange;

/**
 * OGC-1363: the shapes the Locations and Organizations menu exchanges with its
 * REST endpoints. Every record in the menu is an Organization; its kind
 * (facility, ward, site, area) is derived from its organization types.
 */
public final class LocationsApi {

    private LocationsApi() {
    }

    public static final String KIND_FACILITY = "facility";
    public static final String KIND_WARD = "ward";
    public static final String KIND_SITE = "site";
    public static final String KIND_AREA = "area";

    public static final String VIEW_ORGANIZATIONS = "organizations";
    public static final String VIEW_SITES = "sites";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_ALL = "all";

    public static final String SOURCE_REGISTRY = "REGISTRY";
    public static final String SOURCE_LOCAL = "LOCAL";
    public static final String SOURCE_IMPORT = "IMPORT";

    public static final List<String> REFERRAL_STATUSES = List.of("Approved", "Under evaluation", "Suspended",
            "Not approved");

    public record TypeRef(String id, String name, Integer hierarchyLevel) {
    }

    public record ListRef(String id, String label) {
    }

    /**
     * The area a record is placed in: the most specific area, its level, and the
     * full path top-down.
     */
    public record LocationRef(String id, String name, String levelName, List<String> path) {
    }

    public record Usage(int open, int total) {
    }

    public record Identifier(Integer id, String label, String value, boolean reporting) {
    }

    public record Row(String id, String name, String shortName, String code, String kind, List<TypeRef> types,
            ListRef category, ListRef ownership, LocationRef location, int wardCount, String siteType, Usage inUse,
            boolean active, boolean registry, boolean reviewOverdue, boolean accreditationExpired, String matchNote) {
    }

    public record Page(List<Row> items, long total, int page, int pageSize) {
    }

    public record Referral(String approvalStatus, String accreditationBody, String accreditationNumber,
            String accreditationExpiry, String lastReviewDate, String nextReviewDue, String reviewNotes) {
    }

    public record Site(Integer siteId, String siteType, String subtype, String environmentalZone) {
    }

    public record Ward(String id, String name, String code, String serviceType, String contactName, String phone,
            String email, BigDecimal gpsLatitude, BigDecimal gpsLongitude, boolean gpsInherited, Usage inUse,
            boolean active, Long lastupdated) {
    }

    public record Detail(Row row, String parentId, String streetAddress, String city, String state, String zipCode,
            String phone, String fax, String email, String internetAddress, String contactName, String description,
            BigDecimal gpsLatitude, BigDecimal gpsLongitude, String serviceType, String source,
            List<Identifier> identifiers, List<Ward> wards, Referral referral, Site site, Long lastupdated,
            int historyCount) {
    }

    public record SaveRequest(String id, String kind, String name, String shortName, List<String> typeIds,
            String categoryId, String ownershipId, String description, String parentId, String streetAddress,
            String city, String state, String zipCode, BigDecimal gpsLatitude, BigDecimal gpsLongitude,
            String contactName, String phone, String fax, String email, String internetAddress,
            List<Identifier> identifiers, Referral referral, Site site, String serviceType, Long lastupdated) {
    }

    public record SaveResult(Detail detail, List<String> warnings) {
    }

    public record WardRequest(String id, String name, String code, String serviceType, String contactName, String phone,
            String email, BigDecimal gpsLatitude, BigDecimal gpsLongitude, Long lastupdated) {
    }

    public record ActiveRequest(List<String> ids, boolean active, boolean includeChildren) {
    }

    public record ActiveResult(List<String> changed, List<String> names) {
    }

    public record ChildUsage(String id, String name, String kind, Usage inUse) {
    }

    public record UsageDetail(String id, String name, String kind, Usage inUse, List<ChildUsage> activeChildren) {
    }

    public record AreaLevel(Integer level, String name, String typeId) {
    }

    public record Area(String id, String name, String code, Integer level, String levelName, String parentId,
            boolean active, int childCount, boolean matched, Long lastupdated) {
    }

    public record AreaRequest(String id, String name, String code, String parentId, Long lastupdated) {
    }

    public record HistoryEntry(Integer id, String when, String user, String action, List<FieldChange> changes) {
    }

    public record Lists(List<ListRef> categories, List<ListRef> ownerships, List<ListRef> serviceTypes,
            List<ListRef> siteTypes, List<ListRef> environmentalZones, List<ListRef> referralStatuses,
            List<String> identifierLabels, List<TypeRef> facilityTypes, List<AreaLevel> areaLevels) {
    }

    public record Query(String view, String q, List<String> typeIds, String locationId, List<String> categoryIds,
            List<String> ownershipIds, String status, boolean reviewOverdue, String sort, int page, int pageSize) {
    }
}
