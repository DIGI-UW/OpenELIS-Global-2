package org.openelisglobal.testTrailer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.testtrailer.service.TestTrailerService;
import org.openelisglobal.testtrailer.valueholder.TestTrailer;
import org.springframework.beans.factory.annotation.Autowired;

public class TestTrailerServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestTrailerService testTrailerService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-trailer.xml");
    }

    @Test
    public void getDatabaseData() {
        List<TestTrailer> testTrailers = testTrailerService.getAll();

        Assert.assertFalse("The test_trailer table should not be empty!", testTrailers.isEmpty());

        for (TestTrailer testTrailer : testTrailers) {
            Assert.assertNotNull("Test trailer name should not be null", testTrailer.getTestTrailerName());
        }
    }

    @Test
    public void getTestTrailerByName() {
        TestTrailer testTrailer = new TestTrailer();
        testTrailer.setTestTrailerName("Trailer Name 1");
        TestTrailer testTrailerByName = testTrailerService.getTestTrailerByName(testTrailer);
        assertEquals("Trailer Name 1", testTrailerByName.getTestTrailerName());
        assertEquals("Description 1", testTrailerByName.getDescription());
    }

    @Test
    public void getTestTrailers() {
        TestTrailer testTrailer = testTrailerService.get("1");
        String testTrailerName = testTrailer.getTestTrailerName();
        List<TestTrailer> testTrailers = testTrailerService.getTestTrailers(testTrailerName);
        assertTrue(testTrailers.size() > 0);
    }

    @Test
    public void getTotalTestTrailerCount() {
        Integer totalTestTrailerCount = testTrailerService.getTotalTestTrailerCount();
        assertEquals(3, totalTestTrailerCount.intValue());
    }

    @Test
    public void getPageOfTestTrailers() {
        List<TestTrailer> testTrailers = testTrailerService.getPageOfTestTrailers(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testTrailers.size() <= expectedPages);
    }

    @Test
    public void getTestTrailerById() {
        TestTrailer testTrailer = testTrailerService.get("1");
        assertEquals("Trailer Name 1", testTrailer.getTestTrailerName());
    }

    @Test
    public void getAllTestTrailers() {
        List<TestTrailer> testTrailers = testTrailerService.getAllTestTrailers();
        assertEquals(3, testTrailers.size());
    }

}
