package org.openelisglobal.accreditation.daoimpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.accreditation.dao.AccreditingBodyDAO;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.accreditation.valueholder.AccreditingBody.LogoVisibilityMode;
import org.springframework.beans.factory.annotation.Autowired;

public class AccreditingBodyDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AccreditingBodyDAO accreditingBodyDAO;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/accreditation.xml");
        resyncSequence("clinlims.accrediting_body_seq", "clinlims.accrediting_body");
        resyncSequence("clinlims.test_accreditation_seq", "clinlims.test_accreditation");
    }

    @Test
    public void findByCode_shouldReturnBodyWhenCodeExists() {
        AccreditingBody found = accreditingBodyDAO.findByCode("ISO15189");

        Assert.assertNotNull(found);
        Assert.assertEquals(Long.valueOf(9001L), found.getId());
        Assert.assertEquals("ISO 15189:2022", found.getName());
    }

    @Test
    public void findByCode_shouldReturnNullWhenCodeDoesNotExist() {
        AccreditingBody found = accreditingBodyDAO.findByCode("NONEXISTENT");

        Assert.assertNull(found);
    }

    @Test
    public void findAllActive_shouldExcludeInactiveBodies() {
        List<AccreditingBody> activeBodies = accreditingBodyDAO.findAllActive();

        Assert.assertNotNull(activeBodies);
        Assert.assertEquals(2, activeBodies.size());
        Assert.assertTrue(activeBodies.stream().noneMatch(b -> "SLIPTA".equals(b.getCode())));
        Assert.assertTrue(activeBodies.stream().allMatch(AccreditingBody::getActive));
    }

    @Test
    public void findAllActive_shouldOrderByDisplayOrderAscending() {
        List<AccreditingBody> activeBodies = accreditingBodyDAO.findAllActive();

        Assert.assertEquals("CAP", activeBodies.get(0).getCode()); // display_order 0
        Assert.assertEquals("ISO15189", activeBodies.get(1).getCode()); // display_order 1
    }

    @Test
    public void findAllOrderedByDisplayOrder_shouldIncludeInactiveBodiesToo() {
        List<AccreditingBody> allBodies = accreditingBodyDAO.findAllOrderedByDisplayOrder();

        Assert.assertEquals(3, allBodies.size());
        Assert.assertEquals("CAP", allBodies.get(0).getCode());
        Assert.assertEquals("ISO15189", allBodies.get(1).getCode());
        Assert.assertEquals("SLIPTA", allBodies.get(2).getCode());
    }

    @Test
    public void countTestAccreditationsByBodyId_shouldCountLinkedAccreditations() {
        // ISO15189 (id 9001) has two test_accreditation rows in the fixture
        long count = accreditingBodyDAO.countTestAccreditationsByBodyId(9001L);

        Assert.assertEquals(2, count);
    }

    @Test
    public void countTestAccreditationsByBodyId_shouldReturnZeroWhenNoneLinked() {
        // CAP (id 9003) has no test_accreditation rows
        long count = accreditingBodyDAO.countTestAccreditationsByBodyId(9003L);

        Assert.assertEquals(0, count);
    }

    @Test
    public void insert_shouldPersistAndBeRetrievableByCode() {
        AccreditingBody newBody = new AccreditingBody();
        newBody.setCode("NEWBODY1");
        newBody.setName("New Accrediting Body");
        newBody.setLogoVisibilityMode(LogoVisibilityMode.ANY_ACCREDITED_TEST);
        newBody.setThresholdPct((short) 80);
        newBody.setDisplayOrder((short) 5);
        newBody.setActive(true);
        newBody.setSysUserId(TEST_SYS_USER_ID);

        accreditingBodyDAO.insert(newBody);

        AccreditingBody reloaded = accreditingBodyDAO.findByCode("NEWBODY1");
        Assert.assertNotNull(reloaded);
        Assert.assertEquals("New Accrediting Body", reloaded.getName());
    }

    @Test
    public void update_shouldPersistFieldChanges() {
        Optional<AccreditingBody> existingOpt = accreditingBodyDAO.get(9001L);
        Assert.assertTrue(existingOpt.isPresent());

        AccreditingBody existing = existingOpt.get();
        existing.setName("ISO 15189:2022 Renewed");
        existing.setUpdatedOn(LocalDateTime.now());
        accreditingBodyDAO.update(existing);

        Optional<AccreditingBody> reloadedOpt = accreditingBodyDAO.get(9001L);
        Assert.assertTrue(reloadedOpt.isPresent());
        Assert.assertEquals("ISO 15189:2022 Renewed", reloadedOpt.get().getName());
    }

    @Test
    public void logoVisibilityModeAndThresholdPct_shouldPersistTogether() {
        Optional<AccreditingBody> existingOpt = accreditingBodyDAO.get(9003L); // CAP, PERCENTAGE mode
        Assert.assertTrue(existingOpt.isPresent());

        AccreditingBody cap = existingOpt.get();
        Assert.assertEquals(LogoVisibilityMode.PERCENTAGE, cap.getLogoVisibilityMode());
        Assert.assertEquals(Short.valueOf((short) 90), cap.getThresholdPct());
    }
}
