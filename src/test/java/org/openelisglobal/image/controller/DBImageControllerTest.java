package org.openelisglobal.image.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

public class DBImageControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DBImageController dbImageController;

    private ImageService imageServiceMock;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        imageServiceMock = Mockito.mock(ImageService.class);
        ReflectionTestUtils.setField(dbImageController, "imageService", imageServiceMock);
    }

    @Test
    public void getImage_shouldReturnEmptyValueWhenImageIsMissing() throws Exception {
        when(imageServiceMock.getImageBySiteInfoName("missingLogo")).thenReturn(Optional.empty());

        this.mockMvc.perform(get("/dbImage/siteInformation/missingLogo")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("missingLogo")).andExpect(jsonPath("$.value").value(""));
    }

    @Test
    public void getImage_shouldReturnDataUriWhenImageExists() throws Exception {
        byte[] imageBytes = new byte[] { 1, 2, 3, 4 };
        Image image = new Image();
        image.setImage(imageBytes);
        when(imageServiceMock.getImageBySiteInfoName("headerLeftImage")).thenReturn(Optional.of(image));

        String expectedImageValue = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(imageBytes);

        this.mockMvc.perform(get("/dbImage/siteInformation/headerLeftImage")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("headerLeftImage"))
                .andExpect(jsonPath("$.value").value(expectedImageValue));
    }
}
