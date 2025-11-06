package org.openelisglobal.analyte;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyteServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyteService analyteService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analyte.xml");
    }

    @Test
    public void getAnalytefromDataBase() {
        List<Analyte> analyteList = analyteService.getAll();
        analyteList.forEach(analyte -> {
            System.out.print(analyte.getAnalyteName() + " ");
        });

    }

    @Test
    public void getAnalyteByName_shouldReturnAnalyteByName() {
        Analyte analyte = new Analyte();
        analyte.setAnalyteName("Glucose");
        Analyte analyteByName = analyteService.getAnalyteByName(analyte, false);
        assertEquals("Glucose", analyteByName.getAnalyteName());
        assertEquals("EXT123", analyteByName.getExternalId());

    }

    @Test
    public void insert_shouldInsertAnalyte() {
        Analyte analyte = new Analyte();
        analyte.setAnalyteName("New Name");
        analyte.setIsActive("Y");
        analyte.setExternalId("EXT121");
        analyte.setLocalAbbreviation("NN");
        String inserted = analyteService.insert(analyte);
        Analyte insertedAnalyte = analyteService.get(inserted);
        assertEquals("NN", insertedAnalyte.getLocalAbbreviation());
        assertEquals("EXT121", insertedAnalyte.getExternalId());
        assertEquals("New Name", insertedAnalyte.getAnalyteName());

    }

    @Test
    public void save_shouldSaveAnalyte() {
        Analyte analyte = new Analyte();
        analyte.setAnalyteName("Saved Name");
        analyte.setIsActive("Y");
        analyte.setExternalId("EXT120");
        analyte.setLocalAbbreviation("SN");
        Analyte saved = analyteService.save(analyte);
        assertEquals("SN", saved.getLocalAbbreviation());
        assertEquals("Saved Name", saved.getAnalyteName());
    }

    @Test
    public void update_shouldUpdateAnalyte() {
        Analyte analyte = analyteService.get("1");
        analyte.setAnalyteName("Magnessium");
        analyte.setIsActive("N");
        analyte.setLocalAbbreviation("Mg");
        Analyte updated = analyteService.update(analyte);
        assertEquals("1", updated.getId());
        assertEquals("Magnessium", updated.getAnalyteName());
        assertEquals("N", updated.getIsActive());
    }

    @Test
    public void delete_shouldDeleteAnalyte() {

        Analyte analyte = analyteService.get("1");
        assertNotNull(analyte);
        assertEquals("Y", analyte.getIsActive());

        analyteService.delete(analyte);
        Analyte deleted = analyteService.get("1");
        assertNotNull(deleted);
        assertEquals("N", deleted.getIsActive());
    }

}