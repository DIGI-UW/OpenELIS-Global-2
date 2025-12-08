package org.openelisglobal.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.springframework.beans.factory.annotation.Autowired;

public class ImageServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ImageService imageService;

    private static final String basePath = new File("src/main/webapp/static/images/").getAbsolutePath().toString();
    private static final byte[] image1 = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/wcAAgMBAQEBBQAAAA==");
    private static final byte[] image2 = Base64.getDecoder()
            .decode("iPBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/wcAAgMBAQEBBQAAAA==");

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/image.xml");
    }

    @Test
    public void getFullPreviewPath() {
        String expectedPath = basePath.replaceAll("[/\\\\]+$", "");
        String actualPath = imageService.getFullPreviewPath().replaceAll("[/\\\\]+$", "");

        assertEquals(expectedPath, actualPath);
    }

    @Test
    public void getImageByDescription() {
        Image image = imageService.getImageByDescription("headerLeftImage");
        assertTrue(Arrays.equals(image1, image.getImage()));
        assertEquals("1", image.getId());
    }

    @Test
    public void getImageNameFilePath() {
        String imageString = "headerLeftImage";
        String image = imageService.getImageNameFilePath(imageString);
        assertEquals("leftLabLogo.jpg", image);
    }

    @Test
    public void testGetImageBySiteInfoName() {
        // Test for an existing image
        Optional<Image> imageOptional = imageService.getImageBySiteInfoName("testUsageSendStatus");

        Image image = imageOptional.get();
        assertEquals("1", image.getId());
        assertEquals("headerLeftImage", image.getDescription());
        Optional<Image> imageOptional2 = imageService.getImageBySiteInfoName("reportsDirectory");

        Image image2 = imageOptional2.get();
        assertEquals("2", image2.getId());
        assertEquals("headerRightImage", image2.getDescription());
    }

    @Test
    public void getDeleteImage() {
        Image image = imageService.get("1");
        imageService.delete(image);
        List<Image> images = imageService.getAll();
        assertEquals(3, images.size());

    }

    @Test
    public void deleteAll() {
        List<Image> images = imageService.getAll();
        imageService.deleteAll(images);
        List<Image> imagesd = imageService.getAll();
        assertEquals(0, imagesd.size());
    }

    @Test
    public void getAllMatching() {
        List<Image> images = imageService.getAllMatching("description", "headerRightImage");
        assertEquals(1, images.size());
    }

    @Test
    public void getAll() {
        List<Image> images = imageService.getAll();
        assertEquals("1", images.get(0).getId());
        assertEquals("2", images.get(1).getId());
        assertEquals("3", images.get(2).getId());
        assertEquals("4", images.get(3).getId());
    }

    @Test
    public void insertImage_shouldInsertImages() {
        imageService.deleteAll(imageService.getAll());
        Image image = new Image();
        image.setId("5");
        image.setDescription("BottonRightImage");
        image.setImage(image2);
        imageService.insert(image);
        List<Image> images = imageService.getAll();
        assertEquals(1, images.size());

    }

    @Test
    public void saveImage_shouldSaveImages() {
        imageService.deleteAll(imageService.getAll());
        Image image = new Image();
        image.setDescription("BottonRightImage");
        image.setImage(image2);
        Image image2 = imageService.save(image);
        List<Image> images = imageService.getAll();
        assertEquals(1, images.size());
        assertNotNull(image2);

    }

    @Test
    public void getCount() {
        int count = imageService.getCount();
        assertEquals(4, count);
    }

    @Test
    public void getCountLike() {
        int count = imageService.getCountLike("description", "headerLeftImage");
        assertEquals(1, count);
    }

    @Test
    public void getAllOrdered_shouldReturnAllImagesOrdered() {
        List<Image> images = imageService.getAllOrdered("id", false);
        assertEquals(4, images.size());
        assertEquals("headerLeftImage", images.get(0).getDescription());
        assertEquals("headerRightImage", images.get(1).getDescription());
        assertEquals("labDirectorSignature", images.get(2).getDescription());
        assertEquals("Description4", images.get(3).getDescription());
    }

    @Test
    public void getPage_shouldReturnPageOfImages() {
        List<Image> images = imageService.getPage(1);
        int expectedImages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(images.size() <= expectedImages);

    }

    @Test
    public void getAllMarchingOrdered_shouldReturnAllImagesOrdered() {
        List<Image> images = imageService.getAllMatchingOrdered("description", "headerLeftImage", "id", false);
        assertEquals(1, images.size());
        assertEquals("headerLeftImage", images.get(0).getDescription());
    }

    @Test
    public void getAllMatchingOrderedPaged_shouldReturnPageOfImages() {
        List<Image> images = imageService.getMatchingOrderedPage("description", "headerLeftImage", "id", false, 1);
        int expectedImages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(images.size() <= expectedImages);
    }

    @Test
    public void getNext_shouldReturnNextImage() {
        Image nextImage = imageService.getNext("1");
        assertEquals("2", nextImage.getId());
    }

    @Test
    public void getPrevious_shouldReturnPreviousImage() {
        Image previousImage = imageService.getPrevious("2");
        assertEquals("1", previousImage.getId());
    }

    @Test
    public void get_shouldReturnImageGiveId() {
        Image image = imageService.get("1");
        assertEquals("headerLeftImage", image.getDescription());
    }

    @Test
    public void getCountMatching_shouldReturnCountOfImages() {
        int count = imageService.getCountMatching("description", "headerLeftImage");
        assertEquals(1, count);
    }

    @Test
    public void getAllMatching_givenMaps() {
        Map<String, Object> map = Map.of("description", "headerLeftImage");
        List<Image> images = imageService.getAllMatching(map);
        assertEquals(1, images.size());
    }

    @Test
    public void getAllMatchingOrderedPaged_givenMaps() {
        Map<String, Object> map = Map.of("description", "headerLeftImage");
        List<Image> images = imageService.getMatchingOrderedPage(map, "id", false, 1);
        int expectedImages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(images.size() <= expectedImages);
    }

    @Test
    public void getAllMatchingOrdered_givenMaps() {
        Map<String, Object> map = Map.of("description", "headerLeftImage");
        List<Image> images = imageService.getAllMatchingOrdered(map, "id", false);
        assertEquals(1, images.size());
        assertEquals("headerLeftImage", images.get(0).getDescription());
    }
}