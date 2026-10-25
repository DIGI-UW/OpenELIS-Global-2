package org.openelisglobal.reportqueue.service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IReportTrackingService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ReportTrackingService.ReportType;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reportqueue.dao.ReleasedNumericResultRecord;
import org.openelisglobal.reportqueue.dao.ReportQueueCriteria;
import org.openelisglobal.reportqueue.dao.ReportQueuePreferenceDAO;
import org.openelisglobal.reportqueue.dao.ReportQueueQueryDAO;
import org.openelisglobal.reportqueue.dao.ReportQueueRowRecord;
import org.openelisglobal.reportqueue.dao.ReportVersionDAO;
import org.openelisglobal.reportqueue.dto.ReportQueuePageDTO;
import org.openelisglobal.reportqueue.dto.ReportQueuePreferenceDTO;
import org.openelisglobal.reportqueue.dto.ReportQueueRequest;
import org.openelisglobal.reportqueue.dto.ReportQueueRowDTO;
import org.openelisglobal.reportqueue.dto.ReportQueueRowDetailDTO;
import org.openelisglobal.reportqueue.valueholder.ReportPrintEvent;
import org.openelisglobal.reportqueue.valueholder.ReportQueuePreference;
import org.openelisglobal.reportqueue.valueholder.ReportVersion;
import org.openelisglobal.reportqueue.valueholder.ReportVersionItem;
import org.openelisglobal.reports.action.implementation.PatientCILNSPClinical_vreduit;
import org.openelisglobal.reports.action.implementation.ResultAlertFlags;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.systemuser.controller.UnifiedSystemUserController;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportQueueServiceImpl implements ReportQueueService {

    public static final String INSTALL_DATE_SITE_INFO = "reportPrintQueueInstallDate";
    private static final DateTimeFormatter INSTALL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<Integer> PAGE_SIZES = List.of(10, 20, 50, 100);
    private static final List<String> WINDOWS = List.of("24h", "7d", "30d", "all", "custom");

    @Autowired
    private ReportQueueQueryDAO queryDAO;
    @Autowired
    private ReportVersionDAO versionDAO;
    @Autowired
    private ReportQueuePreferenceDAO preferenceDAO;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleDAO sampleDAO;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private ResultLimitService resultLimitService;
    @Autowired
    private TestResultComponentService testResultComponentService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private SiteInformationService siteInformationService;
    @Autowired
    private IReportTrackingService reportTrackingService;

    /** One analysis of an order as the queue sees it now. */
    static final class SnapshotItem {
        Long analysisId;
        String testName;
        String rawValue;
        String display;
        String flag;
        String unitId;
        String unitName;
        boolean released;
        String statusId;
        Timestamp releasedAt;
        Analysis analysis;
    }

    /**
     * The released content of an order right now, compared against what was last
     * printed.
     */
    static final class Snapshot {
        final List<SnapshotItem> items = new ArrayList<>();
        boolean anyCorrected;
        Timestamp latestRelease;

        List<SnapshotItem> released() {
            List<SnapshotItem> released = new ArrayList<>();
            for (SnapshotItem item : items) {
                if (item.released) {
                    released.add(item);
                }
            }
            return released;
        }
    }

    /**
     * What differs between the last printed version and the current content. A
     * printed result back awaiting validation holds the order: nothing differs
     * until the correction is validated, so no version drops it (OGC-1529).
     */
    static final class Comparison {
        final List<SnapshotItem> added = new ArrayList<>();
        final List<ReportVersionItem> removed = new ArrayList<>();
        final Map<ReportVersionItem, SnapshotItem> changed = new LinkedHashMap<>();
        final List<ReportVersionItem> awaitingValidation = new ArrayList<>();
        boolean legacyNewer;

        boolean changedOrRemoved() {
            return awaitingValidation.isEmpty() && (!changed.isEmpty() || !removed.isEmpty());
        }

        boolean contentDiffers() {
            return legacyNewer || (awaitingValidation.isEmpty() && (!added.isEmpty() || changedOrRemoved()));
        }
    }

    // ----- list

    @Override
    @Transactional(readOnly = true)
    public ReportQueuePageDTO list(ReportQueueRequest request, String sysUserId) {
        ReportQueuePreference preference = preferenceFor(sysUserId);
        String window = request.getWindow() == null || !WINDOWS.contains(request.getWindow())
                ? preference.getTimeWindow()
                : request.getWindow();
        int pageSize = request.getPageSize() == null || !PAGE_SIZES.contains(request.getPageSize())
                ? preference.getPageSize()
                : request.getPageSize();

        ReportQueueCriteria criteria = baseCriteria(sysUserId);
        criteria.setLabNo(request.getLabNo());
        criteria.setLabNoTo(request.getLabNoTo());
        criteria.setPatientId(request.getPatientId());
        criteria.setStates(request.getStates());
        criteria.setPrintStatus(request.getPrintStatus());
        applyWindow(criteria, window, request.getFrom(), request.getTo());
        criteria.setFacilityIds(request.getFacilityIds());
        criteria.setWardIds(request.getWardIds());
        criteria.setRequesterIds(request.getRequesterIds());
        criteria.setSortKey(sortKey(request.getSort()));
        criteria.setSortDescending(!"asc".equalsIgnoreCase(request.getSortDir()));
        criteria.setPage(Math.max(1, request.getPage()));
        criteria.setPageSize(pageSize);

        ReportQueuePageDTO page = new ReportQueuePageDTO();
        page.setPage(criteria.getPage());
        page.setPageSize(pageSize);
        page.setTimeWindow(window);
        page.setLookup(criteria.isLookup());
        List<ReportQueueRowRecord> records = queryDAO.list(criteria);
        Set<Long> critical = criticalSampleIds(records);
        for (ReportQueueRowRecord record : records) {
            page.getRows().add(toRow(record, critical.contains(record.getSampleId())));
        }
        page.setTotal(queryDAO.count(criteria));
        long[] tiles = queryDAO.tiles(criteria);
        page.setToPrint(tiles[0]);
        page.setAmendedToPrint(tiles[1]);

        if (page.getRows().isEmpty() && !GenericValidator.isBlankOrNull(request.getLabNo())
                && GenericValidator.isBlankOrNull(request.getLabNoTo())) {
            Sample sample = sampleService.getSampleByAccessionNumber(request.getLabNo().trim());
            if (sample != null && sample.getId() != null
                    && queryDAO.isInScope(Long.valueOf(sample.getId()), criteria)) {
                Long sampleId = Long.valueOf(sample.getId());
                ReportQueueRowDTO row = new ReportQueueRowDTO();
                row.setSampleId(sampleId);
                row.setLabNumber(sample.getAccessionNumber());
                row.setNothingReleased(true);
                row.setState("PARTIAL");
                Optional<ReportVersion> printed = versionDAO.findLatest(sampleId, ReportVersion.TYPE_PATIENT);
                Snapshot current = printed.isPresent() ? snapshot(sample) : null;
                if (current != null && !compare(printed.get(), current).awaitingValidation.isEmpty()) {
                    row.setNothingReleased(false);
                    row.setAwaitingValidation(true);
                    row.setState(printed.get().isAmended() ? "AMENDED" : "PARTIAL");
                    row.setReleasedCount(current.released().size());
                    row.setTotalCount(current.items.size());
                }
                row.setPrintStatus(printed.isPresent() ? "PRINTED" : "UNPRINTED");
                row.setVersion(printed.map(ReportVersion::getVersion).orElse(null));
                row.setLegacy(printed.map(ReportVersion::isLegacy).orElse(false));
                Patient patient = sampleHumanService.getPatientForSample(sample);
                if (patient != null) {
                    row.setPatientName(patientName(patient));
                    row.setPatientCode(patientCode(patient));
                }
                queryDAO.findParties(sampleId, criteria).ifPresent(parties -> {
                    row.setFacility(parties.getFacilityName());
                    row.setWard(parties.getWardName());
                    row.setRequester(parties.getRequesterName());
                });
                page.getRows().add(row);
                page.setTotal(page.getRows().size());
            }
        }
        return page;
    }

    private ReportQueueCriteria baseCriteria(String sysUserId) {
        ReportQueueCriteria criteria = new ReportQueueCriteria();
        criteria.setFinalizedStatusId(statusService.getStatusID(AnalysisStatus.Finalized));
        criteria.setExcludedStatusIds(excludedStatusIds());
        criteria.setSectionIds(sectionScope(sysUserId));
        criteria.setOrganizationRequesterTypeId(TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID);
        criteria.setProviderRequesterTypeId(TableIdService.getInstance().PROVIDER_REQUESTER_TYPE_ID);
        criteria.setInstallDate(installDate());
        return criteria;
    }

    private List<String> excludedStatusIds() {
        return List.of(statusService.getStatusID(AnalysisStatus.Canceled),
                statusService.getStatusID(AnalysisStatus.SampleRejected));
    }

    private static ReportQueueCriteria.SortKey sortKey(String sort) {
        if (sort == null) {
            return ReportQueueCriteria.SortKey.RELEASED;
        }
        switch (sort) {
        case "labNumber":
            return ReportQueueCriteria.SortKey.LAB_NO;
        case "subject":
            return ReportQueueCriteria.SortKey.SUBJECT;
        case "state":
            return ReportQueueCriteria.SortKey.STATE;
        case "printStatus":
            return ReportQueueCriteria.SortKey.PRINT_STATUS;
        default:
            return ReportQueueCriteria.SortKey.RELEASED;
        }
    }

    private static void applyWindow(ReportQueueCriteria criteria, String window, String from, String to) {
        OffsetDateTime now = OffsetDateTime.now();
        switch (window) {
        case "24h":
            criteria.setWindowFrom(now.minusHours(24));
            break;
        case "7d":
            criteria.setWindowFrom(now.minusDays(7));
            break;
        case "30d":
            criteria.setWindowFrom(now.minusDays(30));
            break;
        case "custom":
            criteria.setWindowFrom(startOfDay(from));
            OffsetDateTime end = startOfDay(to);
            criteria.setWindowTo(end == null ? null : end.plusDays(1));
            break;
        default:
            break;
        }
    }

    private static OffsetDateTime startOfDay(String isoDate) {
        if (GenericValidator.isBlankOrNull(isoDate)) {
            return null;
        }
        try {
            return LocalDate.parse(isoDate.trim()).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private OffsetDateTime installDate() {
        SiteInformation info = siteInformationService.getSiteInformationByName(INSTALL_DATE_SITE_INFO);
        if (info == null || GenericValidator.isBlankOrNull(info.getValue())) {
            return null;
        }
        try {
            return LocalDateTime.parse(info.getValue().trim(), INSTALL_DATE_FORMAT).atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
        } catch (DateTimeParseException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "installDate",
                    "unreadable " + INSTALL_DATE_SITE_INFO + ": " + info.getValue());
            return null;
        }
    }

    /**
     * Null when the user may see the whole lab; otherwise the test sections they
     * hold, with child sections included, as the home dashboard scopes its tiles.
     */
    private List<String> sectionScope(String sysUserId) {
        if (isGlobalScopeUser(sysUserId)) {
            return null;
        }
        List<IdValuePair> userSections = userService.getUserTestSections(sysUserId, null);
        Set<String> ids = new HashSet<>();
        if (userSections != null) {
            for (IdValuePair pair : userSections) {
                ids.add(pair.getId());
            }
        }
        if (!ids.isEmpty()) {
            for (TestSection section : testSectionService.getAllActiveTestSections()) {
                TestSection parent = section.getParentTestSection();
                if (parent != null && ids.contains(parent.getId())) {
                    ids.add(section.getId());
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private boolean isGlobalScopeUser(String sysUserId) {
        if (sysUserId == null) {
            return false;
        }
        if (userRoleService.userInRole(sysUserId, Constants.ROLE_GLOBAL_ADMIN)) {
            return true;
        }
        UserLabUnitRoles labUnitRoles = userService.getUserLabUnitRoles(sysUserId);
        if (labUnitRoles == null || labUnitRoles.getLabUnitRoleMap() == null) {
            return false;
        }
        return labUnitRoles.getLabUnitRoleMap().stream()
                .anyMatch(roleMap -> UnifiedSystemUserController.ALL_LAB_UNITS.equals(roleMap.getLabUnit()));
    }

    private ReportQueueRowDTO toRow(ReportQueueRowRecord record, boolean critical) {
        ReportQueueRowDTO row = new ReportQueueRowDTO();
        row.setSampleId(record.getSampleId());
        row.setLabNumber(record.getAccessionNumber());
        row.setPatientName(joinName(record.getPatientLastName(), record.getPatientFirstName()));
        row.setPatientCode(GenericValidator.isBlankOrNull(record.getPatientNationalId()) ? record.getPatientId()
                : record.getPatientNationalId());
        row.setFacility(record.getFacilityName());
        row.setWard(record.getWardName());
        row.setRequester(record.getRequesterName());
        row.setReleased(record.getLatestRelease() == null ? null : record.getLatestRelease().toString());
        row.setReleasedCount(record.getReleasedCount());
        row.setTotalCount(record.getTotalCount());
        row.setState(record.getState());
        row.setPrintStatus(record.isUnprinted() ? "UNPRINTED" : "PRINTED");
        row.setVersion(record.getVersionNumber());
        row.setLegacy(record.isLegacy());
        row.setCritical(critical);
        row.setAwaitingValidation(record.isAwaitingValidation());
        return row;
    }

    /**
     * The page's orders with a released numeric result beyond a critical bound,
     * judged as the report flags BB and EE (the first result of each analysis
     * against its test's, or its component's, critical range for the patient), from
     * one query for the page instead of a report snapshot per row (FRS OGC-1031
     * NFR-1).
     */
    private Set<Long> criticalSampleIds(List<ReportQueueRowRecord> records) {
        Set<Long> critical = new HashSet<>();
        List<Long> sampleIds = new ArrayList<>();
        for (ReportQueueRowRecord record : records) {
            sampleIds.add(record.getSampleId());
        }
        if (sampleIds.isEmpty()) {
            return critical;
        }
        Map<String, Patient> patients = new HashMap<>();
        Map<String, List<TestResultComponent>> components = new HashMap<>();
        Map<String, ResultLimit> limits = new HashMap<>();
        String finalized = statusService.getStatusID(AnalysisStatus.Finalized);
        for (ReleasedNumericResultRecord result : queryDAO.releasedNumericResults(sampleIds, finalized)) {
            if (critical.contains(result.getSampleId())) {
                continue;
            }
            try {
                Patient patient = result.getPatientId() == null ? null
                        : patients.computeIfAbsent(result.getPatientId(), patientService::getData);
                String scope = componentScope(result, components);
                String key = result.getTestId() + "/" + scope + "/" + result.getSampleTypeId() + "/"
                        + result.getPatientId();
                ResultLimit limit = limits.computeIfAbsent(key,
                        k -> scope == null
                                ? resultLimitService.getResultLimitForTestAndPatient(result.getTestId(), patient,
                                        result.getSampleTypeId())
                                : resultLimitService.getResultLimitForComponentAndPatient(scope, patient,
                                        result.getSampleTypeId()));
                String letter = ResultAlertFlags.criticalLetter(limit,
                        StringUtil.getActualNumericValue(result.getValue()));
                if (!GenericValidator.isBlankOrNull(letter)) {
                    critical.add(result.getSampleId());
                }
            } catch (RuntimeException e) {
                LogEvent.logWarn(getClass().getSimpleName(), "criticalSampleIds",
                        "no critical flag for analysis " + result.getAnalysisId() + ": " + e.getMessage());
            }
        }
        return critical;
    }

    /**
     * The component whose range the result is judged against on a test with several
     * components, as the result limit lookup resolves it; null for a test-level
     * range.
     */
    private String componentScope(ReleasedNumericResultRecord result, Map<String, List<TestResultComponent>> cache) {
        List<TestResultComponent> list = cache.computeIfAbsent(result.getTestId(),
                testResultComponentService::getActiveComponentsByTestId);
        if (list == null || list.size() < 2) {
            return null;
        }
        for (TestResultComponent component : list) {
            if (component.getId().equals(result.getComponentId())) {
                return component.getId();
            }
        }
        for (TestResultComponent component : list) {
            if (component.getIsPrimary()) {
                return component.getId();
            }
        }
        return list.get(0).getId();
    }

    // ----- detail

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportQueueRowDetailDTO> detail(Long sampleId, String sysUserId) {
        Sample sample = findSample(String.valueOf(sampleId));
        if (sample == null || !queryDAO.isInScope(sampleId, baseCriteria(sysUserId))) {
            return Optional.empty();
        }
        ReportQueueRowDetailDTO detail = new ReportQueueRowDetailDTO();
        detail.setSampleId(sampleId);
        detail.setLabNumber(sample.getAccessionNumber());
        detail.setCollected(iso(sample.getCollectionDate()));
        detail.setReceived(iso(sample.getReceivedTimestamp()));
        OffsetDateTime installDate = installDate();
        detail.setInstallDate(installDate == null ? null : installDate.toString());
        ReportQueueCriteria criteria = baseCriteria(sysUserId);
        queryDAO.findRow(sampleId, criteria).or(() -> queryDAO.findParties(sampleId, criteria)).ifPresent(record -> {
            detail.setFacility(record.getFacilityName());
            detail.setWard(record.getWardName());
            detail.setRequester(record.getRequesterName());
        });

        Snapshot snapshot = snapshot(sample);
        for (SnapshotItem item : snapshot.items) {
            ReportQueueRowDetailDTO.TestRowDTO test = new ReportQueueRowDetailDTO.TestRowDTO();
            test.setAnalysisId(item.analysisId);
            test.setTestName(item.testName);
            test.setReleased(item.released);
            test.setResult(item.released ? withUnit(item.display, item.unitName) : null);
            test.setFlag(item.released ? item.flag : null);
            test.setStatus(statusName(item.statusId));
            test.setReleasedAt(iso(item.releasedAt));
            test.setValidatedBy(item.released ? validatedBy(item.analysis) : null);
            detail.getTests().add(test);
        }

        for (ReportVersion version : versionDAO.findAllForSample(sampleId, ReportVersion.TYPE_PATIENT)) {
            ReportQueueRowDetailDTO.VersionDTO dto = new ReportQueueRowDetailDTO.VersionDTO();
            dto.setId(version.getId());
            dto.setVersion(version.getVersion());
            dto.setIssuedAt(version.getIssuedAt() == null ? null : version.getIssuedAt().toString());
            dto.setIssuedBy(userName(version.getIssuedByUserId()));
            dto.setAmended(version.isAmended());
            dto.setChangeSummary(version.getChangeSummary());
            dto.setReleasedCount(version.getReleasedCount());
            dto.setTotalCount(version.getTotalCount());
            dto.setSha256(version.getSha256Hash());
            dto.setArchived(version.getPdfContent() != null);
            dto.setLegacy(version.isLegacy());
            dto.setArchiveFailed(version.isArchiveFailed());
            for (ReportPrintEvent event : version.getPrintEvents()) {
                if (!event.isReprint()) {
                    continue;
                }
                ReportQueueRowDetailDTO.PrintEventDTO reprint = new ReportQueueRowDetailDTO.PrintEventDTO();
                reprint.setPrintedAt(event.getPrintedAt() == null ? null : event.getPrintedAt().toString());
                reprint.setPrintedBy(userName(event.getPrintedByUserId()));
                dto.getReprints().add(reprint);
            }
            detail.getVersions().add(dto);
        }
        return Optional.of(detail);
    }

    private String statusName(String statusId) {
        if (statusService.matches(statusId, AnalysisStatus.Finalized)) {
            return "FINALIZED";
        }
        if (statusService.matches(statusId, AnalysisStatus.TechnicalAcceptance)) {
            return "AWAITING_VALIDATION";
        }
        return "IN_PROGRESS";
    }

    /**
     * Who released the analysis. Validation stamps the release time on the analysis
     * and records the validating user only in its audit history, so the validator
     * is the author of the first history entry written at or after the release: the
     * same reading the patient report prints under "validated and released by".
     */
    private String validatedBy(Analysis analysis) {
        if (analysis.getReleasedDate() == null) {
            return null;
        }
        History release = null;
        for (History entry : historyService.getHistoryByRefIdAndRefTableId(analysis.getId(),
                AnalysisServiceImpl.getTableReferenceId())) {
            if (entry.getTimestamp() != null && !entry.getTimestamp().before(analysis.getReleasedDate())
                    && (release == null || entry.getTimestamp().before(release.getTimestamp()))) {
                release = entry;
            }
        }
        if (release == null || GenericValidator.isBlankOrNull(release.getSysUserId())) {
            return null;
        }
        return userName(release.getSysUserId());
    }

    private String userName(String userId) {
        if (GenericValidator.isBlankOrNull(userId)) {
            return null;
        }
        try {
            SystemUser user = systemUserService.getUserById(userId);
            return user == null ? userId : user.getNameForDisplay();
        } catch (RuntimeException e) {
            return userId;
        }
    }

    // ----- print

    @Override
    public Optional<PrintOutcome> print(Long sampleId, String sysUserId) {
        Sample sample = findSample(String.valueOf(sampleId));
        if (sample == null || !queryDAO.isInScope(sampleId, baseCriteria(sysUserId))) {
            return Optional.empty();
        }
        return Optional.of(printOrder(sample, sysUserId));
    }

    /**
     * An order with nothing released still gets its page: the plain preliminary
     * report, archived as nothing, the same as a single-order print of it.
     */
    @Override
    public byte[] printPatientReports(List<String> sampleIds, String sysUserId) {
        List<byte[]> pdfs = new ArrayList<>();
        for (String sampleId : sampleIds) {
            Sample sample = findSample(sampleId);
            if (sample == null) {
                continue;
            }
            try {
                pdfs.add(printOrder(sample, sysUserId).getPdf());
            } catch (NothingReleasedException e) {
                LogEvent.logInfo(getClass().getSimpleName(), "printPatientReports",
                        "nothing released on " + e.getMessage() + ", printed the preliminary report");
                pdfs.add(render(sample, sysUserId, null));
            }
        }
        if (pdfs.isEmpty()) {
            throw new NothingReleasedException(String.join(", ", sampleIds));
        }
        try {
            return PdfMerge.concat(pdfs);
        } catch (IOException e) {
            throw new LIMSRuntimeException("Could not join the patient reports", e);
        }
    }

    /**
     * One print of an order at a time: the lock holds until the transaction ends,
     * so a second print of the same order reads the version the first one issued
     * and reprints it instead of colliding on the version number.
     */
    private PrintOutcome printOrder(Sample sample, String sysUserId) {
        Long sampleId = Long.valueOf(sample.getId());
        queryDAO.lockForPrint(sampleId);
        Snapshot current = snapshot(sample);
        List<SnapshotItem> released = current.released();
        Optional<ReportVersion> last = versionDAO.findLatest(sampleId, ReportVersion.TYPE_PATIENT);
        Comparison comparison = compare(last.orElse(null), current);
        if (released.isEmpty() && comparison.awaitingValidation.isEmpty()) {
            throw new NothingReleasedException(sample.getAccessionNumber());
        }
        OffsetDateTime now = OffsetDateTime.now();

        if (last.isPresent() && !comparison.contentDiffers()) {
            ReportVersion version = last.get();
            byte[] pdf = version.getPdfContent();
            if (pdf == null) {
                pdf = render(sample, sysUserId,
                        new PatientCILNSPClinical_vreduit.IssueContext(version.getVersion(), version.isAmended(),
                                version.getChangeSummary(), replacedIssueTime(version),
                                issueTime(version.getIssuedAt())));
            }
            ReportPrintEvent reprint = new ReportPrintEvent();
            reprint.setVersion(version);
            reprint.setPrintedAt(now);
            reprint.setPrintedByUserId(sysUserId);
            reprint.setReprint(true);
            version.getPrintEvents().add(reprint);
            versionDAO.save(version);
            trackLegacy(sample, sysUserId);
            return new PrintOutcome(pdf, version.getVersion(), true, sample.getAccessionNumber());
        }

        int number = last.map(version -> version.getVersion() + 1).orElse(1);
        boolean amendedNow = comparison.changedOrRemoved()
                || (last.isPresent() && last.get().isLegacy() && current.anyCorrected);
        boolean amended = last.isPresent() && (last.get().isAmended() || amendedNow);
        String summary = last.isPresent() ? summary(comparison, last.get(), current) : null;
        String replaced = last.map(this::replacedIssueTime).orElse(null);
        byte[] pdf = render(sample, sysUserId,
                new PatientCILNSPClinical_vreduit.IssueContext(number, amended, summary, replaced, issueTime(now)));

        ReportVersion version = new ReportVersion();
        version.setSampleId(sampleId);
        version.setReportType(ReportVersion.TYPE_PATIENT);
        version.setVersion(number);
        version.setAmended(amended);
        version.setAmendedCount(last.map(ReportVersion::getAmendedCount).orElse(0) + (amendedNow ? 1 : 0));
        version.setReissue(false);
        version.setChangeSummary(summary);
        version.setReleasedCount(released.size());
        version.setTotalCount(current.items.size());
        version.setIssuedByUserId(sysUserId);
        version.setIssuedAt(now);
        version.setPdfContent(pdf);
        version.setSha256Hash(sha256Hex(pdf));
        for (SnapshotItem item : released) {
            ReportVersionItem printed = new ReportVersionItem();
            printed.setVersion(version);
            printed.setAnalysisId(item.analysisId);
            printed.setTestName(item.testName);
            printed.setPrintedValue(item.rawValue);
            printed.setPrintedDisplay(item.display);
            printed.setPrintedFlag(item.flag);
            printed.setPrintedUnit(item.unitId);
            version.getItems().add(printed);
        }
        ReportPrintEvent issue = new ReportPrintEvent();
        issue.setVersion(version);
        issue.setPrintedAt(now);
        issue.setPrintedByUserId(sysUserId);
        issue.setReprint(false);
        version.getPrintEvents().add(issue);
        versionDAO.save(version);
        trackLegacy(sample, sysUserId);
        return new PrintOutcome(pdf, number, false, sample.getAccessionNumber());
    }

    /**
     * Readers of the older print history (document_track) keep seeing every print.
     */
    private void trackLegacy(Sample sample, String sysUserId) {
        try {
            reportTrackingService.addReports(List.of(sample.getId()), ReportType.PATIENT,
                    PatientCILNSPClinical_vreduit.REPORT_NAME, sysUserId);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }

    private String replacedIssueTime(ReportVersion version) {
        return issueTime(version.getIssuedAt());
    }

    private static String issueTime(OffsetDateTime issuedAt) {
        if (issuedAt == null) {
            return null;
        }
        return PatientCILNSPClinical_vreduit.reportTime(Timestamp.from(issuedAt.toInstant()));
    }

    private byte[] render(Sample sample, String sysUserId, PatientCILNSPClinical_vreduit.IssueContext context) {
        PatientCILNSPClinical_vreduit report = new PatientCILNSPClinical_vreduit();
        report.setSystemUserId(sysUserId);
        report.setRequestedReport(PatientCILNSPClinical_vreduit.REPORT_NAME);
        report.setIssueContext(context);
        ReportForm form = new ReportForm();
        List<String> analysisIds = new ArrayList<>();
        for (Analysis analysis : analysisService.getAnalysesBySampleId(sample.getId())) {
            analysisIds.add(analysis.getId());
        }
        form.setAnalysisIds(analysisIds);
        report.initializeReport(form);
        try {
            return report.runReport();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Could not render the patient report for " + sample.getAccessionNumber(), e);
        }
    }

    // ----- content and comparison

    Snapshot snapshot(Sample sample) {
        Snapshot snapshot = new Snapshot();
        String finalized = statusService.getStatusID(AnalysisStatus.Finalized);
        List<String> excluded = excludedStatusIds();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
        if (analyses == null) {
            return snapshot;
        }
        analyses.sort(Comparator.comparing(analysis -> Long.valueOf(analysis.getId())));
        for (Analysis analysis : analyses) {
            String statusId = analysis.getStatusId();
            if (statusId == null || excluded.contains(statusId)) {
                continue;
            }
            if (analysis.isCorrectedSincePatientReport() && finalized.equals(statusId)) {
                snapshot.anyCorrected = true;
            }
            SnapshotItem item = new SnapshotItem();
            item.analysis = analysis;
            item.analysisId = Long.valueOf(analysis.getId());
            item.statusId = statusId;
            Test test = analysis.getTest();
            item.testName = test == null ? "" : TestServiceImpl.getUserLocalizedReportingTestName(test);
            if (test != null && test.getUnitOfMeasure() != null) {
                item.unitId = test.getUnitOfMeasure().getId();
                item.unitName = test.getUnitOfMeasure().getName();
            }
            item.released = finalized.equals(statusId);
            item.releasedAt = analysis.getReleasedDate();
            if (item.released) {
                if (item.releasedAt != null
                        && (snapshot.latestRelease == null || item.releasedAt.after(snapshot.latestRelease))) {
                    snapshot.latestRelease = item.releasedAt;
                }
                List<Result> results = new ArrayList<>(resultService.getResultsByAnalysis(analysis));
                results.sort(Comparator.comparing(result -> Long.valueOf(result.getId())));
                List<String> raw = new ArrayList<>();
                List<String> display = new ArrayList<>();
                for (Result result : results) {
                    raw.add(result.getValue() == null ? "" : result.getValue());
                    display.add(displayValue(result));
                }
                item.rawValue = String.join("|", raw);
                item.display = String.join(", ", display);
                item.flag = results.isEmpty() ? "" : flag(analysis, results.get(0), patient);
            }
            snapshot.items.add(item);
        }
        return snapshot;
    }

    private String displayValue(Result result) {
        String value = result.getValue();
        if (GenericValidator.isBlankOrNull(value)) {
            return "";
        }
        if (ResultType.isDictionaryVariant(result.getResultType())) {
            List<String> names = new ArrayList<>();
            for (String id : value.split(",")) {
                String dictionaryId = id.trim();
                if (dictionaryId.isEmpty()) {
                    continue;
                }
                try {
                    Dictionary entry = dictionaryService.getDictionaryById(dictionaryId);
                    names.add(entry == null ? dictionaryId : entry.getLocalizedName());
                } catch (RuntimeException e) {
                    names.add(dictionaryId);
                }
            }
            return String.join(", ", names);
        }
        return ResultType.NUMERIC.matches(result.getResultType()) ? result.getValue(true) : value;
    }

    /**
     * The same letters the report prints: B/E against the result's own range, BB/EE
     * beyond the critical bound.
     */
    private String flag(Analysis analysis, Result result, Patient patient) {
        try {
            if (ResultType.NUMERIC.matches(result.getResultType())
                    && !GenericValidator.isBlankOrNull(result.getValue())) {
                String flag = "";
                if (result.getMinNormal() != null && result.getMaxNormal() != null
                        && (result.getMinNormal() != 0.0 || result.getMaxNormal() != 0.0)) {
                    double value = Double.parseDouble(result.getValue(true));
                    if (value < result.getMinNormal()) {
                        flag = ResultAlertFlags.BELOW;
                    } else if (value > result.getMaxNormal()) {
                        flag = ResultAlertFlags.ABOVE;
                    }
                }
                ResultLimit limit = resultLimitService.getResultLimitForResult(analysis, result, patient);
                String critical = ResultAlertFlags.criticalLetter(limit, result.getValue(true));
                return GenericValidator.isBlankOrNull(critical) ? flag : critical;
            }
            if (ResultType.isDictionaryVariant(result.getResultType())
                    && resultService.isAbnormalDictionaryResult(result)) {
                return ResultAlertFlags.ABNORMAL;
            }
        } catch (RuntimeException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "flag",
                    "no flag for analysis " + analysis.getId() + ": " + e.getMessage());
        }
        return "";
    }

    Comparison compare(ReportVersion last, Snapshot current) {
        Comparison comparison = new Comparison();
        if (last == null) {
            return comparison;
        }
        if (last.isLegacy()) {
            comparison.legacyNewer = current.latestRelease != null && last.getIssuedAt() != null
                    && current.latestRelease.toInstant().isAfter(last.getIssuedAt().toInstant());
            return comparison;
        }
        Map<Long, SnapshotItem> releasedNow = new HashMap<>();
        for (SnapshotItem item : current.released()) {
            releasedNow.put(item.analysisId, item);
        }
        Set<Long> notReleased = new HashSet<>();
        for (SnapshotItem item : current.items) {
            if (!item.released) {
                notReleased.add(item.analysisId);
            }
        }
        Set<Long> printed = new HashSet<>();
        for (ReportVersionItem item : last.getItems()) {
            printed.add(item.getAnalysisId());
            SnapshotItem now = releasedNow.get(item.getAnalysisId());
            if (now == null && notReleased.contains(item.getAnalysisId())) {
                comparison.awaitingValidation.add(item);
            } else if (now == null) {
                comparison.removed.add(item);
            } else if (!nullToEmpty(now.rawValue).equals(nullToEmpty(item.getPrintedValue()))
                    || !nullToEmpty(now.unitId).equals(nullToEmpty(item.getPrintedUnit()))) {
                comparison.changed.put(item, now);
            }
        }
        for (SnapshotItem item : current.released()) {
            if (!printed.contains(item.analysisId)) {
                comparison.added.add(item);
            }
        }
        return comparison;
    }

    /**
     * The system-written change summary of a new version (FRS FR-4.5): changed
     * results with old and new values, removed results, added results, and the
     * corrected-result note when the change carried one.
     */
    private String summary(Comparison comparison, ReportVersion last, Snapshot current) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<ReportVersionItem, SnapshotItem> change : comparison.changed.entrySet()) {
            ReportVersionItem was = change.getKey();
            SnapshotItem now = change.getValue();
            String unitBefore = nullToEmpty(was.getPrintedUnit()).equals(nullToEmpty(now.unitId)) ? null
                    : unitName(was.getPrintedUnit());
            String line = MessageUtil.getMessage("reportQueue.change.changed", new Object[] { now.testName,
                    withUnit(was.getPrintedDisplay(), unitBefore), withUnit(now.display, now.unitName) });
            String note = correctionNote(now.analysis, last.getIssuedAt());
            parts.add(note == null ? line : line + " (" + note + ")");
        }
        if (!comparison.removed.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (ReportVersionItem item : comparison.removed) {
                names.add(item.getTestName());
            }
            parts.add(MessageUtil.getMessage("reportQueue.change.removed", new Object[] { String.join(", ", names) }));
        }
        if (!comparison.added.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (SnapshotItem item : comparison.added) {
                names.add(item.testName);
            }
            parts.add(MessageUtil.getMessage("reportQueue.change.added", new Object[] { String.join(", ", names) }));
        }
        if (parts.isEmpty() && last.isLegacy()) {
            parts.add(MessageUtil.getMessage(current.anyCorrected ? "reportQueue.change.correctedSinceLegacy"
                    : "reportQueue.change.releasedSinceLegacy"));
        }
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private String correctionNote(Analysis analysis, OffsetDateTime since) {
        if (analysis == null) {
            return null;
        }
        try {
            String latest = null;
            for (Note note : noteService.getNotesChronologicallyByRefIdAndRefTable(analysis.getId(),
                    AnalysisServiceImpl.getTableReferenceId())) {
                if (!NoteType.EXTERNAL.getDBCode().equals(note.getNoteType()) || note.getLastupdated() == null
                        || GenericValidator.isBlankOrNull(note.getText())) {
                    continue;
                }
                if (since == null || note.getLastupdated().toInstant().isAfter(since.toInstant())) {
                    latest = note.getText();
                }
            }
            return latest;
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ----- versions and preferences

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportVersion> version(Long versionId, String sysUserId) {
        return versionDAO.get(versionId)
                .filter(version -> queryDAO.isInScope(version.getSampleId(), baseCriteria(sysUserId)));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportQueuePreferenceDTO preferences(String sysUserId) {
        ReportQueuePreference preference = preferenceFor(sysUserId);
        return new ReportQueuePreferenceDTO(preference.getTimeWindow(), preference.getPageSize());
    }

    @Override
    public ReportQueuePreferenceDTO savePreferences(String sysUserId, ReportQueuePreferenceDTO wanted) {
        ReportQueuePreference preference = preferenceDAO.findByUser(Long.valueOf(sysUserId))
                .orElseGet(ReportQueuePreference::new);
        preference.setSystemUserId(Long.valueOf(sysUserId));
        if (wanted.getTimeWindow() != null && WINDOWS.contains(wanted.getTimeWindow())) {
            preference.setTimeWindow(wanted.getTimeWindow());
        }
        if (wanted.getPageSize() != null && PAGE_SIZES.contains(wanted.getPageSize())) {
            preference.setPageSize(wanted.getPageSize());
        }
        preference.setSysUserId(sysUserId);
        ReportQueuePreference saved = preferenceDAO.save(preference);
        return new ReportQueuePreferenceDTO(saved.getTimeWindow(), saved.getPageSize());
    }

    private ReportQueuePreference preferenceFor(String sysUserId) {
        if (GenericValidator.isBlankOrNull(sysUserId)) {
            return new ReportQueuePreference();
        }
        try {
            return preferenceDAO.findByUser(Long.valueOf(sysUserId)).orElseGet(ReportQueuePreference::new);
        } catch (NumberFormatException e) {
            return new ReportQueuePreference();
        }
    }

    // ----- helpers

    /**
     * The sample behind a queue row, or null when no sample has that id. The
     * service lookup throws on a missing row, which would mark the surrounding
     * transaction for rollback; a print or detail request for an id that never
     * existed is a not-found answer, not a server error.
     */
    private Sample findSample(String sampleId) {
        if (GenericValidator.isBlankOrNull(sampleId) || !sampleId.matches("\\d+")) {
            return null;
        }
        return sampleDAO.get(sampleId).orElse(null);
    }

    private static String patientName(Patient patient) {
        if (patient.getPerson() == null) {
            return null;
        }
        return joinName(patient.getPerson().getLastName(), patient.getPerson().getFirstName());
    }

    private static String patientCode(Patient patient) {
        return GenericValidator.isBlankOrNull(patient.getNationalId()) ? patient.getId() : patient.getNationalId();
    }

    private static String joinName(String last, String first) {
        boolean hasLast = !GenericValidator.isBlankOrNull(last);
        boolean hasFirst = !GenericValidator.isBlankOrNull(first);
        if (hasLast && hasFirst) {
            return last.trim() + ", " + first.trim();
        }
        return hasLast ? last.trim() : hasFirst ? first.trim() : null;
    }

    /**
     * The unit's name for the change summary, when the printed unit is no longer
     * the test's.
     */
    private String unitName(String unitId) {
        if (GenericValidator.isBlankOrNull(unitId)) {
            return null;
        }
        UnitOfMeasure unit = unitOfMeasureService.getUnitOfMeasureById(unitId);
        return unit == null ? null : unit.getName();
    }

    private static String withUnit(String value, String unit) {
        if (GenericValidator.isBlankOrNull(value)) {
            return "";
        }
        return GenericValidator.isBlankOrNull(unit) ? value : value + " " + unit;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String iso(Timestamp timestamp) {
        return timestamp == null ? null
                : timestamp.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
    }

    static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new LIMSRuntimeException("SHA-256 is not available", e);
        }
    }
}
