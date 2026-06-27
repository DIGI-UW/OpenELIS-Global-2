package org.openelisglobal.microbiology;

import static org.junit.Assert.assertNotNull;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.beans.factory.annotation.Autowired;

public class MicrobiologyOrmValidationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SessionFactory sessionFactory;

    @Before
    public void unwrapSessionFactory() {
        sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    }

    @Test
    public void microbiologyReferenceEntitiesAreRegisteredInMetamodel() {
        Metamodel metamodel = sessionFactory.getMetamodel();
        assertNotNull(metamodel.entity(MicroOrganism.class));
        assertNotNull(metamodel.entity(MicroAntibiotic.class));
        assertNotNull(metamodel.entity(MicroAstPanel.class));
        assertNotNull(metamodel.entity(MicroAstPanelAntibiotic.class));
        assertNotNull(metamodel.entity(MicroBreakpointStandard.class));
        assertNotNull(metamodel.entity(MicroBreakpointRule.class));
        assertNotNull(metamodel.entity(MicroCultureSetup.class));
        assertNotNull(metamodel.entity(MicroCase.class));
        assertNotNull(metamodel.entity(MicroCaseActivity.class));
        assertNotNull(metamodel.entity(MicroIsolate.class));
        assertNotNull(metamodel.entity(MicroAstRun.class));
        assertNotNull(metamodel.entity(MicroAstReading.class));
        assertNotNull(metamodel.entity(MicroCriticalCommunication.class));
    }
}
