package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1420 (9a), FR-I5: the FHIR Organization carries every identifier the
 * record holds, each with its label as {@code type.text} and the reporting code
 * as the official one, and always its OpenELIS UUID.
 */
public class OrganizationTransformIdentifiersTest extends BaseWebContextSensitiveTest {

    @Autowired
    private OrganizationTransformService transformService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private DataSource dataSource;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/organization.xml");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("UPDATE clinlims.organization SET code = NULL WHERE id = 4");
        jdbc.update("INSERT INTO clinlims.organization_identifier (id, organization_id, label, value, is_reporting,"
                + " lastupdated) VALUES (nextval('clinlims.organization_identifier_seq'), 4, 'Code', 'HSI002', true,"
                + " now()), (nextval('clinlims.organization_identifier_seq'), 4, 'DHIS2 ID', 'Qw8LmZa21Ks', false,"
                + " now())");
    }

    private static Identifier labelled(org.hl7.fhir.r4.model.Organization organization, String label) {
        return organization.getIdentifier().stream().filter(i -> i.hasType() && label.equals(i.getType().getText()))
                .findFirst().orElse(null);
    }

    @Test
    public void everyIdentifierIsSentWithItsLabel_andTheReportingCodeIsOfficial() {
        org.hl7.fhir.r4.model.Organization fhir = transformService
                .transformToFhirOrganization(organizationService.get("4"));

        Identifier dhis2 = labelled(fhir, "DHIS2 ID");
        assertTrue("the DHIS2 ID is sent", dhis2 != null);
        assertEquals("Qw8LmZa21Ks", dhis2.getValue());
        assertTrue(dhis2.getUse() != IdentifierUse.OFFICIAL);
        Identifier code = labelled(fhir, "Code");
        assertTrue("the reporting code is sent", code != null);
        assertEquals("HSI002", code.getValue());
        assertEquals(IdentifierUse.OFFICIAL, code.getUse());
        assertTrue("the OpenELIS UUID is sent even when the legacy code column is empty",
                fhir.getIdentifier().stream().anyMatch(i -> i.getSystem() != null && i.getSystem().endsWith("/org_uuid")
                        && "a3bdeff8-8d5b-4023-bd7c-932b4b98b6d4".equals(i.getValue())));
    }
}
