package org.openelisglobal.reports.adhoc.service;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.reports.adhoc.dto.AdHocReportDefinitionDTO;
import org.openelisglobal.reports.adhoc.dto.FilterCriteriaDTO;
import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO.FilterOperator;
import org.openelisglobal.reports.adhoc.service.AdHocQueryBuilderService.QueryResult;

@RunWith(MockitoJUnitRunner.class)
public class AdHocQueryBuilderServiceTest {

    @Spy
    private AdHocFieldDefinitionServiceImpl fieldDefinitionService;

    @InjectMocks
    private AdHocQueryBuilderServiceImpl queryBuilderService;

    @Test
    public void testBuildQuery_PatientFieldsOnly_GeneratesValidHql() {
        AdHocReportDefinitionDTO definition = new AdHocReportDefinitionDTO();
        definition.setSelectedFields(Arrays.asList("patient.nationalId", "patient.firstName", "patient.lastName"));

        QueryResult result = queryBuilderService.buildQuery(definition);

        assertNotNull(result);
        assertNotNull(result.getHql());
        assertTrue(result.getHql().contains("SELECT"));
        assertTrue(result.getHql().contains("FROM"));
        assertTrue(result.getHql().contains("Patient"));
    }

    @Test
    public void testBuildQuery_SampleFieldsOnly_GeneratesJoins() {
        AdHocReportDefinitionDTO definition = new AdHocReportDefinitionDTO();
        definition.setSelectedFields(Arrays.asList("sample.accessionNumber", "sample.collectionDate"));

        QueryResult result = queryBuilderService.buildQuery(definition);

        assertNotNull(result);
        assertNotNull(result.getHql());
        assertTrue(result.getHql().contains("Sample"));
    }

    @Test
    public void testBuildQuery_MixedFields_GeneratesBothEntities() {
        AdHocReportDefinitionDTO definition = new AdHocReportDefinitionDTO();
        definition
                .setSelectedFields(Arrays.asList("patient.nationalId", "patient.firstName", "sample.accessionNumber"));

        QueryResult result = queryBuilderService.buildQuery(definition);

        assertNotNull(result);
        String hql = result.getHql();
        assertTrue(hql.contains("Patient"));
        assertTrue(hql.contains("Sample"));
        assertTrue(hql.contains("SampleHuman"));
    }

    @Test
    public void testBuildQuery_WithEqualsFilter_AddsWhereClause() {
        AdHocReportDefinitionDTO definition = new AdHocReportDefinitionDTO();
        definition.setSelectedFields(Arrays.asList("patient.nationalId", "patient.gender"));
        definition.setFilters(Arrays.asList(new FilterCriteriaDTO("patient.gender", FilterOperator.EQUALS, "M")));

        QueryResult result = queryBuilderService.buildQuery(definition);

        assertNotNull(result);
        assertTrue(result.getHql().contains("WHERE"));
        assertTrue(result.getParameters().containsValue("M"));
    }

    @Test
    public void testBuildQuery_WithContainsFilter_GeneratesLikeClause() {
        AdHocReportDefinitionDTO definition = new AdHocReportDefinitionDTO();
        definition.setSelectedFields(Arrays.asList("patient.nationalId", "patient.firstName"));
        definition
                .setFilters(Arrays.asList(new FilterCriteriaDTO("patient.firstName", FilterOperator.CONTAINS, "John")));

        QueryResult result = queryBuilderService.buildQuery(definition);

        assertNotNull(result);
        assertTrue(result.getHql().toUpperCase().contains("LIKE"));
        assertTrue(result.getParameters().values().stream().anyMatch(v -> v.toString().contains("%John%")));
    }

    @Test
    public void testBuildQuery_WithBetweenFilter_GeneratesBetweenClause() {
        AdHocReportDefinitionDTO definition = new AdHocReportDefinitionDTO();
        definition.setSelectedFields(Arrays.asList("sample.accessionNumber", "sample.collectionDate"));
        definition.setFilters(Arrays.asList(
                new FilterCriteriaDTO("sample.collectionDate", FilterOperator.BETWEEN, "2025-01-01", "2025-01-31")));

        QueryResult result = queryBuilderService.buildQuery(definition);

        assertNotNull(result);
        assertTrue(result.getHql().toUpperCase().contains(">="));
        assertTrue(result.getHql().toUpperCase().contains("<"));
    }

    @Test
    public void testBuildQuery_WithIsNullFilter_GeneratesIsNullClause() {
        AdHocReportDefinitionDTO definition = new AdHocReportDefinitionDTO();
        definition.setSelectedFields(Arrays.asList("patient.nationalId", "patient.externalId"));
        definition.setFilters(Arrays.asList(new FilterCriteriaDTO("patient.externalId", FilterOperator.IS_NULL, null)));

        QueryResult result = queryBuilderService.buildQuery(definition);

        assertNotNull(result);
        assertTrue(result.getHql().toUpperCase().contains("IS NULL"));
    }

    @Test
    public void testBuildQuery_WithSortBy_GeneratesOrderByClause() {
        AdHocReportDefinitionDTO definition = new AdHocReportDefinitionDTO();
        definition.setSelectedFields(Arrays.asList("patient.nationalId", "patient.firstName"));
        definition.setSortBy("patient.firstName");
        definition.setSortOrder(AdHocReportDefinitionDTO.SortOrder.DESC);

        QueryResult result = queryBuilderService.buildQuery(definition);

        assertNotNull(result);
        assertTrue(result.getHql().toUpperCase().contains("ORDER BY"));
        assertTrue(result.getHql().toUpperCase().contains("DESC"));
    }

    @Test
    public void testBuildCountQuery_GeneratesCountSelect() {
        AdHocReportDefinitionDTO definition = new AdHocReportDefinitionDTO();
        definition.setSelectedFields(Arrays.asList("patient.nationalId", "sample.accessionNumber"));

        QueryResult result = queryBuilderService.buildCountQuery(definition);

        assertNotNull(result);
        assertTrue(result.getHql().toUpperCase().contains("COUNT"));
        assertTrue(result.getHql().toUpperCase().contains("DISTINCT"));
    }

    @Test
    public void testBuildQuery_WithMultipleFilters_CombinesWithAnd() {
        AdHocReportDefinitionDTO definition = new AdHocReportDefinitionDTO();
        definition.setSelectedFields(Arrays.asList("patient.nationalId", "patient.gender", "patient.firstName"));
        definition.setFilters(Arrays.asList(new FilterCriteriaDTO("patient.gender", FilterOperator.EQUALS, "M"),
                new FilterCriteriaDTO("patient.firstName", FilterOperator.STARTS_WITH, "J")));

        QueryResult result = queryBuilderService.buildQuery(definition);

        assertNotNull(result);
        String hql = result.getHql().toUpperCase();
        int andCount = hql.split(" AND ").length - 1;
        assertTrue(andCount >= 2);
    }
}
