package org.openelisglobal.image.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletContext;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.image.dao.ImageDAO;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;

@RunWith(MockitoJUnitRunner.class)
public class ImageServiceImplTest {

    @Mock
    private ImageDAO imageDAO;

    @Mock
    private SiteInformationService siteInformationService;

    @Mock
    private ServletContext servletContext;

    @InjectMocks
    private ImageServiceImpl imageService;

    @Test
    public void init_shouldBuildPreviewPathFromServletContext() {
        when(servletContext.getRealPath("")).thenReturn("/opt/openelis");

        imageService.init();

        assertEquals("/opt/openelis/static/images/", imageService.getFullPreviewPath());
    }

    @Test
    public void getImageNameFilePath_shouldResolveConfiguredAliases() {
        assertEquals("leftLabLogo.jpg", imageService.getImageNameFilePath("headerLeftImage"));
        assertEquals("labDirectorSignature.jpg", imageService.getImageNameFilePath("labDirectorSignature"));
        assertEquals("rightLabLogo.jpg", imageService.getImageNameFilePath("anyOtherKey"));
    }

    @Test
    public void getImageByDescription_shouldDelegateToDAO() {
        Image expected = new Image();
        expected.setId("2");
        when(imageDAO.getImageByDescription("headerRightImage")).thenReturn(expected);

        Image actual = imageService.getImageByDescription("headerRightImage");

        assertEquals("2", actual.getId());
        verify(imageDAO).getImageByDescription("headerRightImage");
    }

    @Test
    public void getImageBySiteInfoName_shouldReturnEmptyWhenSiteInfoMissing() {
        when(siteInformationService.getSiteInformationByName("missing")).thenReturn(null);

        Optional<Image> result = imageService.getImageBySiteInfoName("missing");

        assertFalse(result.isPresent());
    }

    @Test
    public void getImageBySiteInfoName_shouldReturnEmptyWhenSiteInfoValueIsBlank() {
        SiteInformation siteInformation = new SiteInformation();
        siteInformation.setValue("   ");
        when(siteInformationService.getSiteInformationByName("blankValue")).thenReturn(siteInformation);

        Optional<Image> result = imageService.getImageBySiteInfoName("blankValue");

        assertFalse(result.isPresent());
    }

    @Test
    public void getImageBySiteInfoName_shouldReturnImageWhenLookupSucceeds() {
        SiteInformation siteInformation = new SiteInformation();
        siteInformation.setValue("123");
        when(siteInformationService.getSiteInformationByName("headerLeftImage")).thenReturn(siteInformation);

        Image expected = new Image();
        expected.setId("123");

        ImageServiceImpl serviceSpy = Mockito.spy(imageService);
        doReturn(expected).when(serviceSpy).get("123");

        Optional<Image> result = serviceSpy.getImageBySiteInfoName("headerLeftImage");

        assertTrue(result.isPresent());
        assertEquals("123", result.get().getId());
    }

    @Test
    public void getImageBySiteInfoName_shouldReturnEmptyWhenLookupThrows() {
        SiteInformation siteInformation = new SiteInformation();
        siteInformation.setValue("invalid");
        when(siteInformationService.getSiteInformationByName("invalidRef")).thenReturn(siteInformation);

        ImageServiceImpl serviceSpy = Mockito.spy(imageService);
        doThrow(new RuntimeException("lookup failed")).when(serviceSpy).get("invalid");

        Optional<Image> result = serviceSpy.getImageBySiteInfoName("invalidRef");

        assertNotNull(result);
        assertFalse(result.isPresent());
    }
}
