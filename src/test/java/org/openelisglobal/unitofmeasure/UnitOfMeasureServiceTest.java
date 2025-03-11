package org.openelisglobal.unitofmeasure;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;

public class UnitOfMeasureServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    UnitOfMeasureService uomService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/unitofmeasure.xml");
    }

    @Test
    public void createUnitOfMeasureShouldReturnCreatedUnitOfMeasure() throws Exception {
        UnitOfMeasure measure = new UnitOfMeasure();
        measure.setUnitOfMeasureName("Kilograms");
        measure.setDescription("measures weight");
        uomService.insert(measure);

        Assert.assertEquals("Kilograms", measure.getUnitOfMeasureName());

    }

    @Test
    public void getUnitOfMeasureByIdShouldReturnUnitOfMeasureById() throws Exception {
        UnitOfMeasure measure = uomService.getUnitOfMeasureById("1");

        Assert.assertEquals("inch", measure.getUnitOfMeasureName());
    }

    @Test
    public void getUnitOfMeasureByNameShouldReturnUnitOfMeasureByName() throws Exception {
        UnitOfMeasure measure = uomService.get("2");
        UnitOfMeasure savedmeasure = uomService.getUnitOfMeasureByName(measure);

        Assert.assertEquals("measure area", savedmeasure.getDescription());
    }

    @Test
    public void getLocalizationForUnitOfMeasureShouldReturnLocalizationForUnitOfMeasure() throws Exception {
        Localization local = uomService.getLocalizationForUnitOfMeasure("2");

        Assert.assertEquals("French", local.getFrench());
    }
}
