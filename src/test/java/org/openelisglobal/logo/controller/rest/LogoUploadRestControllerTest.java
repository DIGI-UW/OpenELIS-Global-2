package org.openelisglobal.logo.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.servlet.reports.LogoUploadService;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

/**
 * A failed logo save must not answer 200 OK: the admin is told the logo changed
 * while the database still holds the old one.
 */
@RunWith(MockitoJUnitRunner.class)
public class LogoUploadRestControllerTest {

    @Rule
    public TemporaryFolder previewFolder = new TemporaryFolder();

    @Mock
    private ImageService imageService;
    @Mock
    private SiteInformationService siteInformationService;
    @Mock
    private LogoUploadService logoUploadService;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        String previewPath = previewFolder.getRoot().getAbsolutePath() + File.separator;
        when(imageService.getFullPreviewPath()).thenReturn(previewPath);
        when(imageService.getImageNameFilePath(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0) + ".png");

        LogoUploadRestController controller = new LogoUploadRestController();
        ReflectionTestUtils.setField(controller, "imageService", imageService);
        ReflectionTestUtils.setField(controller, "siteInformationService", siteInformationService);
        ReflectionTestUtils.setField(controller, "logoUploadService", logoUploadService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void upload_whenDatabaseSaveFails_answers500InsteadOf200() throws Exception {
        SiteInformation logoInformation = new SiteInformation();
        logoInformation.setValue("");
        when(siteInformationService.getSiteInformationByName("siteLogo")).thenReturn(logoInformation);
        when(logoUploadService.validateLogo(any(MultipartFile.class))).thenReturn(new byte[] { 1, 2, 3 });
        doThrow(new LIMSRuntimeException("simulated database failure")).when(logoUploadService)
                .saveImage(any(Image.class), anyBoolean(), anyString(), eq(logoInformation));

        MockMultipartFile logoFile = new MockMultipartFile("logoFile", "logo.png", "image/png",
                new byte[] { 1, 2, 3, 4 });

        mockMvc.perform(multipart("/rest/logoUpload").file(logoFile).param("logoName", "siteLogo"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error").value("saveFailed"))
                .andExpect(jsonPath("$.message").value("The change was not saved."));
    }

    @Test
    public void remove_whenDatabaseDeleteFails_answers500InsteadOf200() throws Exception {
        SiteInformation logoInformation = new SiteInformation();
        logoInformation.setValue("42");
        when(siteInformationService.getSiteInformationByName("siteLogo")).thenReturn(logoInformation);
        Image image = new Image();
        when(imageService.get("42")).thenReturn(image);
        doThrow(new LIMSRuntimeException("simulated database failure")).when(logoUploadService).removeImage(eq(image),
                eq(logoInformation));

        mockMvc.perform(multipart("/rest/logoUpload").param("logoName", "siteLogo").param("removeImage", "true"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error").value("saveFailed"))
                .andExpect(jsonPath("$.message").value("The change was not saved."));
    }
}
