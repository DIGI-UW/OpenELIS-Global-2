package org.openelisglobal.logo.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
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
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private ImageService imageService;
    @Mock
    private SiteInformationService siteInformationService;
    @Mock
    private LogoUploadService logoUploadService;
    @InjectMocks
    private LogoUploadRestController controller;

    private MockMvc mockMvc;
    private SiteInformation logoInformation;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        logoInformation = new SiteInformation();
        logoInformation.setValue("");
        when(imageService.getFullPreviewPath())
                .thenReturn(temporaryFolder.getRoot().getAbsolutePath() + File.separator);
        when(imageService.getImageNameFilePath("headerLogo")).thenReturn("logo.png");
        when(siteInformationService.getSiteInformationByName("headerLogo")).thenReturn(logoInformation);
    }

    @Test
    public void uploadFailureShouldReturnInternalServerError() throws Exception {
        doThrow(new LIMSRuntimeException("Save failed")).when(logoUploadService).saveImage(any(Image.class), eq(true),
                eq(""), same(logoInformation));

        mockMvc.perform(multipart("/rest/logoUpload").file(logoFile()).param("logoName", "headerLogo"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error").value("saveFailed"));
    }

    @Test
    public void removeFailureShouldReturnInternalServerError() throws Exception {
        Image image = prepareExistingImage();
        doThrow(new LIMSRuntimeException("Remove failed")).when(logoUploadService).removeImage(image, logoInformation);

        mockMvc.perform(post("/rest/logoUpload").param("logoName", "headerLogo").param("removeImage", "true"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error").value("saveFailed"));
    }

    @Test
    public void successfulUploadShouldReturnOk() throws Exception {
        mockMvc.perform(multipart("/rest/logoUpload").file(logoFile()).param("logoName", "headerLogo"))
                .andExpect(status().isOk());

        verify(logoUploadService).saveImage(any(Image.class), eq(true), eq(""), same(logoInformation));
    }

    @Test
    public void successfulRemoveShouldReturnOk() throws Exception {
        Image image = prepareExistingImage();

        mockMvc.perform(post("/rest/logoUpload").param("logoName", "headerLogo").param("removeImage", "true"))
                .andExpect(status().isOk());

        verify(logoUploadService).removeImage(image, logoInformation);
    }

    private Image prepareExistingImage() throws Exception {
        temporaryFolder.newFile("logo.png");
        logoInformation.setValue("123");
        Image image = new Image();
        when(imageService.get("123")).thenReturn(image);
        return image;
    }

    private MockMultipartFile logoFile() {
        return new MockMultipartFile("logoFile", "logo.png", "image/png", new byte[] { 1, 2, 3 });
    }
}
