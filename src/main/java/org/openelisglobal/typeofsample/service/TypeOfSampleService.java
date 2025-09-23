package org.openelisglobal.typeofsample.service;

import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.dao.TypeOfSampleDAO;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

public interface TypeOfSampleService extends BaseObjectService<TypeOfSample, String> {
    void getData(TypeOfSample typeOfSample);

    String getNameForTypeOfSampleId(String id);

    List<TypeOfSample> getAllTypeOfSamples();

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
}
