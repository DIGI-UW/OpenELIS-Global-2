package org.openelisglobal.logo.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.servlet.reports.LogoUploadService;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class LogoUploadRestControllerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private ImageService imageService;

    @Mock
    private SiteInformationService siteInformationService;

    @Mock
    private LogoUploadService logoUploadService;

    @InjectMocks
    private LogoUploadRestController controller;

    private MockMvc mockMvc;

    private String previewDirPath;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ControllerSetup()).build();

        File previewDir = tempFolder.newFolder("previews");
        previewDirPath = previewDir.getAbsolutePath() + File.separator;
        when(imageService.getFullPreviewPath()).thenReturn(previewDirPath);
        when(imageService.getImageNameFilePath(any())).thenAnswer(inv -> inv.getArgument(0) + ".png");
    }

    @Test
    public void removeImage_success_returns200() throws Exception {
        SiteInformation siteInfo = new SiteInformation();
        siteInfo.setName("headerLogo");
        siteInfo.setValue("10");

        Image image = new Image();
        image.setId("10");

        when(siteInformationService.getSiteInformationByName("headerLogo")).thenReturn(siteInfo);
        when(imageService.get("10")).thenReturn(image);

        mockMvc.perform(post("/rest/logoUpload").param("logoName", "headerLogo").param("removeImage", "true"))
                .andExpect(status().isOk());

        verify(logoUploadService).removeImage(image, siteInfo);
    }

    @Test
    public void removeImage_serviceThrowsLimsRuntimeException_returns500() throws Exception {
        SiteInformation siteInfo = new SiteInformation();
        siteInfo.setName("headerLogo");
        siteInfo.setValue("10");

        Image image = new Image();
        image.setId("10");

        when(siteInformationService.getSiteInformationByName("headerLogo")).thenReturn(siteInfo);
        when(imageService.get("10")).thenReturn(image);
        doThrow(new LIMSRuntimeException("Database rollback error")).when(logoUploadService)
                .removeImage(any(Image.class), any(SiteInformation.class));

        mockMvc.perform(post("/rest/logoUpload").param("logoName", "headerLogo").param("removeImage", "true"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    public void uploadImage_success_returns200() throws Exception {
        SiteInformation siteInfo = new SiteInformation();
        siteInfo.setName("headerLogo");
        siteInfo.setValue("10");

        when(siteInformationService.getSiteInformationByName("headerLogo")).thenReturn(siteInfo);

        MockMultipartFile file = new MockMultipartFile("logoFile", "test-logo.png", "image/png",
                new byte[] { 1, 2, 3, 4, 5 });

        when(logoUploadService.validateLogo(any())).thenReturn(new byte[] { 1, 2, 3, 4, 5 });

        mockMvc.perform(multipart("/rest/logoUpload").file(file).param("logoName", "headerLogo"))
                .andExpect(status().isOk());

        verify(logoUploadService).saveImage(any(Image.class), eq(false), eq("10"), eq(siteInfo));
    }

    @Test
    public void uploadImage_serviceThrowsLimsRuntimeException_returns500() throws Exception {
        SiteInformation siteInfo = new SiteInformation();
        siteInfo.setName("headerLogo");
        siteInfo.setValue("10");

        when(siteInformationService.getSiteInformationByName("headerLogo")).thenReturn(siteInfo);

        MockMultipartFile file = new MockMultipartFile("logoFile", "test-logo.png", "image/png",
                new byte[] { 1, 2, 3, 4, 5 });

        when(logoUploadService.validateLogo(any())).thenReturn(new byte[] { 1, 2, 3, 4, 5 });
        doThrow(new LIMSRuntimeException("Database failure on save")).when(logoUploadService)
                .saveImage(any(Image.class), eq(false), eq("10"), eq(siteInfo));

        mockMvc.perform(multipart("/rest/logoUpload").file(file).param("logoName", "headerLogo"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    public void uploadImage_invalidLogo_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("logoFile", "bad.txt", "text/plain", new byte[] { 1, 2, 3 });

        doThrow(new IllegalArgumentException("Logo rejected: invalid image format")).when(logoUploadService)
                .validateLogo(any());

        mockMvc.perform(multipart("/rest/logoUpload").file(file).param("logoName", "headerLogo"))
                .andExpect(status().isBadRequest());
    }
}
