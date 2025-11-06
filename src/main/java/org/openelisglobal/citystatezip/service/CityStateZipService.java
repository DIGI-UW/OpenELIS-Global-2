package org.openelisglobal.citystatezip.service;

import java.util.List;
import org.openelisglobal.citystatezip.valueholder.CityStateZip;
import org.openelisglobal.common.service.BaseObjectService;

public interface CityStateZipService extends BaseObjectService<CityStateZip, String> {
    CityStateZip getState(CityStateZip cityStateZip);

    List<CityStateZip> getValidCityStateZipCombosForHumanSampleEntry(CityStateZip cityStateZip);

    List<CityStateZip> getCities(String filter);

    CityStateZip getZipCode(CityStateZip cityStateZip);

    List<CityStateZip> getAllStateCodes();

    CityStateZip getCity(CityStateZip cityStateZip);

    List<CityStateZip> getCitiesByZipCode(CityStateZip cityStateZip);

    String getCountyCodeByStateAndZipCode(CityStateZip cityStateZip);

    List<CityStateZip> getZipCodesByCity(CityStateZip cityStateZip);

    boolean isCityStateZipComboValid(CityStateZip cityStateZip);

    CityStateZip getCityStateZipByCityAndZipCode(CityStateZip cityStateZip);
}
