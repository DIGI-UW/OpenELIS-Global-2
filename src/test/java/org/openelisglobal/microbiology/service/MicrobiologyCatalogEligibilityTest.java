package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.microbiology.valueholder.MicroCaseTestRole;

public class MicrobiologyCatalogEligibilityTest {

    private final MicroOrderRoutingService routing = new MicroOrderRoutingServiceImpl(null, null, null);

    @Test
    public void aNewCatalogTestDoesNotOpenACase() {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();

        assertFalse(test.isOpensMicrobiologyCase());
        assertEquals(MicroCaseTestRole.DIRECT.name(), test.getMicrobiologyCaseRole());
        assertFalse(test.isCollectedInSets());
        assertFalse(routing.isMicrobiologyOrder(List.of(test)));
    }

    @Test
    public void surveillanceFlagDoesNotEnableCaseOpening() {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setAntimicrobialResistance(true);

        assertFalse(routing.isMicrobiologyOrder(List.of(test)));
        test.setOpensMicrobiologyCase(true);
        test.setAntimicrobialResistance(false);

        assertTrue(routing.isMicrobiologyOrder(List.of(test)));
        assertFalse(test.getAntimicrobialResistance());
    }

    @Test
    public void everyCaseRoleCanOpenWithoutACultureOrProgram() {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setOpensMicrobiologyCase(true);
        for (MicroCaseTestRole role : MicroCaseTestRole.values()) {
            test.setMicrobiologyCaseRole(role.name());
            assertTrue(role.name(), routing.isMicrobiologyOrder(List.of(test)));
        }
    }
}
