package org.openelisglobal.siteinformation.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.test.util.ReflectionTestUtils;

public class SiteInformationConfigurationHandlerTest {

    private SiteInformationService siteInformationService;
    private SiteInformationDomainService siteInformationDomainService;
    private SiteInformationConfigurationHandler handler;

    @Before
    public void setUp() {
        siteInformationService = mock(SiteInformationService.class);
        siteInformationDomainService = mock(SiteInformationDomainService.class);
        handler = new SiteInformationConfigurationHandler();
        ReflectionTestUtils.setField(handler, "siteInformationService", siteInformationService);
        ReflectionTestUtils.setField(handler, "siteInformationDomainService", siteInformationDomainService);
    }

    @Test
    public void processConfiguration_updatesExistingSiteInformationByName() throws Exception {
        SiteInformation existing = new SiteInformation();
        existing.setName("phone format");
        existing.setValue("old");
        SiteInformationDomain domain = domain("validationConfig");
        when(siteInformationService.getSiteInformationByName("phone format")).thenReturn(existing);
        when(siteInformationDomainService.getByName("validationConfig")).thenReturn(domain);

        handler.processConfiguration(
                csv("""
                        name,value,domainName,valueType,tag,description,instructionKey,descriptionKey,group,encrypted
                        phone format,+261 (37|38) XX XXX XX,validationConfig,text,phone,Phone format,site.phone.instruction,site.phone.description,4,false
                        """),
                "site-information.csv");

        verify(siteInformationService).update(existing);
        assertEquals("+261 (37|38) XX XXX XX", existing.getValue());
        assertEquals("text", existing.getValueType());
        assertEquals("phone", existing.getTag());
        assertEquals("Phone format", existing.getDescription());
        assertEquals("site.phone.instruction", existing.getInstructionKey());
        assertEquals("site.phone.description", existing.getDescriptionKey());
        assertEquals(4, existing.getGroup());
        assertEquals(false, existing.isEncrypted());
        assertSame(domain, existing.getDomain());
        assertEquals("1", existing.getSysUserId());
    }

    @Test
    public void processConfiguration_insertsMissingSiteInformationByName() throws Exception {
        SiteInformationDomain domain = domain("validationConfig");
        when(siteInformationService.getSiteInformationByName("phone international validation")).thenReturn(null);
        when(siteInformationDomainService.getByName("validationConfig")).thenReturn(domain);

        handler.processConfiguration(
                csv("""
                        name,value,domainName,valueType,tag,description,instructionKey,descriptionKey,group,encrypted
                        phone international validation,E164,validationConfig,text,phone,International phone validation mode,,,4,false
                        """),
                "site-information.csv");

        ArgumentCaptor<SiteInformation> captor = ArgumentCaptor.forClass(SiteInformation.class);
        verify(siteInformationService).insert(captor.capture());
        SiteInformation inserted = captor.getValue();
        assertEquals("phone international validation", inserted.getName());
        assertEquals("E164", inserted.getValue());
        assertEquals("text", inserted.getValueType());
        assertEquals("phone", inserted.getTag());
        assertSame(domain, inserted.getDomain());
        assertEquals("1", inserted.getSysUserId());
    }

    private static ByteArrayInputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static SiteInformationDomain domain(String name) {
        SiteInformationDomain domain = new SiteInformationDomain();
        domain.setName(name);
        return domain;
    }
}
