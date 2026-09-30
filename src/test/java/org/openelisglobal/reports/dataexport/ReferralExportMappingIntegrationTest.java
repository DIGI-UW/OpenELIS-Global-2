package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralType;
import org.openelisglobal.reports.dataexport.dao.ReferralExportDAO;
import org.openelisglobal.reports.dataexport.form.ExportFilter;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ExportSubmission;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.form.SavedReportDefinition;
import org.openelisglobal.reports.dataexport.form.SavedReportFilters;
import org.openelisglobal.reports.dataexport.service.ReportingCatalogService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Transactional
public class ReferralExportMappingIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private ReferralExportDAO referrals;
    @Autowired
    private ReportingCatalogService catalog;
    @Autowired
    @Qualifier("reportingSourceConfigurationHandler")
    private DomainConfigurationHandler configurations;
    @PersistenceContext
    private EntityManager entityManager;
    private String referralTypeId;
    private RequestAttributes previousRequest;

    @After
    public void restoreRequest() {
        RequestContextHolder.setRequestAttributes(previousRequest);
    }

    @Before
    public void fixture() throws Exception {
        executeDataSetWithStateManagement("testdata/reporting-sample-testing.xml");
        previousRequest = RequestContextHolder.getRequestAttributes();
        var request = new MockHttpServletRequest();
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(new UsernamePasswordAuthenticationToken(
                User.withUsername("admin").password("unused").roles("ADMIN").build(), "unused"));
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        request.getSession().setAttribute("userSessionData", new UserSessionData());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var type = new ReferralType();
        type.setName("Reporting mapping fixture");
        entityManager.persist(type);
        referralTypeId = type.getId();
    }

    private Referral referral(String analysis, String requested, String sent) {
        var referral = new Referral();
        referral.setAnalysis(entityManager.find(Analysis.class, analysis));
        referral.setReferralTypeId(referralTypeId);
        referral.setOrganization(entityManager.find(Organization.class, "3"));
        referral.setRequestDate(stamp(requested));
        referral.setSentDate(stamp(sent));
        referral.setStatus(sent == null ? ReferralStatus.DRAFT : ReferralStatus.REQUESTED);
        entityManager.persist(referral);
        return referral;
    }

    private ReferralResult returned(Referral referral, String value, String date) {
        var result = new Result();
        result.setAnalysis(referral.getAnalysis());
        result.setResultType("N");
        result.setValue(value);
        result.setIsReportable("Y");
        entityManager.persist(result);
        var link = new ReferralResult();
        link.setReferralId(referral.getId());
        link.setTestId(referral.getAnalysis().getTest().getId());
        link.setResult(result);
        link.setReferralReportDate(stamp(date));
        entityManager.persist(link);
        return link;
    }

    private static Timestamp stamp(String value) {
        return value == null ? null : Timestamp.from(Instant.parse(value));
    }

    private ExportSnapshot request(String anchor, String from, String to, String zone, List<String> sections,
            List<String> tests) {
        var definition = new ReportSourceConfig("REFERRALS", 1, "Referrals", "REFERRALS", anchor, List.of("TABLE"),
                List.of("referralId"), List.of(), List.of("labSectionIds", "testIds"),
                Map.of("TABLE", List.of("referralId")));
        return new ExportSnapshot(definition, "TABLE", List.of(),
                new ExportFilter(from, to, sections, tests, List.of()), zone, List.of());
    }

    private List<ReferralExportDAO.Row> read(ExportSnapshot request) {
        entityManager.flush();
        entityManager.clear();
        try (var rows = referrals.stream(request)) {
            return rows.toList();
        }
    }

    @Test
    public void requestAndSentDatesAreIndependentAnchorsWithoutNullFallback() {
        var sent = referral("1", "2026-05-05T10:00:00Z", "2026-05-07T11:00:00Z");
        var draft = referral("1", "2026-05-05T12:00:00Z", null);
        assertEquals(List.of(sent.getId(), draft.getId()),
                read(request("requestDate", "2026-05-05", "2026-05-05", "UTC", List.of("1"), List.of())).stream()
                        .map(row -> row.referral().getId()).toList());
        assertTrue(read(request("sentDate", "2026-05-05", "2026-05-05", "UTC", List.of("1"), List.of())).isEmpty());
        assertEquals(List.of(sent.getId()),
                read(request("sentDate", "2026-05-07", "2026-05-07", "UTC", List.of("1"), List.of())).stream()
                        .map(row -> row.referral().getId()).toList());
    }

    @Test
    public void datesIncludeBothLocalBoundariesAcrossDaylightSaving() {
        var first = referral("1", null, "2026-03-08T08:00:00Z");
        var last = referral("1", null, "2026-03-09T06:59:59Z");
        referral("1", null, "2026-03-08T07:59:59Z");
        referral("1", null, "2026-03-09T07:00:00Z");
        assertEquals(List.of(first.getId(), last.getId()),
                read(request("sentDate", "2026-03-08", "2026-03-08", "America/Los_Angeles", List.of("1"), List.of()))
                        .stream().map(row -> row.referral().getId()).toList());
    }

    @Test
    public void linkedResultsRemainDistinctAndUnreturnedReferralsRemainAvailable() {
        var first = referral("1", null, "2026-05-07T11:00:00Z");
        var one = returned(first, "450", "2026-05-08T12:00:00Z");
        var two = returned(first, "450", "2026-05-09T12:00:00Z");
        var pending = referral("1", null, "2026-05-07T12:00:00Z");
        var outside = referral("1", null, "2026-05-06T12:00:00Z");
        returned(outside, "999", "2026-05-07T12:00:00Z");
        var rows = read(request("sentDate", "2026-05-07", "2026-05-07", "UTC", List.of("1"), List.of()));
        assertEquals(3, rows.size());
        assertEquals(List.of(one.getId(), two.getId()),
                rows.subList(0, 2).stream().map(row -> row.returnedResult().getId()).toList());
        assertNotEquals(rows.get(0).returnedResult().getResult().getId(),
                rows.get(1).returnedResult().getResult().getId());
        assertEquals("450", rows.get(0).returnedResult().getResult().getValue());
        assertEquals("450", rows.get(1).returnedResult().getResult().getValue());
        assertEquals(pending.getId(), rows.get(2).referral().getId());
        assertNull(rows.get(2).returnedResult());
        entityManager.clear();
        assertEquals("12345", rows.get(0).referral().getAnalysis().getSampleItem().getSample().getAccessionNumber());
        assertEquals("Global Health Org", rows.get(0).referral().getOrganization().getOrganizationName());
        assertEquals("1", rows.get(0).returnedTest().getId());
    }

    @Test
    public void testAndLabSectionFiltersUseTheReferredAnalysis() {
        var selected = referral("1", null, "2026-05-07T11:00:00Z");
        referral("2", null, "2026-05-07T11:00:00Z");
        assertEquals(List.of(selected.getId()),
                read(request("sentDate", "2026-05-07", "2026-05-07", "UTC", List.of("1", "2"), List.of("1"))).stream()
                        .map(row -> row.referral().getId()).toList());
        assertTrue(read(request("sentDate", "2026-05-07", "2026-05-07", "UTC", List.of("2"), List.of("1"))).isEmpty());
    }

    @Test
    public void unsupportedDateAnchorsAreRejectedBeforeQueryExecution() {
        assertThrows(IllegalArgumentException.class,
                () -> read(request("resultDate", "2026-05-07", "2026-05-07", "UTC", List.of("1"), List.of())));
    }

    private String export(String type, List<String> fields, String date, long expectedRows) throws Exception {
        entityManager.flush();
        entityManager.clear();
        var definition = catalog.definition(type);
        var available = catalog.variables(definition, "TABLE").stream()
                .collect(java.util.stream.Collectors.toMap(v -> v.id(), java.util.function.Function.identity()));
        var snapshot = new ExportSnapshot(definition, "TABLE", fields.stream().map(available::get).toList(),
                new ExportFilter(date, date, List.of("1"), List.of(), List.of()), "UTC", List.of());
        StringWriter output = new StringWriter();
        assertEquals(expectedRows, catalog.source(definition.source()).write(output, snapshot));
        return output.toString();
    }

    @Test
    public void publishedReferralDefinitionUsesSentDatesAndPreservesReturnedAndPendingRows() throws Exception {
        var sent = referral("1", "2026-05-05T10:00:00Z", "2026-05-07T11:00:00Z");
        var first = returned(sent, "450", "2026-05-08T12:00:00Z");
        var repeated = returned(sent, "450", "2026-05-09T12:00:00Z");
        var pending = referral("1", "2026-05-05T10:00:00Z", "2026-05-07T12:00:00Z");
        referral("1", "2026-05-07T10:00:00Z", null);
        referral("1", "2026-05-07T10:00:00Z", "2026-05-08T00:00:00Z");
        var fields = List.of("referralId", "referralResultId", "resultId", "referralAccessionNumber",
                "referredTestName", "referralDate", "referralResultValue", "referralResultDate", "referralStatus");
        assertEquals("sentDate", catalog.definition("REFERRALS").dateAnchor());
        assertEquals("\uFEFFReferral ID,Referral Result ID,Result ID,Accession Number,Referred Test Name,Referral Date,"
                + "Referral Result Value,Referral Result Date,Referral Status\r\n" + sent.getId() + "," + first.getId()
                + "," + first.getResult().getId() + ",12345,Blood Test,2026-05-07,450,2026-05-08,REQUESTED\r\n"
                + sent.getId() + "," + repeated.getId() + "," + repeated.getResult().getId()
                + ",12345,Blood Test,2026-05-07,450,2026-05-09,REQUESTED\r\n" + pending.getId()
                + ",,,12345,Blood Test,2026-05-07,,,REQUESTED\r\n", export("REFERRALS", fields, "2026-05-07", 3));
    }

    @Test
    public void anotherReferralDefinitionChangesDateAndColumnsThroughConfiguration() throws Exception {
        referral("1", "2026-05-05T10:00:00Z", "2026-05-07T11:00:00Z");
        String json = """
                {"id":"REFERRAL_REQUESTS","version":1,"label":"Referral requests",
                 "source":"REFERRALS","dateAnchor":"requestDate","layouts":["TABLE"],
                 "attributes":["referralAccessionNumber","requestDate"],"catalogs":[],
                 "filters":["labSectionIds","testIds"],
                 "defaultColumns":{"TABLE":["requestDate","referralAccessionNumber"]}}
                """;
        configurations.processConfiguration(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                "referral-requests.json");
        assertEquals(List.of("requestDate", "referralAccessionNumber"),
                catalog.defaultColumns(catalog.definition("REFERRAL_REQUESTS"), "TABLE"));
        assertEquals("\uFEFFRequest Date,Accession Number\r\n2026-05-05,12345\r\n",
                export("REFERRAL_REQUESTS", List.of("requestDate", "referralAccessionNumber"), "2026-05-05", 1));
    }

    @Test
    public void referralRequestsAndSavedReportsHaveNoImplicitFinalizedResultFilter() {
        var role = entityManager.createQuery("from Role where name = :name", Role.class)
                .setParameter("name", Constants.ROLE_GLOBAL_ADMIN).getResultStream().findFirst().orElseGet(() -> {
                    var created = new Role();
                    created.setName(Constants.ROLE_GLOBAL_ADMIN);
                    created.setActive(true);
                    entityManager.persist(created);
                    return created;
                });
        var user = new SystemUser();
        user.setLoginName("referral-" + UUID.randomUUID().toString().substring(0, 8));
        user.setFirstName("Reporting");
        user.setLastName("Referrals");
        user.setIsActive("Y");
        user.setIsEmployee("Y");
        entityManager.persist(user);
        var grant = new UserRole();
        grant.setSystemUserId(user.getId());
        grant.setRoleId(role.getId());
        entityManager.persist(grant);
        entityManager.flush();
        var filter = new ExportFilter("2026-05-07", "2026-05-07", List.of(), List.of(), List.of());
        var request = new ExportSubmission(1, "REFERRALS", "TABLE", "referral-filter", List.of("referralId"), filter);
        var snapshot = catalog.freeze(request, user.getId());
        assertTrue(snapshot.filterSpec().resultStatuses().isEmpty());
        assertTrue(snapshot.statusIds().isEmpty());
        var saved = catalog.validateSaved(user.getId(), new SavedReportDefinition(1, "REFERRALS", "TABLE",
                List.of("referralId"), new SavedReportFilters(List.of(), List.of(), List.of())));
        assertTrue(saved.filters().resultStatuses().isEmpty());
        assertEquals(List.of(), catalog.catalog(user.getId(), "REFERRALS", "TABLE").get("statuses"));
        var sample = catalog.freeze(new ExportSubmission(1, "SAMPLE_TESTING", "SPREADSHEET", "sample-filter",
                List.of("accessionNumber"), filter), user.getId());
        assertEquals(List.of("FINALIZED"), sample.filterSpec().resultStatuses());
        assertEquals(1, sample.statusIds().size());
    }

    @Test
    public void interleavedMultiselectResultsKeepTheirGroupsAndReturnedDates() throws Exception {
        var sent = referral("1", "2026-05-05T10:00:00Z", "2026-05-07T11:00:00Z");
        var firstOption = new Dictionary();
        firstOption.setDictEntry("Option one");
        firstOption.setIsActive("Y");
        entityManager.persist(firstOption);
        var secondOption = new Dictionary();
        secondOption.setDictEntry("Option two");
        secondOption.setIsActive("Y");
        entityManager.persist(secondOption);
        var first = returned(sent, firstOption.getId(), "2026-05-08T12:00:00Z");
        var repeat = returned(sent, firstOption.getId(), "2026-05-09T12:00:00Z");
        var second = returned(sent, secondOption.getId(), "2026-05-08T12:00:00Z");
        var repeatedSecond = returned(sent, secondOption.getId(), "2026-05-09T12:00:00Z");
        for (var linked : List.of(first, repeat, second, repeatedSecond)) {
            linked.getResult().setResultType("M");
            linked.getResult().setGrouping(7);
        }
        assertEquals(
                "\uFEFFReferral Result ID,Result ID,Referral Result Value,Referral Result Date\r\n" + first.getId()
                        + "; " + second.getId() + "," + first.getResult().getId() + "; " + second.getResult().getId()
                        + ",Option one; Option two,2026-05-08\r\n" + repeat.getId() + "; " + repeatedSecond.getId()
                        + "," + repeat.getResult().getId() + "; " + repeatedSecond.getResult().getId()
                        + ",Option one; Option two,2026-05-09\r\n",
                export("REFERRALS",
                        List.of("referralResultId", "resultId", "referralResultValue", "referralResultDate"),
                        "2026-05-07", 2));
    }

}
