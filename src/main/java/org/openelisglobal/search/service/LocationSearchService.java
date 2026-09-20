package org.openelisglobal.search.service;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.model.valueset.BundleEntrySearchModeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.openelisglobal.common.fhir.dao.DateParamBounds;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.bundleProviders.PagedBundleProvider;
import org.openelisglobal.fhir.search.searchparams.LocationSearchParams;
import org.openelisglobal.storage.fhir.StorageLocationFhirTransform;
import org.openelisglobal.storage.service.StorageBoxService;
import org.openelisglobal.storage.service.StorageDeviceService;
import org.openelisglobal.storage.service.StorageRackService;
import org.openelisglobal.storage.service.StorageRoomService;
import org.openelisglobal.storage.service.StorageShelfService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local Location search over the storage hierarchy.
 *
 * <p>
 * The five storage levels live in separate tables and the whole hierarchy is
 * small, so the search renders every level to a Location once and filters the
 * FHIR view in memory. That keeps the filter identical to what a client sees
 * (identifier codes, names, status, partOf, hierarchy tag) at the cost of
 * reading the tables on each request.
 */
@Service
@Transactional(readOnly = true)
public class LocationSearchService {

    private final StorageLocationFhirTransform transform;
    private final StorageRoomService roomService;
    private final StorageDeviceService deviceService;
    private final StorageShelfService shelfService;
    private final StorageRackService rackService;
    private final StorageBoxService boxService;

    public LocationSearchService(StorageLocationFhirTransform transform, StorageRoomService roomService,
            StorageDeviceService deviceService, StorageShelfService shelfService, StorageRackService rackService,
            StorageBoxService boxService) {
        this.transform = transform;
        this.roomService = roomService;
        this.deviceService = deviceService;
        this.shelfService = shelfService;
        this.rackService = rackService;
        this.boxService = boxService;
    }

    public IBundleProvider searchLocations(LocationSearchParams params) {

        Map<String, Location> all = renderHierarchy();
        List<Location> matches = all.values().stream().filter(matcher(params)).toList();

        List<IBaseResource> resources = new ArrayList<>();
        for (Location match : matches) {
            ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(match, BundleEntrySearchModeEnum.MATCH);
            resources.add(match);
        }

        Map<String, Location> included = new LinkedHashMap<>();
        if (params.hasInclude(FhirConstants.LOCATION_PARTOF_INCLUDE)) {
            for (Location match : matches) {
                String parentId = referencedId(match);
                if (parentId != null && all.containsKey(parentId)) {
                    included.putIfAbsent(parentId, all.get(parentId));
                }
            }
        }
        if (params.hasRevInclude(FhirConstants.LOCATION_PARTOF_INCLUDE)) {
            for (Location candidate : all.values()) {
                String parentId = referencedId(candidate);
                if (parentId != null
                        && matches.stream().anyMatch(match -> parentId.equals(match.getIdElement().getIdPart()))) {
                    included.putIfAbsent(candidate.getIdElement().getIdPart(), candidate);
                }
            }
        }
        matches.forEach(match -> included.remove(match.getIdElement().getIdPart()));
        List<IBaseResource> extras = new ArrayList<>();
        for (Location extra : included.values()) {
            ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(extra, BundleEntrySearchModeEnum.INCLUDE);
            extras.add(extra);
        }

        return new HierarchyBundleProvider(resources, extras);
    }

    /**
     * In-memory bundle provider whose size is the match count while every page
     * carries the included parents and children, mirroring how the database-backed
     * bundle providers append their includes.
     */
    static final class HierarchyBundleProvider implements PagedBundleProvider {

        private final List<IBaseResource> matches;
        private final List<IBaseResource> includes;
        private final InstantDt published = InstantDt.withCurrentTime();
        private Integer currentPageOffset;
        private Integer currentPageSize;

        HierarchyBundleProvider(List<IBaseResource> matches, List<IBaseResource> includes) {
            this.matches = matches;
            this.includes = includes;
        }

        @Override
        public void setCurrentPage(Integer offset, Integer count) {
            currentPageOffset = offset;
            currentPageSize = offset == null || count == null || count <= 0 ? null : count;
        }

        @Override
        public Integer getCurrentPageOffset() {
            return currentPageOffset;
        }

        @Override
        public Integer getCurrentPageSize() {
            return currentPageSize;
        }

        @Override
        public List<IBaseResource> getResources(int fromIndex, int toIndex) {
            int requestedFrom = currentPageOffset == null ? fromIndex : currentPageOffset;
            int requestedTo = currentPageSize == null ? toIndex : requestedFrom + currentPageSize;
            int from = Math.max(0, Math.min(requestedFrom, matches.size()));
            int to = Math.max(from, Math.min(requestedTo, matches.size()));
            List<IBaseResource> page = new ArrayList<>(matches.subList(from, to));
            page.addAll(includes);
            return page;
        }

        @Override
        public Integer size() {
            return matches.size();
        }

        @Override
        public IPrimitiveType<Date> getPublished() {
            return published;
        }

