package org.openelisglobal.citystatezip;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.citystatezip.service.CityStateZipService;
import org.openelisglobal.citystatezip.valueholder.CityStateZip;
import org.springframework.beans.factory.annotation.Autowired;

public class CityStateZipServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    CityStateZipService cService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/citystatezip.xml");
    }

    @Test
    public void getState_shouldReturnCorrectCityStateZipObject() {
        CityStateZip cityStateZip = cService.get("1");

        Assert.assertNotNull("cityStateZip should not be null", cityStateZip);
        Assert.assertEquals("WA", cityStateZip.getState());

        CityStateZip result = cService.getState(cityStateZip);

        Assert.assertNotNull("Returned CityStateZip should not be null", result);
        Assert.assertEquals("WA", result.getState());
    }

    @Test
    public void getValidCityStateZipCombosForHumanSampleEntry_shouldReturnValidCityStateZipCombosForHumanSampleEntry() {
        CityStateZip cityStateZip = cService.get("1");
        List<CityStateZip> cityStateZips = cService.getValidCityStateZipCombosForHumanSampleEntry(cityStateZip);

        Assert.assertEquals(5, cityStateZips.size());
        Assert.assertEquals("SEATTLE", cityStateZips.get(0).getCity());
        Assert.assertEquals("BROADWAY", cityStateZips.get(1).getCity());
    }

    @Test
    public void getCities_shouldReturnFilteredCities() {
        List<CityStateZip> cityStateZips = cService.getCities("SAN");

        Assert.assertEquals(2, cityStateZips.size());
        Assert.assertEquals("SAN DIEGO", cityStateZips.get(0).getCity());
        Assert.assertEquals("SAN FRANCISCO", cityStateZips.get(1).getCity());
    }

    @Test
    public void getZipCode_shouldReturnCorrectZipCodeObject() {
        CityStateZip cityStateZip = cService.get("1");

        Assert.assertNotNull("getZipCode should not be null", cityStateZip);
        Assert.assertEquals("98102", cityStateZip.getZipCode());

        CityStateZip result = cService.getZipCode(cityStateZip);

        Assert.assertNotNull("Returned getZipCode should not be null", result);
        Assert.assertEquals("98102", result.getZipCode());
    }

    @Test
    public void getAllStateCodes_shouldReturnAllStateCodes() {
        List<CityStateZip> cityStateZips = cService.getAllStateCodes();

        Assert.assertEquals(2, cityStateZips.size());
    }

    @Test
    public void getCity_shouldReturnCity() {
        CityStateZip cityStateZip = cService.get("5");
        CityStateZip city = cService.getCity(cityStateZip);

        Assert.assertEquals("LOS ANGELES", city.getCity());
    }

    @Test
    public void getCitiesByZipCode_shouldReturnCity() {
        CityStateZip cityStateZip = cService.get("1");
        List<CityStateZip> cityStateZips = cService.getCitiesByZipCode(cityStateZip);

        Assert.assertEquals(4, cityStateZips.size());
        Assert.assertEquals("BROADWAY", cityStateZips.get(0).getCity());
        Assert.assertEquals("CAPITOL HILL", cityStateZips.get(1).getCity());
    }

    @Test
    public void getCountyCodeByStateAndZipCode_shouldReturnCountyCode() {
        CityStateZip cityStateZip = cService.get("5");
        String cCode = cService.getCountyCodeByStateAndZipCode(cityStateZip);

        Assert.assertEquals("34", cCode);
    }

    @Test
    public void getZipCodesByCity_shouldReturnZipCodesByCity() {
        CityStateZip cityStateZip = cService.get("1");
        List<CityStateZip> cityStateZips = cService.getZipCodesByCity(cityStateZip);

        Assert.assertEquals(1, cityStateZips.size());
        Assert.assertEquals("98102", cityStateZips.get(0).getZipCode());
    }

    @Test
    public void isCityStateZipComboValid_shouldConfirmValidCityStateZipCombo() {
        CityStateZip cityStateZip = cService.get("5");

        Assert.assertTrue(cService.isCityStateZipComboValid(cityStateZip));
    }

    @Test
    public void getCityStateZipByCityAndZipCode_shouldReturnCityStateZipByCityAndZipCode() {
        CityStateZip cityStateZip = cService.get("7");
        CityStateZip city = cService.getCityStateZipByCityAndZipCode(cityStateZip);

        Assert.assertEquals("SACRAMENTO", city.getCity());
        Assert.assertEquals("QUEENS", city.getCounty());
        Assert.assertEquals("CALIFORNIA", city.getStateName());
    }
}
