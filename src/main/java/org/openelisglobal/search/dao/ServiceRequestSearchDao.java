package org.openelisglobal.search.dao;

import jakarta.persistence.criteria.Expression;
import java.util.List;
import java.util.Objects;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.fhir.dao.BaseFhirDao;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.ServiceRequestSearchParams;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ServiceRequestSearchDao extends BaseFhirDao {

    public ServiceRequestSearchDao(FhirPropertyResolver propertyResolver) {

        super(propertyResolver);
    }

    /**
     * Searches ServiceRequests represented by Analysis entities.
     */
    public List<Analysis> search(ServiceRequestSearchParams params) {

        FhirCriteriaContext<Analysis, Analysis> context = createCriteriaContext(Analysis.class);

        if (params != null) {
            addSearchPredicates(context, params);
        }

        context.distinct(true);

        return list(context);
    }

    /**
     * Searches ServiceRequests using zero-based pagination.
     */
    public List<Analysis> search(ServiceRequestSearchParams params, int offset, int pageSize) {

        validatePagination(offset, pageSize);

        FhirCriteriaContext<Analysis, Analysis> context = createCriteriaContext(Analysis.class);

        if (params != null) {
            addSearchPredicates(context, params);
        }

        context.distinct(true);

        return list(context, offset, pageSize);
    }

    /**
     * Counts ServiceRequests matching the supplied search parameters.
     */
    public long count(ServiceRequestSearchParams params) {

        return count(Analysis.class, context -> {
            if (params != null) {
                addSearchPredicates(context, params);
            }
        });
    }

    /**
     * Finds Analysis records related to the supplied SampleHuman records.
     *
     * This supports the Practitioner reverse-include flow:
     *
     * matched Provider objects -> SampleHumanSearchDao.findByProviders(...) ->
     * List<SampleHuman> -> ServiceRequestSearchDao.findBySampleHumans(...) ->
     * List<Analysis>
     *
     * SampleHuman.sampleId is an OpenELIS Sample primary key. It is matched against
     * Analysis.sampleItem.sample.id.
     */
    public List<Analysis> findBySampleHumans(List<SampleHuman> sampleHumans) {

        List<String> sampleIds = extractSampleIds(sampleHumans);

        if (sampleIds.isEmpty()) {
            return List.of();
        }

        FhirCriteriaContext<Analysis, Analysis> context = createCriteriaContext(Analysis.class);

        Expression<String> analysisSampleId = resolveExpression(context, FhirConstants.ANALYSIS_SAMPLE_ID_HANDLER,
                String.class);

        context.addPredicate(analysisSampleId.in(sampleIds));

        context.distinct(true);

        return list(context);
    }

    /**
     * Adds the standard ServiceRequest search predicates.
     */
    private <R> void addSearchPredicates(FhirCriteriaContext<Analysis, R> context, ServiceRequestSearchParams params) {

        addPredicate(context, createIdPredicate(context, params.getId()));

        addPredicate(context, createIdentifierPredicate(context, params.getIdentifier()));

        addPredicate(context, createLastUpdatedPredicate(context, params.getLastUpdated()));
    }

    /**
     * Extracts distinct OpenELIS Sample primary keys from SampleHuman records.
     */
    private List<String> extractSampleIds(List<SampleHuman> sampleHumans) {

        if (sampleHumans == null || sampleHumans.isEmpty()) {
            return List.of();
        }

        return sampleHumans.stream().filter(Objects::nonNull).map(SampleHuman::getSampleId).filter(Objects::nonNull)
                .map(String::trim).filter(sampleId -> !sampleId.isEmpty()).distinct().toList();
    }

    private void validatePagination(int offset, int pageSize) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be zero or greater");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
    }
}