package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroCaseSpecimen;
import org.openelisglobal.microbiology.valueholder.MicroExportReportingTrack;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;

public class MicrobiologyMembershipMappingTest {
    @Test
    public void canonicalMembershipAndIsolateSourceBuildWithoutADatabase() {
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        for (Class<?> entity : Set.of(MicroCase.class, MicroCaseSpecimen.class, MicroCaseAnalysis.class,
                MicroIsolate.class, MicroExportReportingTrack.class, MicroCaseActivity.class)) {
            configuration.addAnnotatedClass(entity);
        }
        try (SessionFactory factory = configuration.buildSessionFactory()) {
            Set<String> caseAttributes = factory.getMetamodel().entity(MicroCase.class).getAttributes().stream()
                    .map(attribute -> attribute.getName()).collect(Collectors.toSet());
            assertTrue(caseAttributes.containsAll(Set.of("sampleId", "sampleTypeId", "testSectionId", "programId")));
            assertFalse(caseAttributes.contains("sampleItemId"));
            assertFalse(caseAttributes.contains("workflowType"));
            assertFalse(caseAttributes.contains("cultureMethodId"));
            assertEquals(String.class,
                    factory.getMetamodel().entity(MicroCaseSpecimen.class).getAttribute("sampleItemId").getJavaType());
            assertEquals(String.class,
                    factory.getMetamodel().entity(MicroIsolate.class).getAttribute("sourceSampleItemId").getJavaType());
            assertEquals(String.class, factory.getMetamodel().entity(MicroCaseActivity.class)
                    .getAttribute("resultSourceSampleItemId").getJavaType());
            assertEquals(boolean.class, factory.getMetamodel().entity(MicroCaseAnalysis.class)
                    .getAttribute("collectedInSets").getJavaType());
        }
    }
}
