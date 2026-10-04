package org.openelisglobal.typeofsample.service;

import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.dao.TypeOfSampleDAO;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.security.access.prepost.PreAuthorize;

// Readable by anyone who may see the orderable catalogue: populating a
// sample-type dropdown is not sample-type administration. Writes on this
// interface carry PRIV_SAMPLE_TYPE_MANAGE individually.
@PreAuthorize("hasAnyAuthority('PRIV_SAMPLE_TYPE_VIEW','PRIV_CATALOGUE_VIEW')")
public interface TypeOfSampleService extends BaseObjectService<TypeOfSample, String> {
    void getData(TypeOfSample typeOfSample);

    String getNameForTypeOfSampleId(String id);

    List<TypeOfSample> getAllTypeOfSamples();

    /**
     * Which field of {@code candidate} collides with another sample type of the
     * same domain (trimmed, case-insensitive, inactive types included; the
     * candidate's own row is ignored): {@code "name"}, {@code "abbreviation"}, or
     * null.
     */
    String conflictingField(TypeOfSample candidate);

    /**
     * A local abbreviation for a new sample type named {@code name} that no other
     * sample type of {@code domain} uses: the first ten characters of the name, or,
     * when taken, a shorter stem with a number ("QADedupe S", then "QADedupe2").
     */
    String uniqueLocalAbbreviation(String name, String domain);

    /**
     * True when any sample type, of any domain and active or not, already has this
     * name (trimmed, case-insensitive). A new sample type's workplan, results and
     * validation modules are named after it, so a name can be created only once.
     */
    boolean nameInUse(String name);

    List<TypeOfSample> getAllTypeOfSamplesSortOrdered();

    List<TypeOfSample> getTypesForDomain(TypeOfSampleDAO.SampleDomain domain);

    Integer getTotalTypeOfSampleCount();

    TypeOfSample getTypeOfSampleById(String typeOfSampleId);

    TypeOfSample getSampleTypeFromTest(Test test);

    List<TypeOfSample> getTypesForDomainBySortOrder(TypeOfSampleDAO.SampleDomain human);

    List<TypeOfSample> getPageOfTypeOfSamples(int startingRecNo);

    List<TypeOfSample> getTypes(String filter, String domain);

    TypeOfSample getTypeOfSampleByLocalAbbrevAndDomain(String localAbbrev, String domain);

    TypeOfSample getTypeOfSampleByDescriptionAndDomain(TypeOfSample tos, boolean ignoreCase);

    List<Test> getAllTestsBySampleTypeId(String sampleTypeId);

    List<Test> getActiveTestsBySampleTypeId(String sampleType, boolean b);

    List<Test> getActiveTestsBySampleTypeIdAndTestUnit(String sampleType, boolean b, List<String> testUnitIds);

    TypeOfSample getTransientTypeOfSampleById(String sampleTypeId);

    void clearCache();

    List<TypeOfSample> getTypeOfSampleForTest(String testId);

    String getTypeOfSampleNameForId(String id);

    String getTypeOfSampleIdForLocalAbbreviation(String name);

    List<TypeOfSample> getTypeOfSampleForPanelId(String id);

    Localization getLocalizationForSampleType(String id);

    TypeOfSample getTypeOfSampleByLocalizedName(String typeOfSampleName, Locale locale);

    /**
     * Moves the given sample type to the 1-based position in the global sort-order
     * sequence and renumbers every sample type to a dense 1..n, so the order-entry
     * Sample Type menu ordering is deterministic. Returns the resulting full list
     * in its new order.
     *
     * <p>
     * A catalogue WRITE, so it is pinned to PRIV_SAMPLE_TYPE_MANAGE rather than
     * inheriting the interface gate, that one accepts PRIV_CATALOGUE_VIEW, which
     * every order-entry role holds, and reordering the menu is not a read.
     */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    List<TypeOfSample> moveToSortOrderPosition(String typeOfSampleId, int position, String sysUserId);
}
