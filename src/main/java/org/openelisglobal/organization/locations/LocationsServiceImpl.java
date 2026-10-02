package org.openelisglobal.organization.locations;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.organization.locations.LocationsApi.ActiveRequest;
import org.openelisglobal.organization.locations.LocationsApi.ActiveResult;
import org.openelisglobal.organization.locations.LocationsApi.Area;
import org.openelisglobal.organization.locations.LocationsApi.AreaLevel;
import org.openelisglobal.organization.locations.LocationsApi.AreaRequest;
import org.openelisglobal.organization.locations.LocationsApi.ChildUsage;
import org.openelisglobal.organization.locations.LocationsApi.Detail;
import org.openelisglobal.organization.locations.LocationsApi.HistoryEntry;
import org.openelisglobal.organization.locations.LocationsApi.Identifier;
import org.openelisglobal.organization.locations.LocationsApi.ListRef;
import org.openelisglobal.organization.locations.LocationsApi.Lists;
import org.openelisglobal.organization.locations.LocationsApi.LocationRef;
import org.openelisglobal.organization.locations.LocationsApi.Page;
import org.openelisglobal.organization.locations.LocationsApi.Query;
import org.openelisglobal.organization.locations.LocationsApi.Referral;
import org.openelisglobal.organization.locations.LocationsApi.Row;
import org.openelisglobal.organization.locations.LocationsApi.SaveRequest;
import org.openelisglobal.organization.locations.LocationsApi.SaveResult;
import org.openelisglobal.organization.locations.LocationsApi.Site;
import org.openelisglobal.organization.locations.LocationsApi.TypeRef;
import org.openelisglobal.organization.locations.LocationsApi.Usage;
import org.openelisglobal.organization.locations.LocationsApi.UsageDetail;
import org.openelisglobal.organization.locations.LocationsApi.Ward;
import org.openelisglobal.organization.locations.LocationsApi.WardRequest;
import org.openelisglobal.organization.service.OrganizationChangeService;
import org.openelisglobal.organization.service.OrganizationChangeService.FieldChange;
import org.openelisglobal.organization.service.OrganizationIdentifierService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationChange;
import org.openelisglobal.organization.valueholder.OrganizationIdentifier;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.organization.valueholder.WardServiceType;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationsServiceImpl implements LocationsService {

    static final String DEPT_TYPE = "dept";
    static final String SITE_TYPE = "sampling site";
    static final Set<String> REFERRAL_TYPE_NAMES = Set.of("referrallab", "referral lab");
    static final String CATEGORY_LIST = "Facility category";
    static final String OWNERSHIP_LIST = "Facility ownership";
    static final String SITE_TYPE_LIST = "Sampling Site Type";
    static final String ZONE_LIST = "Environmental Zone";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationTypeService organizationTypeService;
    @Autowired
    private OrganizationIdentifierService identifierService;
    @Autowired
    private OrganizationChangeService changeService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private VectorSamplingSiteService samplingSiteService;
    @Autowired
    private LocationsUsageDAO usageDAO;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private SystemUserService systemUserService;

    // ---------------------------------------------------------------- kinds

    static boolean isLevelType(OrganizationType type) {
        return type.getHierarchyLevel() != null && type.getHierarchyLevel() > 0;
    }

    static boolean isReferralType(OrganizationType type) {
        return type.getName() != null && REFERRAL_TYPE_NAMES.contains(type.getName().trim().toLowerCase(Locale.ROOT));
    }

    static String kindOf(Organization organization) {
        Set<OrganizationType> types = organization.getOrganizationTypes();
        if (types == null || types.isEmpty()) {
            return LocationsApi.KIND_FACILITY;
        }
        boolean dept = false;
        boolean site = false;
        for (OrganizationType type : types) {
            if (isLevelType(type)) {
                return LocationsApi.KIND_AREA;
            }
            String name = type.getName() == null ? "" : type.getName().trim().toLowerCase(Locale.ROOT);
            dept |= DEPT_TYPE.equals(name);
            site |= SITE_TYPE.equals(name);
        }
        if (dept) {
            return LocationsApi.KIND_WARD;
        }
        return site ? LocationsApi.KIND_SITE : LocationsApi.KIND_FACILITY;
    }

    private static boolean active(Organization organization) {
        return "Y".equals(organization.getIsActive());
    }

    private static boolean isRegistry(Organization organization) {
        return LocationsApi.SOURCE_REGISTRY.equals(organization.getSource());
    }

    private static String norm(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && !needle.isEmpty() && norm(haystack).contains(needle);
    }

    private static Long millis(Organization organization) {
        return organization.getLastupdated() == null ? null : organization.getLastupdated().getTime();
    }

    private static Integer level(Organization area) {
        if (area.getOrganizationTypes() == null) {
            return null;
        }
        for (OrganizationType type : area.getOrganizationTypes()) {
            if (isLevelType(type)) {
                return type.getHierarchyLevel();
            }
        }
        return null;
    }

    private static String levelName(Organization area) {
        if (area.getOrganizationTypes() == null) {
            return null;
        }
        for (OrganizationType type : area.getOrganizationTypes()) {
            if (isLevelType(type)) {
                return type.getName();
            }
        }
        return null;
    }

    /**
     * The parents of a record, top-down; a self-referencing parent is not a parent.
     */
    private static List<Organization> ancestors(Organization organization) {
        List<Organization> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        seen.add(organization.getId());
        Organization parent = organization.getOrganization();
        while (parent != null && parent.getId() != null && seen.add(parent.getId())) {
            chain.add(0, parent);
            parent = parent.getOrganization();
        }
        return chain;
    }

    private static Organization realParent(Organization organization) {
        Organization parent = organization.getOrganization();
        if (parent == null || parent.getId() == null || parent.getId().equals(organization.getId())) {
            return null;
        }
        return parent;
    }

    private LocationRef locationOf(Organization organization) {
        Organization parent = realParent(organization);
        if (parent == null || !LocationsApi.KIND_AREA.equals(kindOf(parent))) {
            return null;
        }
        List<String> path = new ArrayList<>();
        for (Organization ancestor : ancestors(organization)) {
            path.add(ancestor.getOrganizationName());
        }
        return new LocationRef(parent.getId(), parent.getOrganizationName(), levelName(parent), path);
    }

    private static List<TypeRef> typeRefs(Organization organization) {
        List<TypeRef> refs = new ArrayList<>();
        if (organization.getOrganizationTypes() != null) {
            for (OrganizationType type : organization.getOrganizationTypes()) {
                refs.add(new TypeRef(type.getId(), type.getName(), type.getHierarchyLevel()));
            }
        }
        refs.sort(Comparator.comparing(TypeRef::name, String.CASE_INSENSITIVE_ORDER));
        return refs;
    }

    private static boolean reviewOverdue(Organization organization) {
        return organization.getNextReviewDue() != null && organization.getNextReviewDue().isBefore(LocalDate.now());
    }

    private static boolean accreditationExpired(Organization organization) {
        return organization.getAccreditationExpiry() != null
                && organization.getAccreditationExpiry().isBefore(LocalDate.now());
    }

    private boolean hasReferralType(Organization organization) {
        return organization.getOrganizationTypes() != null
                && organization.getOrganizationTypes().stream().anyMatch(LocationsServiceImpl::isReferralType);
    }

    // ---------------------------------------------------------------- lists

    private Map<String, String> dictionaryLabels() {
        Map<String, String> labels = new HashMap<>();
        for (String list : List.of(CATEGORY_LIST, OWNERSHIP_LIST)) {
            for (Dictionary entry : entriesOf(list)) {
                labels.put(entry.getId(), entry.getDictEntry());
            }
        }
        return labels;
    }

    private List<Dictionary> entriesOf(String categoryName) {
        try {
            List<Dictionary> entries = dictionaryService.getDictionaryEntrysByCategoryAbbreviation("categoryName",
                    categoryName, true);
            return entries == null ? List.of() : entries;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private static ListRef listRef(Map<String, String> labels, String id) {
        if (GenericValidator.isBlankOrNull(id)) {
            return null;
        }
        return new ListRef(id, labels.getOrDefault(id, id));
    }

    private Set<String> closedStatusIds() {
        Set<String> closed = new HashSet<>();
        String finished = statusService.getStatusID(OrderStatus.Finished);
        if (finished != null) {
            closed.add(finished);
        }
        return closed;
    }

    @Override
    @Transactional(readOnly = true)
    public Page list(Query query) {
        String view = LocationsApi.VIEW_SITES.equals(query.view()) ? LocationsApi.VIEW_SITES
                : LocationsApi.VIEW_ORGANIZATIONS;
        String wantedKind = LocationsApi.VIEW_SITES.equals(view) ? LocationsApi.KIND_SITE : LocationsApi.KIND_FACILITY;
        String status = query.status() == null ? LocationsApi.STATUS_ACTIVE : query.status();
        String text = norm(query.q());
        int pageSize = query.pageSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(query.pageSize(), MAX_PAGE_SIZE);
        int page = Math.max(1, query.page());

        List<Organization> all = organizationService.getAllWithTypes();
        Map<String, Organization> byId = all.stream().collect(Collectors.toMap(Organization::getId, o -> o));
        Map<String, List<Organization>> wardsByParent = new HashMap<>();
        for (Organization organization : all) {
            if (LocationsApi.KIND_WARD.equals(kindOf(organization)) && realParent(organization) != null) {
                wardsByParent.computeIfAbsent(realParent(organization).getId(), k -> new ArrayList<>())
                        .add(organization);
            }
        }
        Map<String, String> identifierNotes = new HashMap<>();
        Set<String> exactIdentifierHits = new HashSet<>();
        Map<Integer, List<String>> formerNames = Collections.emptyMap();
        if (!text.isEmpty()) {
            for (OrganizationIdentifier identifier : identifierService.searchByValue(text, 500)) {
                String id = String.valueOf(identifier.getOrganizationId());
                identifierNotes.putIfAbsent(id, identifier.getLabel() + " " + identifier.getValue());
                if (norm(identifier.getValue()).equals(text)) {
                    exactIdentifierHits.add(id);
                }
            }
            formerNames = changeService.formerNames();
        }

        List<Row> rows = new ArrayList<>();
        Map<String, String> labels = dictionaryLabels();
        List<Organization> matches = new ArrayList<>();
        Map<String, String> notes = new HashMap<>();
        for (Organization organization : all) {
            if (!wantedKind.equals(kindOf(organization))) {
                continue;
            }
            if (!statusMatches(status, organization)) {
                continue;
            }
            if (query.typeIds() != null && !query.typeIds().isEmpty() && organization.getOrganizationTypes().stream()
                    .noneMatch(type -> query.typeIds().contains(type.getId()))) {
                continue;
            }
            if (query.categoryIds() != null && !query.categoryIds().isEmpty()
                    && !query.categoryIds().contains(organization.getCategoryId())) {
                continue;
            }
            if (query.ownershipIds() != null && !query.ownershipIds().isEmpty()
                    && !query.ownershipIds().contains(organization.getOwnershipId())) {
                continue;
            }
            if (query.reviewOverdue() && !(reviewOverdue(organization) || accreditationExpired(organization))) {
                continue;
            }
            if (!GenericValidator.isBlankOrNull(query.locationId()) && ancestors(organization).stream()
                    .noneMatch(ancestor -> query.locationId().equals(ancestor.getId()))) {
                continue;
            }
            if (!text.isEmpty()) {
                String note = matchNote(organization, text, identifierNotes, formerNames,
                        wardsByParent.getOrDefault(organization.getId(), List.of()));
                if (note == null) {
                    continue;
                }
                if (!note.isEmpty()) {
                    notes.put(organization.getId(), note);
                }
            }
            matches.add(organization);
        }

        Comparator<Organization> byName = Comparator.comparing(o -> norm(o.getOrganizationName()));
        Comparator<Organization> order = Comparator.<Organization, Integer>comparing(
                o -> exactIdentifierHits.contains(o.getId()) || norm(o.getCode()).equals(text) ? 0 : 1);
        if ("location".equals(query.sort())) {
            order = order.thenComparing(o -> norm(
                    ancestors(o).stream().map(Organization::getOrganizationName).collect(Collectors.joining(" / "))));
        }
        matches.sort(order.thenComparing(byName));

        int from = Math.min((page - 1) * pageSize, matches.size());
        int to = Math.min(from + pageSize, matches.size());
        List<Organization> pageItems = matches.subList(from, to);

        Map<String, Usage> usage = usageFor(pageItems);
        for (Organization organization : pageItems) {
            rows.add(row(organization, labels, usage.get(organization.getId()),
                    (int) wardsByParent.getOrDefault(organization.getId(), List.of()).stream()
                            .filter(LocationsServiceImpl::active).count(),
                    notes.get(organization.getId())));
        }
        return new Page(rows, matches.size(), page, pageSize);
    }

    private static boolean statusMatches(String status, Organization organization) {
        if (LocationsApi.STATUS_ALL.equals(status)) {
            return true;
        }
        return LocationsApi.STATUS_INACTIVE.equals(status) != active(organization);
    }

    /**
     * Whether the record matches the search: null when it does not, an empty string
     * for a match on its own name, code or short name, and the note to show under
     * the row when it matched on another identifier, a former name or a ward.
     */
    private static String matchNote(Organization organization, String text, Map<String, String> identifierNotes,
            Map<Integer, List<String>> formerNames, List<Organization> wards) {
        if (contains(organization.getOrganizationName(), text) || contains(organization.getCode(), text)
                || contains(organization.getShortName(), text)) {
            return "";
        }
        String identifierNote = identifierNotes.get(organization.getId());
        if (identifierNote != null) {
            return identifierNote;
        }
        Integer numericId = organization.getId().matches("\\d+") ? Integer.valueOf(organization.getId()) : null;
        for (String former : formerNames.getOrDefault(numericId, List.of())) {
            if (contains(former, text)) {
                return "formerly:" + former;
            }
        }
        for (Organization ward : wards) {
            if (contains(ward.getOrganizationName(), text) || contains(ward.getCode(), text)) {
                return "ward:" + ward.getOrganizationName();
            }
        }
        return null;
    }

    private Map<String, Usage> usageFor(List<Organization> organizations) {
        Map<String, Usage> usage = new HashMap<>();
        if (organizations.isEmpty()) {
            return usage;
        }
        Set<String> closed = closedStatusIds();
        List<String> orgIds = organizations.stream().map(Organization::getId).collect(Collectors.toList());
        usage.putAll(usageDAO.orderCountsForOrganizations(orgIds, closed));
        List<Integer> numericIds = orgIds.stream().filter(id -> id.matches("\\d+")).map(Integer::valueOf)
                .collect(Collectors.toList());
        List<VectorSamplingSite> sites = samplingSiteService.getByOrganizationIds(numericIds);
        if (!sites.isEmpty()) {
            Map<String, String> siteToOrg = new HashMap<>();
            for (VectorSamplingSite site : sites) {
                siteToOrg.put(String.valueOf(site.getId()), String.valueOf(site.getOrganizationId()));
            }
            usageDAO.orderCountsForSites(siteToOrg.keySet(), closed).forEach((siteId, counts) -> {
                String orgId = siteToOrg.get(siteId);
                Usage existing = usage.get(orgId);
                usage.put(orgId, existing == null ? counts
                        : new Usage(existing.open() + counts.open(), existing.total() + counts.total()));
            });
        }
        return usage;
    }

    private Row row(Organization organization, Map<String, String> labels, Usage usage, int wardCount, String note) {
        String siteType = null;
        if (LocationsApi.KIND_SITE.equals(kindOf(organization)) && organization.getId().matches("\\d+")) {
            VectorSamplingSite site = samplingSiteService.getByOrganizationId(Integer.valueOf(organization.getId()));
            siteType = site == null ? null : site.getType();
        }
        return new Row(organization.getId(), organization.getOrganizationName(), organization.getShortName(),
                organization.getCode(), kindOf(organization), typeRefs(organization),
                listRef(labels, organization.getCategoryId()), listRef(labels, organization.getOwnershipId()),
                locationOf(organization), wardCount, siteType, usage == null ? new Usage(0, 0) : usage,
                active(organization), isRegistry(organization), reviewOverdue(organization),
                accreditationExpired(organization), note);
    }

    // ---------------------------------------------------------------- detail

    @Override
    @Transactional(readOnly = true)
    public Detail get(String id) {
        Organization organization = require(id);
        return detail(organization);
    }

    private Organization require(String id) {
        Organization organization = GenericValidator.isBlankOrNull(id) || !id.trim().matches("\\d+") ? null
                : organizationService.getOrganizationById(id.trim());
        if (organization == null) {
            throw new LocationsNotFoundException("No organization with id " + id);
        }
        return organization;
    }

    private Detail detail(Organization organization) {
        Map<String, String> labels = dictionaryLabels();
        Integer numericId = Integer.valueOf(organization.getId());
        List<Identifier> identifiers = identifierService.getForOrganization(numericId).stream()
                .map(i -> new Identifier(i.getId(), i.getLabel(), i.getValue(), i.isReporting()))
                .collect(Collectors.toList());
        List<Ward> wards = LocationsApi.KIND_FACILITY.equals(kindOf(organization)) ? wards(organization.getId(), true)
                : List.of();
        Referral referral = hasReferralType(organization)
                ? new Referral(organization.getApprovalStatus(), organization.getAccreditationBody(),
                        organization.getAccreditationNumber(), text(organization.getAccreditationExpiry()),
                        text(organization.getLastReviewDate()), text(organization.getNextReviewDue()),
                        organization.getReviewNotes())
                : null;
        Site site = null;
        if (LocationsApi.KIND_SITE.equals(kindOf(organization))) {
            VectorSamplingSite stored = samplingSiteService.getByOrganizationId(numericId);
            site = stored == null ? new Site(null, null, null, null)
                    : new Site(stored.getId(), stored.getType(), stored.getSubtype(), stored.getEnvironmentalZone());
        }
        Usage usage = usageFor(List.of(organization)).get(organization.getId());
        int wardCount = (int) wards.stream().filter(Ward::active).count();
        Organization parent = realParent(organization);
        return new Detail(row(organization, labels, usage, wardCount, null), parent == null ? null : parent.getId(),
                organization.getStreetAddress(), organization.getCity(), organization.getState(),
                organization.getZipCode(), organization.getPhone(), organization.getFax(), organization.getEmail(),
                organization.getInternetAddress(), organization.getContactName(), organization.getDescription(),
                organization.getGpsLatitude(), organization.getGpsLongitude(), organization.getServiceType(),
                organization.getSource(), identifiers, wards, referral, site, millis(organization),
                changeService.getForOrganization(numericId).size());
    }

    private static String text(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private static String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static LocalDate date(String text, String field, Map<String, String> errors) {
        if (GenericValidator.isBlankOrNull(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (RuntimeException e) {
            errors.put(field, "Enter the date as YYYY-MM-DD");
            return null;
        }
    }

    // ---------------------------------------------------------------- save

    @Override
    @Transactional
    public SaveResult save(SaveRequest request, String sysUserId) {
        return save(request, sysUserId, null);
    }

    @Override
    @Transactional
    public SaveResult save(SaveRequest request, String sysUserId, String actor) {
        Map<String, OrganizationType> types = organizationTypeService.getAllOrganizationTypes().stream()
                .collect(Collectors.toMap(OrganizationType::getId, t -> t, (a, b) -> a, LinkedHashMap::new));
        boolean isNew = GenericValidator.isBlankOrNull(request.id());
        Organization organization = isNew ? new Organization() : require(request.id());
        if (!isNew && request.lastupdated() != null && millis(organization) != null
                && !request.lastupdated().equals(millis(organization))) {
            throw new LocationsConflictException("Another admin saved this record first", detail(organization));
        }
        String kind = isNew ? request.kind() : kindOf(organization);
        if (GenericValidator.isBlankOrNull(kind)) {
            kind = LocationsApi.KIND_FACILITY;
        }
        List<String> typeIds = resolveTypeIds(kind, request, organization, types);
        Map<String, String> errors = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        validate(request, kind, typeIds, types, organization, errors, warnings);
        List<Identifier> identifiers = cleanIdentifiers(request.identifiers(), errors);
        checkIdentifierClashes(identifiers, kind, organization.getId(), errors);
        if (!errors.isEmpty()) {
            throw new LocationsValidationException("The record was not saved", errors);
        }

        Map<String, String> before = snapshot(organization, types);
        Organization parent = GenericValidator.isBlankOrNull(request.parentId()) ? null
                : organizationService.getOrganizationById(request.parentId());
        applyFields(organization, request, kind, parent, identifiers, errors);
        if (!errors.isEmpty()) {
            throw new LocationsValidationException("The record was not saved", errors);
        }
        organization.setSysUserId(sysUserId);
        if (isNew) {
            organization.setIsActive("Y");
            organization.setMlsSentinelLabFlag("N");
            if (GenericValidator.isBlankOrNull(organization.getSource())) {
                organization.setSource(LocationsApi.SOURCE_LOCAL);
            }
            organization.setId(organizationService.insertUnchecked(organization));
        } else {
            organization = organizationService.updateUnchecked(organization);
        }
        linkTypes(organization, typeIds);
        Integer numericId = Integer.valueOf(organization.getId());
        List<OrganizationIdentifier> wanted = identifiers.stream()
                .map(i -> new OrganizationIdentifier(numericId, i.label(), i.value(), i.reporting()))
                .collect(Collectors.toList());
        List<Identifier> storedBefore = isNew ? List.of()
                : identifierService.getForOrganization(numericId).stream()
                        .map(i -> new Identifier(i.getId(), i.getLabel(), i.getValue(), i.isReporting()))
                        .collect(Collectors.toList());
        identifierService.replaceForOrganization(numericId, wanted, sysUserId);
        if (LocationsApi.KIND_SITE.equals(kind)) {
            syncSite(organization, request.site(), sysUserId);
        }

        Organization reloaded = organizationService.get(organization.getId());
        Map<String, String> after = snapshot(reloaded, types);
        if (isNew) {
            changeService.record(numericId, OrganizationChange.ACTION_CREATED, List.of(), sysUserId, actor);
        } else {
            List<FieldChange> changes = diff(before, after);
            List<FieldChange> referralChanges = changes.stream().filter(c -> c.field().startsWith("referral."))
                    .collect(Collectors.toList());
            changes.removeAll(referralChanges);
            if (!changes.isEmpty()) {
                changeService.record(numericId, OrganizationChange.ACTION_EDITED, changes, sysUserId, actor);
            }
            if (!referralChanges.isEmpty()) {
                changeService.record(numericId, OrganizationChange.ACTION_REFERRAL_REVIEW, referralChanges, sysUserId,
                        actor);
            }
        }
        if (!isNew && !sameIdentifiers(storedBefore, identifiers)) {
            changeService.record(numericId, OrganizationChange.ACTION_IDENTIFIERS,
                    List.of(new FieldChange("identifiers", describe(storedBefore), describe(identifiers))), sysUserId,
                    actor);
        }
        refreshDisplayLists();
        return new SaveResult(detail(reloaded), warnings);
    }

    private List<String> resolveTypeIds(String kind, SaveRequest request, Organization organization,
            Map<String, OrganizationType> types) {
        if (LocationsApi.KIND_SITE.equals(kind)) {
            OrganizationType siteType = organizationTypeService.getOrganizationTypeByName(SITE_TYPE);
            if (siteType == null) {
                throw new LocationsValidationException("types", "The organization type 'sampling site' is missing");
            }
            return List.of(siteType.getId());
        }
        if (LocationsApi.KIND_WARD.equals(kind)) {
            OrganizationType dept = organizationTypeService.getOrganizationTypeByName(DEPT_TYPE);
            return dept == null ? List.of() : List.of(dept.getId());
        }
        if (request.typeIds() != null && !request.typeIds().isEmpty()) {
            return new ArrayList<>(new LinkedHashSet<>(request.typeIds()));
        }
        if (organization.getOrganizationTypes() != null) {
            return organization.getOrganizationTypes().stream().map(OrganizationType::getId)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void validate(SaveRequest request, String kind, List<String> typeIds, Map<String, OrganizationType> types,
            Organization organization, Map<String, String> errors, List<String> warnings) {
        if (GenericValidator.isBlankOrNull(request.name())) {
            errors.put("name", "Name is required");
        }
        if (LocationsApi.KIND_FACILITY.equals(kind)) {
            if (typeIds.isEmpty()) {
                errors.put("types", "Choose at least one organization type");
            }
            for (String typeId : typeIds) {
                OrganizationType type = types.get(typeId);
                if (type == null) {
                    errors.put("types", "Unknown organization type " + typeId);
                } else if (isLevelType(type) || DEPT_TYPE.equalsIgnoreCase(type.getName())
                        || SITE_TYPE.equalsIgnoreCase(type.getName())) {
                    errors.put("types", "A facility cannot carry the type '" + type.getName() + "'");
                }
            }
        }
        if (!GenericValidator.isBlankOrNull(request.parentId())) {
            Organization parent = organizationService.getOrganizationById(request.parentId());
            if (parent == null) {
                errors.put("parent", "The location no longer exists");
            } else if (!LocationsApi.KIND_AREA.equals(kindOf(parent))) {
                errors.put("parent", "The location must be a geographic area");
            }
        }
        if (request.gpsLatitude() != null && (request.gpsLatitude().compareTo(BigDecimal.valueOf(-90)) < 0
                || request.gpsLatitude().compareTo(BigDecimal.valueOf(90)) > 0)) {
            errors.put("gpsLatitude", "Enter decimal degrees: latitude -90 to 90, longitude -180 to 180");
        }
        if (request.gpsLongitude() != null && (request.gpsLongitude().compareTo(BigDecimal.valueOf(-180)) < 0
                || request.gpsLongitude().compareTo(BigDecimal.valueOf(180)) > 0)) {
            errors.put("gpsLongitude", "Enter decimal degrees: latitude -90 to 90, longitude -180 to 180");
        }
        if (!GenericValidator.isBlankOrNull(request.email()) && !EMAIL.matcher(request.email().trim()).matches()) {
            errors.put("email", "Enter a valid email address");
        }
        if (request.referral() != null && !GenericValidator.isBlankOrNull(request.referral().approvalStatus())
                && !LocationsApi.REFERRAL_STATUSES.contains(request.referral().approvalStatus().trim())) {
            errors.put("referral.approvalStatus", "Choose an approval status");
        }
        if (!GenericValidator.isBlankOrNull(request.serviceType())
                && WardServiceType.parse(request.serviceType()) == null) {
            errors.put("serviceType", "Choose a service type");
        }
        if (!GenericValidator.isBlankOrNull(request.name())) {
            String wanted = norm(request.name());
            for (Organization other : organizationService.getAllWithTypes()) {
                if (other.getId().equals(organization.getId()) || !active(other) || !kind.equals(kindOf(other))
                        || !wanted.equals(norm(other.getOrganizationName()))) {
                    continue;
                }
                String otherParent = realParent(other) == null ? "" : realParent(other).getId();
                String thisParent = request.parentId() == null ? "" : request.parentId();
                boolean sharesType = other.getOrganizationTypes().stream().anyMatch(t -> typeIds.contains(t.getId()));
                if (otherParent.equals(thisParent) && (sharesType || !LocationsApi.KIND_FACILITY.equals(kind))) {
                    warnings.add("Another active record named \"" + other.getOrganizationName() + "\" exists here");
                    break;
                }
            }
        }
    }

    private static List<Identifier> cleanIdentifiers(List<Identifier> identifiers, Map<String, String> errors) {
        List<Identifier> clean = new ArrayList<>();
        if (identifiers == null) {
            return clean;
        }
        Set<String> labels = new HashSet<>();
        int reporting = 0;
        for (Identifier identifier : identifiers) {
            boolean hasLabel = !GenericValidator.isBlankOrNull(identifier.label());
            boolean hasValue = !GenericValidator.isBlankOrNull(identifier.value());
            if (!hasLabel && !hasValue) {
                continue;
            }
            if (!hasLabel) {
                errors.put("identifiers", "Every identifier needs a label");
                continue;
            }
            if (!hasValue) {
                errors.put("identifiers", "Every identifier needs a value");
                continue;
            }
            if (!labels.add(norm(identifier.label()))) {
                errors.put("identifiers", "The label \"" + identifier.label().trim() + "\" is used twice");
                continue;
            }
            if (identifier.reporting()) {
                reporting++;
            }
            clean.add(new Identifier(identifier.id(), identifier.label().trim(), identifier.value().trim(),
                    identifier.reporting()));
        }
        if (reporting > 1) {
            errors.put("identifiers", "Only one identifier can be the reporting code");
        }
        return clean;
    }

    private void checkIdentifierClashes(List<Identifier> identifiers, String kind, String selfId,
            Map<String, String> errors) {
        for (Identifier identifier : identifiers) {
            for (OrganizationIdentifier other : identifierService.getByLabelAndValue(identifier.label(),
                    identifier.value())) {
                String otherId = String.valueOf(other.getOrganizationId());
                if (otherId.equals(selfId)) {
                    continue;
                }
                Organization owner = organizationService.getOrganizationById(otherId);
                if (owner != null && kind.equals(kindOf(owner))) {
                    errors.put("identifiers", identifier.label() + " " + identifier.value() + " is already used by "
                            + owner.getOrganizationName());
                    return;
                }
            }
        }
    }

    private void applyFields(Organization organization, SaveRequest request, String kind, Organization parent,
            List<Identifier> identifiers, Map<String, String> errors) {
        organization.setOrganizationName(request.name().trim());
        organization.setShortName(trimTo(request.shortName(), 15));
        organization.setOrganization(parent);
        organization.setCategoryId(blankToNull(request.categoryId()));
        organization.setOwnershipId(blankToNull(request.ownershipId()));
        organization.setDescription(trimTo(request.description(), 1000));
        organization.setStreetAddress(trimTo(request.streetAddress(), 30));
        organization.setCity(trimTo(request.city(), 30));
        organization.setState(trimTo(request.state(), 2));
        organization.setZipCode(trimTo(request.zipCode(), 10));
        organization.setGpsLatitude(request.gpsLatitude());
        organization.setGpsLongitude(request.gpsLongitude());
        organization.setContactName(trimTo(request.contactName(), 100));
        organization.setPhone(trimTo(request.phone(), 20));
        organization.setFax(trimTo(request.fax(), 20));
        organization.setEmail(trimTo(request.email(), 255));
        organization.setInternetAddress(trimTo(request.internetAddress(), 40));
        WardServiceType serviceType = WardServiceType.parse(request.serviceType());
        organization.setServiceType(serviceType == null ? null : serviceType.name());
        Identifier reporting = identifiers.stream().filter(Identifier::reporting).findFirst().orElse(null);
        organization.setCode(reporting == null ? null : trimTo(reporting.value(), 20));
        Identifier clia = identifiers.stream()
                .filter(i -> OrganizationIdentifier.CLIA_LABEL.equalsIgnoreCase(i.label())).findFirst().orElse(null);
        organization.setCliaNum(clia == null ? null : trimTo(clia.value(), 12));
        Referral referral = request.referral();
        if (referral != null) {
            organization.setApprovalStatus(blankToNull(referral.approvalStatus()));
            organization.setAccreditationBody(trimTo(referral.accreditationBody(), 100));
            organization.setAccreditationNumber(trimTo(referral.accreditationNumber(), 50));
            organization.setAccreditationExpiry(
                    date(referral.accreditationExpiry(), "referral.accreditationExpiry", errors));
            organization.setLastReviewDate(date(referral.lastReviewDate(), "referral.lastReviewDate", errors));
            organization.setNextReviewDue(date(referral.nextReviewDue(), "referral.nextReviewDue", errors));
            organization.setReviewNotes(trimTo(referral.reviewNotes(), 1000));
        }
        if (LocationsApi.KIND_SITE.equals(kind) && reporting == null) {
            errors.put("identifiers", "A sampling site needs a code");
        }
    }

    private static String trimTo(String text, int length) {
        if (GenericValidator.isBlankOrNull(text)) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() > length ? trimmed.substring(0, length) : trimmed;
    }

    private static String blankToNull(String text) {
        return GenericValidator.isBlankOrNull(text) ? null : text.trim();
    }

    private void linkTypes(Organization organization, List<String> typeIds) {
        organizationService.deleteAllLinksForOrganization(organization.getId());
        Set<OrganizationType> linked = new LinkedHashSet<>();
        for (String typeId : typeIds) {
            organizationService.linkOrganizationAndType(organization, typeId);
            OrganizationType type = organizationTypeService.get(typeId);
            if (type != null) {
                linked.add(type);
            }
        }
        organization.setOrganizationTypes(linked);
    }

    private void syncSite(Organization organization, Site fields, String sysUserId) {
        Integer numericId = Integer.valueOf(organization.getId());
        VectorSamplingSite site = samplingSiteService.getByOrganizationId(numericId);
        boolean isNew = site == null;
        if (isNew) {
            site = new VectorSamplingSite();
            site.setOrganizationId(numericId);
            site.setSource(LocationsApi.SOURCE_LOCAL);
        }
        site.setCode(organization.getCode());
        site.setName(organization.getOrganizationName());
        site.setContactName(organization.getContactName());
        site.setContactPhone(organization.getPhone());
        site.setGpsLatitude(
                organization.getGpsLatitude() == null ? null : organization.getGpsLatitude().toPlainString());
        site.setGpsLongitude(
                organization.getGpsLongitude() == null ? null : organization.getGpsLongitude().toPlainString());
        site.setDescription(organization.getDescription());
        Organization parent = realParent(organization);
        site.setLocationOrgId(parent == null ? null : parent.getId());
        site.setActive(active(organization));
        if (fields != null) {
            site.setType(blankToNull(fields.siteType()));
            site.setSubtype(blankToNull(fields.subtype()));
            site.setEnvironmentalZone(blankToNull(fields.environmentalZone()));
        }
        site.setSysUserId(sysUserId);
        if (isNew) {
            samplingSiteService.insert(site);
        } else {
            samplingSiteService.update(site);
        }
    }

    private static final Map<String, Function<Organization, String>> TRACKED = trackedFields();

    private static Map<String, Function<Organization, String>> trackedFields() {
        Map<String, Function<Organization, String>> fields = new LinkedHashMap<>();
        fields.put("name", Organization::getOrganizationName);
        fields.put("shortName", Organization::getShortName);
        fields.put("code", Organization::getCode);
        fields.put("description", Organization::getDescription);
        fields.put("location", o -> {
            Organization parent = realParent(o);
            return parent == null ? null : parent.getOrganizationName();
        });
        fields.put("streetAddress", Organization::getStreetAddress);
        fields.put("city", Organization::getCity);
        fields.put("state", Organization::getState);
        fields.put("zipCode", Organization::getZipCode);
        fields.put("gpsLatitude", o -> decimalText(o.getGpsLatitude()));
        fields.put("gpsLongitude", o -> decimalText(o.getGpsLongitude()));
        fields.put("contactName", Organization::getContactName);
        fields.put("phone", Organization::getPhone);
        fields.put("fax", Organization::getFax);
        fields.put("email", Organization::getEmail);
        fields.put("internetAddress", Organization::getInternetAddress);
        fields.put("categoryId", Organization::getCategoryId);
        fields.put("ownershipId", Organization::getOwnershipId);
        fields.put("serviceType", Organization::getServiceType);
        fields.put("active", Organization::getIsActive);
        fields.put("referral.approvalStatus", Organization::getApprovalStatus);
        fields.put("referral.accreditationBody", Organization::getAccreditationBody);
        fields.put("referral.accreditationNumber", Organization::getAccreditationNumber);
        fields.put("referral.accreditationExpiry", o -> text(o.getAccreditationExpiry()));
        fields.put("referral.lastReviewDate", o -> text(o.getLastReviewDate()));
        fields.put("referral.nextReviewDue", o -> text(o.getNextReviewDue()));
        fields.put("referral.reviewNotes", Organization::getReviewNotes);
        return fields;
    }

    private static Map<String, String> snapshot(Organization organization, Map<String, OrganizationType> types) {
        Map<String, String> values = new LinkedHashMap<>();
        if (organization.getId() == null) {
            return values;
        }
        TRACKED.forEach((field, reader) -> values.put(field, reader.apply(organization)));
        if (organization.getOrganizationTypes() != null) {
            values.put("types", organization.getOrganizationTypes().stream().map(OrganizationType::getName)
                    .sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", ")));
        }
        return values;
    }

    private static List<FieldChange> diff(Map<String, String> before, Map<String, String> after) {
        List<FieldChange> changes = new ArrayList<>();
        Set<String> fields = new LinkedHashSet<>(before.keySet());
        fields.addAll(after.keySet());
        for (String field : fields) {
            String old = before.get(field);
            String now = after.get(field);
            if (!Objects.equals(old == null ? "" : old, now == null ? "" : now)) {
                changes.add(new FieldChange(field, old, now));
            }
        }
        return changes;
    }

    private static boolean sameIdentifiers(List<Identifier> a, List<Identifier> b) {
        return describe(a).equals(describe(b));
    }

    private static String describe(List<Identifier> identifiers) {
        return identifiers.stream().map(i -> i.label() + " " + i.value() + (i.reporting() ? " (reporting)" : ""))
                .sorted().collect(Collectors.joining(", "));
    }

    /**
     * Only the pickers built from organizations are rebuilt; a full
     * {@code refreshLists()} reloads every catalog and made a save take ten
     * seconds.
     */
    private static void refreshDisplayLists() {
        DisplayListService lists = DisplayListService.getInstance();
        if (lists != null) {
            lists.refreshList(DisplayListService.ListType.SAMPLE_PATIENT_REFERRING_CLINIC);
            lists.refreshList(DisplayListService.ListType.REFERRAL_ORGANIZATIONS);
        }
    }

    // ---------------------------------------------------------------- usage and
    // activation

    @Override
    @Transactional(readOnly = true)
    public UsageDetail usage(String id) {
        Organization organization = require(id);
        List<Organization> children = organizationService.getChildrenWithTypes(id).stream()
                .filter(LocationsServiceImpl::active).collect(Collectors.toList());
        List<Organization> all = new ArrayList<>(children);
        all.add(organization);
        Map<String, Usage> usage = usageFor(all);
        List<ChildUsage> childUsage = children.stream().map(child -> new ChildUsage(child.getId(),
                child.getOrganizationName(), kindOf(child), usage.getOrDefault(child.getId(), new Usage(0, 0))))
                .collect(Collectors.toList());
        return new UsageDetail(id, organization.getOrganizationName(), kindOf(organization),
                usage.getOrDefault(id, new Usage(0, 0)), childUsage);
    }

    @Override
    @Transactional
    public ActiveResult setActive(ActiveRequest request, String sysUserId) {
        return setActive(request, sysUserId, null);
    }

    @Override
    @Transactional
    public ActiveResult setActive(ActiveRequest request, String sysUserId, String actor) {
        List<String> changed = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (String id : request.ids() == null ? List.<String>of() : request.ids()) {
            Organization organization = require(id);
            if (request.active()) {
                Organization parent = realParent(organization);
                if (parent != null && !active(parent)) {
                    throw new LocationsConflictException("Reactivate " + parent.getOrganizationName() + " first.");
                }
                if (!active(organization)) {
                    activate(organization, true, sysUserId, actor, changed, names);
                }
            } else {
                if (LocationsApi.KIND_AREA.equals(kindOf(organization))) {
                    long inside = organizationService.countActiveChildren(List.of(id)).getOrDefault(id, 0L);
                    if (inside > 0) {
                        throw new LocationsConflictException(organization.getOrganizationName() + " still has " + inside
                                + " active areas, organizations or sites inside it. Deactivate or move those first.");
                    }
                }
                if (active(organization)) {
                    activate(organization, false, sysUserId, actor, changed, names);
                }
                if (request.includeChildren()) {
                    for (Organization child : organizationService.getChildrenWithTypes(id)) {
                        if (active(child) && !LocationsApi.KIND_AREA.equals(kindOf(child))) {
                            activate(child, false, sysUserId, actor, changed, names);
                        }
                    }
                }
            }
        }
        if (!changed.isEmpty()) {
            refreshDisplayLists();
        }
        return new ActiveResult(changed, names);
    }

    private void activate(Organization organization, boolean active, String sysUserId, String actor,
            List<String> changed, List<String> names) {
        String before = organization.getIsActive();
        organization.setIsActive(active ? "Y" : "N");
        organization.setSysUserId(sysUserId);
        organizationService.updateUnchecked(organization);
        Integer numericId = Integer.valueOf(organization.getId());
        changeService.record(numericId,
                active ? OrganizationChange.ACTION_REACTIVATED : OrganizationChange.ACTION_DEACTIVATED,
                List.of(new FieldChange("active", before, organization.getIsActive())), sysUserId, actor);
        VectorSamplingSite site = samplingSiteService.getByOrganizationId(numericId);
        if (site != null && !Objects.equals(site.getActive(), active)) {
            site.setActive(active);
            site.setSysUserId(sysUserId);
            samplingSiteService.update(site);
        }
        changed.add(organization.getId());
        names.add(organization.getOrganizationName());
    }

    // ---------------------------------------------------------------- wards

    @Override
    @Transactional(readOnly = true)
    public List<Ward> wards(String organizationId, boolean includeInactive) {
        Organization parent = require(organizationId);
        List<Organization> wards = organizationService.getChildrenWithTypes(organizationId).stream()
                .filter(child -> LocationsApi.KIND_WARD.equals(kindOf(child)))
                .filter(child -> includeInactive || active(child)).collect(Collectors.toList());
        Map<String, Usage> usage = usageFor(wards);
        return wards.stream().map(ward -> ward(ward, parent, usage.get(ward.getId()))).collect(Collectors.toList());
    }

    private static Ward ward(Organization ward, Organization parent, Usage usage) {
        boolean inherited = ward.getGpsLatitude() == null && ward.getGpsLongitude() == null;
        BigDecimal latitude = inherited ? parent.getGpsLatitude() : ward.getGpsLatitude();
        BigDecimal longitude = inherited ? parent.getGpsLongitude() : ward.getGpsLongitude();
        return new Ward(ward.getId(), ward.getOrganizationName(), ward.getCode(), ward.getServiceType(),
                ward.getContactName(), ward.getPhone(), ward.getEmail(), latitude, longitude,
                inherited && (latitude != null || longitude != null), usage == null ? new Usage(0, 0) : usage,
                active(ward), millis(ward));
    }

    @Override
    @Transactional
    public Ward saveWard(String organizationId, WardRequest request, String sysUserId) {
        return saveWard(organizationId, request, sysUserId, null);
    }

    @Override
    @Transactional
    public Ward saveWard(String organizationId, WardRequest request, String sysUserId, String actor) {
        Organization parent = require(organizationId);
        if (!LocationsApi.KIND_FACILITY.equals(kindOf(parent))) {
            throw new LocationsValidationException("parent", "A ward's parent must be a facility");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        if (GenericValidator.isBlankOrNull(request.name())) {
            errors.put("name", "Name is required");
        }
        boolean isNew = GenericValidator.isBlankOrNull(request.id());
        WardServiceType serviceType = WardServiceType.parse(request.serviceType());
        if (serviceType == null && (isNew || !GenericValidator.isBlankOrNull(request.serviceType()))) {
            errors.put("serviceType", "Choose a service type");
        }
        if (!GenericValidator.isBlankOrNull(request.email()) && !EMAIL.matcher(request.email().trim()).matches()) {
            errors.put("email", "Enter a valid email address");
        }
        if (request.gpsLatitude() != null && (request.gpsLatitude().compareTo(BigDecimal.valueOf(-90)) < 0
                || request.gpsLatitude().compareTo(BigDecimal.valueOf(90)) > 0)) {
            errors.put("gpsLatitude", "Enter decimal degrees: latitude -90 to 90, longitude -180 to 180");
        }
        if (request.gpsLongitude() != null && (request.gpsLongitude().compareTo(BigDecimal.valueOf(-180)) < 0
                || request.gpsLongitude().compareTo(BigDecimal.valueOf(180)) > 0)) {
            errors.put("gpsLongitude", "Enter decimal degrees: latitude -90 to 90, longitude -180 to 180");
        }
        List<Identifier> identifiers = GenericValidator.isBlankOrNull(request.code()) ? List.of()
                : List.of(new Identifier(null, OrganizationIdentifier.CODE_LABEL, request.code().trim(), true));
        Organization ward = isNew ? new Organization() : require(request.id());
        if (!isNew && realParent(ward) != null && !realParent(ward).getId().equals(organizationId)) {
            throw new LocationsValidationException("parent", "This ward belongs to another organization");
        }
        if (!isNew && request.lastupdated() != null && millis(ward) != null
                && !request.lastupdated().equals(millis(ward))) {
            throw new LocationsConflictException("Another admin saved this ward first", detail(ward));
        }
        checkIdentifierClashes(identifiers, LocationsApi.KIND_WARD, ward.getId(), errors);
        if (!errors.isEmpty()) {
            throw new LocationsValidationException("The ward was not saved", errors);
        }
        Map<String, OrganizationType> types = organizationTypeService.getAllOrganizationTypes().stream()
                .collect(Collectors.toMap(OrganizationType::getId, t -> t, (a, b) -> a));
        Map<String, String> before = snapshot(ward, types);
        ward.setOrganizationName(request.name().trim());
        ward.setCode(trimTo(request.code(), 20));
        ward.setShortName(trimTo(request.code(), 15));
        if (serviceType != null) {
            ward.setServiceType(serviceType.name());
        }
        ward.setContactName(trimTo(request.contactName(), 100));
        ward.setPhone(trimTo(request.phone(), 20));
        ward.setEmail(trimTo(request.email(), 255));
        ward.setGpsLatitude(request.gpsLatitude());
        ward.setGpsLongitude(request.gpsLongitude());
        ward.setOrganization(parent);
        ward.setSysUserId(sysUserId);
        if (isNew) {
            ward.setIsActive("Y");
            ward.setMlsSentinelLabFlag("N");
            ward.setSource(LocationsApi.SOURCE_LOCAL);
            ward.setId(organizationService.insertUnchecked(ward));
        } else {
            ward = organizationService.updateUnchecked(ward);
        }
        linkTypes(ward, resolveTypeIds(LocationsApi.KIND_WARD, null, ward, types));
        Integer numericId = Integer.valueOf(ward.getId());
        identifierService.replaceForOrganization(numericId,
                identifiers.stream()
                        .map(i -> new OrganizationIdentifier(numericId, i.label(), i.value(), i.reporting()))
                        .collect(Collectors.toList()),
                sysUserId);
        Organization reloaded = organizationService.get(ward.getId());
        if (isNew) {
            changeService.record(numericId, OrganizationChange.ACTION_CREATED, List.of(), sysUserId, actor);
        } else {
            List<FieldChange> changes = diff(before, snapshot(reloaded, types));
            if (!changes.isEmpty()) {
                changeService.record(numericId, OrganizationChange.ACTION_EDITED, changes, sysUserId, actor);
            }
        }
        refreshDisplayLists();
        return ward(reloaded, parent, usageFor(List.of(reloaded)).get(reloaded.getId()));
    }

    @Override
    @Transactional
    public Ward moveWard(String wardId, String newParentId, String sysUserId) {
        Organization ward = require(wardId);
        if (!LocationsApi.KIND_WARD.equals(kindOf(ward))) {
            throw new LocationsValidationException("parent", "Only a ward or department can be moved");
        }
        Organization target = require(newParentId);
        if (!LocationsApi.KIND_FACILITY.equals(kindOf(target))) {
            throw new LocationsValidationException("parent", "A ward's parent must be a facility");
        }
        Organization from = realParent(ward);
        ward.setOrganization(target);
        ward.setSysUserId(sysUserId);
        Organization moved = organizationService.updateUnchecked(ward);
        changeService.record(
                Integer.valueOf(wardId), OrganizationChange.ACTION_MOVED, List.of(new FieldChange("organization",
                        from == null ? null : from.getOrganizationName(), target.getOrganizationName())),
                sysUserId, null);
        refreshDisplayLists();
        return ward(moved, target, usageFor(List.of(moved)).get(moved.getId()));
    }

    // ---------------------------------------------------------------- history

    @Override
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(String id) {
        require(id);
        Map<Integer, String> users = new HashMap<>();
        List<HistoryEntry> entries = new ArrayList<>();
        for (OrganizationChange change : changeService.getForOrganization(Integer.valueOf(id))) {
            String who = change.getActor();
            if (GenericValidator.isBlankOrNull(who) && change.getSystemUserId() != null) {
                who = users.computeIfAbsent(change.getSystemUserId(), this::userName);
            }
            Timestamp at = change.getChangedAt();
            entries.add(new HistoryEntry(change.getId(), at == null ? null : at.toLocalDateTime().format(WHEN), who,
                    change.getAction(), changeService.changesOf(change)));
        }
        return entries;
    }

    private String userName(Integer systemUserId) {
        try {
            SystemUser user = systemUserService.getUserById(String.valueOf(systemUserId));
            if (user == null) {
                return String.valueOf(systemUserId);
            }
            String display = user.getDisplayName();
            return GenericValidator.isBlankOrNull(display) || ",".equals(display.trim()) ? user.getLoginName()
                    : display;
        } catch (RuntimeException e) {
            return String.valueOf(systemUserId);
        }
    }

    // ---------------------------------------------------------------- lists

    @Override
    @Transactional(readOnly = true)
    public Lists lists() {
        List<ListRef> serviceTypes = new ArrayList<>();
        for (WardServiceType type : WardServiceType.values()) {
            serviceTypes.add(new ListRef(type.name(), type.getLabel()));
        }
        List<ListRef> referralStatuses = LocationsApi.REFERRAL_STATUSES.stream().map(s -> new ListRef(s, s))
                .collect(Collectors.toList());
        List<TypeRef> facilityTypes = organizationTypeService.getAllOrganizationTypes().stream()
                .filter(type -> !isLevelType(type) && !DEPT_TYPE.equalsIgnoreCase(type.getName())
                        && !SITE_TYPE.equalsIgnoreCase(type.getName()))
                .map(type -> new TypeRef(type.getId(), type.getName(), type.getHierarchyLevel()))
                .sorted(Comparator.comparing(TypeRef::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        return new Lists(refs(entriesOf(CATEGORY_LIST)), refs(entriesOf(OWNERSHIP_LIST)), serviceTypes,
                entryRefs(entriesOf(SITE_TYPE_LIST)), entryRefs(entriesOf(ZONE_LIST)), referralStatuses,
                identifierService.getDistinctLabels(), facilityTypes, areaLevels());
    }

    private static List<ListRef> refs(List<Dictionary> entries) {
        return entries.stream().map(entry -> new ListRef(entry.getId(), entry.getDictEntry()))
                .collect(Collectors.toList());
    }

    /**
     * Site types and zones are stored on the site by their text, so the text is the
     * id.
     */
    private static List<ListRef> entryRefs(List<Dictionary> entries) {
        return entries.stream().map(entry -> new ListRef(entry.getDictEntry(), entry.getDictEntry()))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- areas

    @Override
    @Transactional(readOnly = true)
    public List<AreaLevel> areaLevels() {
        return organizationTypeService.getAllOrganizationTypes().stream().filter(LocationsServiceImpl::isLevelType)
                .sorted(Comparator.comparing(OrganizationType::getHierarchyLevel))
                .map(type -> new AreaLevel(type.getHierarchyLevel(), type.getName(), type.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Area> areaChildren(String parentId, String status) {
        List<Organization> areas;
        if (GenericValidator.isBlankOrNull(parentId)) {
            List<AreaLevel> levels = areaLevels();
            areas = levels.isEmpty() ? List.of()
                    : organizationService.getByTypeIdWithTypes(levels.get(0).typeId()).stream()
                            .filter(area -> realParent(area) == null
                                    || !LocationsApi.KIND_AREA.equals(kindOf(realParent(area))))
                            .collect(Collectors.toList());
        } else {
            areas = organizationService.getChildrenWithTypes(parentId).stream()
                    .filter(child -> LocationsApi.KIND_AREA.equals(kindOf(child))).collect(Collectors.toList());
        }
        String wanted = status == null ? LocationsApi.STATUS_ACTIVE : status;
        areas = areas.stream().filter(area -> statusMatches(wanted, area))
                .sorted(Comparator.comparing(area -> norm(area.getOrganizationName()))).collect(Collectors.toList());
        return areaRows(areas, Set.of());
    }

    private List<Area> areaRows(List<Organization> areas, Set<String> matched) {
        Map<String, Long> childCounts = organizationService
                .countActiveChildren(areas.stream().map(Organization::getId).collect(Collectors.toList()));
        List<Area> rows = new ArrayList<>();
        for (Organization area : areas) {
            Organization parent = realParent(area);
            rows.add(new Area(area.getId(), area.getOrganizationName(), area.getCode(), level(area), levelName(area),
                    parent == null ? null : parent.getId(), active(area),
                    childCounts.getOrDefault(area.getId(), 0L).intValue(), matched.contains(area.getId()),
                    millis(area)));
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Area> searchAreas(String text, String status) {
        String wanted = status == null ? LocationsApi.STATUS_ACTIVE : status;
        String needle = norm(text);
        if (needle.isEmpty()) {
            return List.of();
        }
        Map<String, Organization> hits = new LinkedHashMap<>();
        for (Organization area : organizationService.searchAreas(needle, 200)) {
            hits.put(area.getId(), area);
        }
        Map<Integer, List<String>> formerNames = changeService.formerNames();
        for (Map.Entry<Integer, List<String>> entry : formerNames.entrySet()) {
            if (entry.getValue().stream().anyMatch(former -> contains(former, needle))) {
                Organization area = organizationService.getOrganizationById(String.valueOf(entry.getKey()));
                if (area != null && LocationsApi.KIND_AREA.equals(kindOf(area))) {
                    hits.putIfAbsent(area.getId(), area);
                }
            }
        }
        Map<String, Organization> shown = new LinkedHashMap<>();
        Set<String> matched = new HashSet<>();
        for (Organization hit : hits.values()) {
            if (!statusMatches(wanted, hit)) {
                continue;
            }
            matched.add(hit.getId());
            for (Organization ancestor : ancestors(hit)) {
                shown.putIfAbsent(ancestor.getId(), ancestor);
            }
            shown.putIfAbsent(hit.getId(), hit);
        }
        return areaRows(new ArrayList<>(shown.values()), matched);
    }

    @Override
    @Transactional
    public Area saveArea(AreaRequest request, String sysUserId) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (GenericValidator.isBlankOrNull(request.name())) {
            errors.put("name", "Name is required");
        }
        boolean isNew = GenericValidator.isBlankOrNull(request.id());
        Organization area = isNew ? new Organization() : require(request.id());
        if (!isNew && !LocationsApi.KIND_AREA.equals(kindOf(area))) {
            throw new LocationsValidationException("id", "This record is not a geographic area");
        }
        if (!isNew && request.lastupdated() != null && millis(area) != null
                && !request.lastupdated().equals(millis(area))) {
            throw new LocationsConflictException("Another admin saved this area first", null);
        }
        List<AreaLevel> levels = areaLevels();
        if (levels.isEmpty()) {
            throw new LocationsValidationException("parent",
                    "No geographic levels are configured; load the levels file first");
        }
        Organization parent = isNew ? (GenericValidator.isBlankOrNull(request.parentId()) ? null
                : organizationService.getOrganizationById(request.parentId())) : realParent(area);
        if (isNew && !GenericValidator.isBlankOrNull(request.parentId()) && parent == null) {
            errors.put("parent", "The parent area no longer exists");
        }
        int level = parent == null ? 1 : (level(parent) == null ? 0 : level(parent) + 1);
        if (isNew && parent != null && (level(parent) == null || !LocationsApi.KIND_AREA.equals(kindOf(parent)))) {
            errors.put("parent", "The parent must be a geographic area");
        }
        AreaLevel levelType = levels.stream().filter(l -> l.level() == level).findFirst().orElse(null);
        if (isNew && levelType == null) {
            errors.put("parent", "There is no level below " + (parent == null ? "the top" : levelName(parent)));
        }
        if (!errors.isEmpty()) {
            throw new LocationsValidationException("The area was not saved", errors);
        }
        Map<String, OrganizationType> types = organizationTypeService.getAllOrganizationTypes().stream()
                .collect(Collectors.toMap(OrganizationType::getId, t -> t, (a, b) -> a));
        Map<String, String> before = snapshot(area, types);
        area.setOrganizationName(request.name().trim());
        area.setCode(trimTo(request.code(), 20));
        area.setShortName(trimTo(request.code() == null ? request.name() : request.code(), 15));
        area.setSysUserId(sysUserId);
        if (isNew) {
            area.setOrganization(parent);
            area.setIsActive("Y");
            area.setMlsSentinelLabFlag("N");
            area.setSource(LocationsApi.SOURCE_LOCAL);
            area.setId(organizationService.insertUnchecked(area));
            linkTypes(area, List.of(levelType.typeId()));
        } else {
            area = organizationService.updateUnchecked(area);
        }
        Integer numericId = Integer.valueOf(area.getId());
        identifierService.replaceForOrganization(numericId, GenericValidator.isBlankOrNull(area.getCode()) ? List.of()
                : List.of(
                        new OrganizationIdentifier(numericId, OrganizationIdentifier.CODE_LABEL, area.getCode(), true)),
                sysUserId);
        Organization reloaded = organizationService.get(area.getId());
        if (isNew) {
            changeService.record(numericId, OrganizationChange.ACTION_CREATED, List.of(), sysUserId, null);
        } else {
            List<FieldChange> changes = diff(before, snapshot(reloaded, types));
            if (!changes.isEmpty()) {
                changeService.record(numericId, OrganizationChange.ACTION_EDITED, changes, sysUserId, null);
            }
        }
        refreshDisplayLists();
        return areaRows(List.of(reloaded), Set.of()).get(0);
    }

    @Override
    @Transactional
    public Area setAreaActive(String id, boolean active, String sysUserId) {
        Organization area = require(id);
        if (!LocationsApi.KIND_AREA.equals(kindOf(area))) {
            throw new LocationsValidationException("id", "This record is not a geographic area");
        }
        setActive(new ActiveRequest(List.of(id), active, false), sysUserId);
        return areaRows(List.of(organizationService.get(id)), Set.of()).get(0);
    }
}