        @Override
        public String getUuid() {
            return null;
        }

        @Override
        public Integer preferredPageSize() {
            return null;
        }
    }

    private Map<String, Location> renderHierarchy() {
        Map<String, Location> all = new LinkedHashMap<>();
        roomService.getAll().forEach(room -> put(all, transform.transformToFhirLocation(room)));
        deviceService.getAll().forEach(device -> put(all, transform.transformToFhirLocation(device)));
        shelfService.getAll().forEach(shelf -> put(all, transform.transformToFhirLocation(shelf)));
        rackService.getAll().forEach(rack -> put(all, transform.transformToFhirLocation(rack)));
        boxService.getAll().forEach(box -> put(all, transform.transformToFhirLocation(box)));
        return all;
    }

    private static void put(Map<String, Location> all, Location location) {
        if (location != null && location.hasId()) {
            all.putIfAbsent(location.getIdElement().getIdPart(), location);
        }
    }

    private static String referencedId(Location location) {
        if (!location.hasPartOf() || !location.getPartOf().hasReference()) {
            return null;
        }
        return location.getPartOf().getReferenceElement().getIdPart();
    }

    private static Predicate<Location> matcher(LocationSearchParams params) {

        Predicate<Location> predicate = location -> true;

        if (params.getId() != null) {
            predicate = predicate.and(location -> tokenAndList(params.getId().getValuesAsQueryTokens(),
                    token -> equalsIgnoreCase(token.getValue(), location.getIdElement().getIdPart())));
        }
        if (params.getIdentifier() != null) {
            predicate = predicate
                    .and(location -> tokenAndList(params.getIdentifier().getValuesAsQueryTokens(), token -> location
                            .getIdentifier().stream().anyMatch(identifier -> identifierMatches(identifier, token))));
        }
        if (params.getName() != null) {
            predicate = predicate.and(location -> stringAndList(params.getName().getValuesAsQueryTokens(),
                    value -> stringMatches(location.getName(), value)));
        }
        if (params.getStatus() != null) {
            predicate = predicate.and(
                    location -> tokenAndList(params.getStatus().getValuesAsQueryTokens(), token -> location.hasStatus()
                            && equalsIgnoreCase(token.getValue(), location.getStatus().toCode())));
        }
        if (params.getPartOf() != null) {
            predicate = predicate.and(location -> referenceAndList(params.getPartOf().getValuesAsQueryTokens(),
                    reference -> equalsIgnoreCase(reference.getIdPart(), referencedId(location))));
        }
        if (params.getLastUpdated() != null) {
            predicate = predicate.and(location -> location.getMeta().hasLastUpdated()
                    && DateParamBounds.contains(params.getLastUpdated(), location.getMeta().getLastUpdated()));
        }
        if (params.getTag() != null) {
            predicate = predicate.and(location -> tokenAndList(params.getTag().getValuesAsQueryTokens(),
                    token -> location.getMeta().getTag().stream().anyMatch(tag -> tagMatches(tag, token))));
        }
        return predicate;
    }

    private static boolean identifierMatches(Identifier identifier, TokenParam token) {
        if (!equalsIgnoreCase(token.getValue(), identifier.getValue())) {
            return false;
        }
        String system = token.getSystem();
        return system == null || system.isBlank() || system.equals(identifier.getSystem());
    }

    private static boolean tagMatches(Coding tag, TokenParam token) {
        if (!equalsIgnoreCase(token.getValue(), tag.getCode())) {
            return false;
        }
        String system = token.getSystem();
        return system == null || system.isBlank() || system.equals(tag.getSystem());
    }

    private static boolean stringMatches(String actual, StringParam value) {
        if (actual == null || value.getValue() == null) {
            return false;
        }
        if (value.isExact()) {
            return actual.equals(value.getValue());
        }
        String haystack = actual.toLowerCase(Locale.ROOT);
        String needle = value.getValue().toLowerCase(Locale.ROOT);
        return value.isContains() ? haystack.contains(needle) : haystack.startsWith(needle);
    }

    private static boolean equalsIgnoreCase(String expected, String actual) {
        return expected != null && actual != null && expected.trim().equalsIgnoreCase(actual.trim());
    }

    private static boolean tokenAndList(List<TokenOrListParam> andList, Predicate<TokenParam> tokenMatches) {
        return andList.stream().allMatch(
                orList -> orList.getValuesAsQueryTokens().stream().filter(Objects::nonNull).anyMatch(tokenMatches));
    }

    private static boolean stringAndList(List<StringOrListParam> andList, Predicate<StringParam> valueMatches) {
        return andList.stream().allMatch(
                orList -> orList.getValuesAsQueryTokens().stream().filter(Objects::nonNull).anyMatch(valueMatches));
    }

    private static boolean referenceAndList(List<ReferenceOrListParam> andList,
            Predicate<ReferenceParam> referenceMatches) {
        return andList.stream().allMatch(
                orList -> orList.getValuesAsQueryTokens().stream().filter(Objects::nonNull).anyMatch(referenceMatches));
    }
}
