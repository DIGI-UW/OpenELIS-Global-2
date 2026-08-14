package org.openelisglobal.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.metamodel.EntityType;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;

public class AnalyzerSiteBindingOrmValidationTest {

    @Test(timeout = 5000)
    public void siteBindingMappingsBuildWithoutDatabaseAccess() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(Analyzer.class);
        configuration.addAnnotatedClass(AnalyzerType.class);
        configuration.addAnnotatedClass(AnalyzerSiteBinding.class);
        configuration.addAnnotatedClass(AnalyzerSiteBindingRevision.class);
        configuration.addAnnotatedClass(AnalyzerSiteBindingTest.class);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");

        try (SessionFactory sessionFactory = configuration.buildSessionFactory(
                new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build())) {
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerSiteBinding.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerSiteBindingRevision.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerSiteBindingTest.class));

            EntityType<Analyzer> analyzer = sessionFactory.getMetamodel().entity(Analyzer.class);
            assertEquals(String.class, analyzer.getAttribute("bridgeProfileId").getJavaType());
            assertEquals(Integer.class, analyzer.getAttribute("bridgeProfileRevision").getJavaType());
            assertEquals(AnalyzerSiteBindingRevision.class, analyzer.getAttribute("siteBindingRevision").getJavaType());
        }
    }

    @Test
    public void sourceRowsKeepIndependentCompositeIdentity() {
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        AnalyzerSiteBindingTest first = row(revision, "row-1", "WBC");
        AnalyzerSiteBindingTest second = row(revision, "row-2", "WBC_ALIAS");

        assertEquals(revision.getId(), first.getId().getSiteBindingRevisionId());
        assertEquals("row-1", first.getId().getSourceRowKey());
        assertTrue(!first.getId().equals(second.getId()));
        assertEquals(List.of("WBC#", "WHITE_COUNT"), first.getAliases());
    }

    private static AnalyzerSiteBindingTest row(AnalyzerSiteBindingRevision revision, String sourceRowKey,
            String analyzerCode) {
        AnalyzerSiteBindingTest row = new AnalyzerSiteBindingTest();
        row.setSiteBindingRevision(revision);
        row.setId(new AnalyzerSiteBindingTestPK(revision.getId(), sourceRowKey));
        row.setRawAnalyzerCode(analyzerCode);
        row.setAliases(List.of("WBC#", "WHITE_COUNT"));
        row.setMappingState(AnalyzerSiteBindingTest.MappingState.UNRESOLVED);
        return row;
    }
}
