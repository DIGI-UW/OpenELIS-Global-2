package org.openelisglobal.image.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;

@RunWith(MockitoJUnitRunner.class)
public class DBImageControllerTest {

    @Mock
    private ImageService imageService;

    @InjectMocks
    private DBImageController controller;

    @Test
    public void getImage_shouldReturnEmptyValueWhenImageIsMissing() {
        when(imageService.getImageBySiteInfoName("missingLogo")).thenReturn(Optional.empty());

        IdValuePair response = controller.getImage("missingLogo");

        assertEquals("missingLogo", response.getId());
        assertEquals("", response.getValue());
    }

    @Test
    public void getImage_shouldReturnDataUriWhenImageExists() {
        byte[] imageBytes = new byte[] { 1, 2, 3, 4 };
        Image image = new Image();
        image.setImage(imageBytes);

        when(imageService.getImageBySiteInfoName("headerLeftImage")).thenReturn(Optional.of(image));

        IdValuePair response = controller.getImage("headerLeftImage");
        String expectedPrefix = "data:image/jpg;base64,";

        assertEquals("headerLeftImage", response.getId());
        assertTrue(response.getValue().startsWith(expectedPrefix));
        assertEquals(expectedPrefix + Base64.getEncoder().encodeToString(imageBytes), response.getValue());
    }
}
