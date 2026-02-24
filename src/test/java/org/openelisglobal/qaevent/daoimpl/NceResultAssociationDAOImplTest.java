package org.openelisglobal.qaevent.daoimpl;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qaevent.dao.NceResultAssociationDAO;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DAO integration test for NceResultAssociationDAOImpl. Uses
 * BaseWebContextSensitiveTest with DBUnit test data for database-backed tests.
 */
public class NceResultAssociationDAOImplTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NceResultAssociationDAO dao;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/nce-result-association.xml");
    }

    @Test
    public void getNceAssociationsForResult_existingResult_returnsAssociations() {
        List<NceResultAssociation> associations = dao.getNceAssociationsForResult("3");

        assertEquals(2, associations.size());
    }

    @Test
    public void getNceAssociationsForResult_noAssociations_returnsEmpty() {
        List<NceResultAssociation> associations = dao.getNceAssociationsForResult("999");

        assertTrue(associations.isEmpty());
    }

    @Test
    public void getResultAssociationsForNCE_existingNCE_returnsAssociations() {
        List<NceResultAssociation> associations = dao.getResultAssociationsForNCE("1");

        assertEquals(2, associations.size());
    }

    @Test
    public void getResultAssociationsForNCE_noAssociations_returnsEmpty() {
        List<NceResultAssociation> associations = dao.getResultAssociationsForNCE("999");

        assertTrue(associations.isEmpty());
    }

    @Test
    public void associationExists_existingAssociation_returnsTrue() {
        assertTrue(dao.associationExists("3", "1"));
    }

    @Test
    public void associationExists_nonExistentAssociation_returnsFalse() {
        assertFalse(dao.associationExists("3", "999"));
    }

    @Test
    public void getAssociationsByType_existingType_returnsMatches() {
        List<NceResultAssociation> associations = dao.getAssociationsByType("RESULT_TRIGGERED_NCE");

        assertEquals(1, associations.size());
        assertEquals("RESULT_TRIGGERED_NCE", associations.get(0).getAssociationType());
    }

    @Test
    public void getAssociationsByType_deltaCheckEscalation_returnsMatches() {
        List<NceResultAssociation> associations = dao.getAssociationsByType("DELTA_CHECK_ESCALATION");

        assertEquals(1, associations.size());
    }

    @Test
    public void countNCEsForResult_resultWithTwoNCEs_returnsTwo() {
        int count = dao.countNCEsForResult("3");

        assertEquals(2, count);
    }

    @Test
    public void countNCEsForResult_resultWithOneNCE_returnsOne() {
        int count = dao.countNCEsForResult("4");

        assertEquals(1, count);
    }

    @Test
    public void countResultsForNCE_nceWithTwoResults_returnsTwo() {
        int count = dao.countResultsForNCE("1");

        assertEquals(2, count);
    }

    @Test
    public void getAssociationsCreatedBy_admin_returnsAll() {
        List<NceResultAssociation> associations = dao.getAssociationsCreatedBy("admin");

        assertEquals(3, associations.size());
    }

    @Test
    public void getAssociationsCreatedBy_unknownUser_returnsEmpty() {
        List<NceResultAssociation> associations = dao.getAssociationsCreatedBy("unknown");

        assertTrue(associations.isEmpty());
    }
}
