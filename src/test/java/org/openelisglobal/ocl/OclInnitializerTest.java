package org.openelisglobal.ocl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.service.TestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class OclInnitializerTest extends BaseWebContextSensitiveTest {
    private static final Logger log = LoggerFactory.getLogger(OclInnitializerTest.class);

    @Autowired
    private OclZipImporter oclZipImporter;

    @Autowired
    private OclImportInitializer oclImportInitializer;

    @Autowired
    TestService testService;

    private static String oclDirPath;
    private static String sampleType = "Whole Blood";

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/ocl-import.xml");
        executeDataSetWithStateManagement("testdata/type-of-testresult.xml");
        if (oclZipImporter == null) {
            fail("OclZipImporter bean not autowired. Check Spring configuration.");
        }
        oclDirPath = this.getClass().getClassLoader().getResource("ocl").getFile();
    }

    @Test
    public void testImportOclPackage_validZip() throws IOException {
        java.io.File tempFile = java.io.File.createTempFile("ocl_imported", ".flag");

        org.openelisglobal.test.valueholder.Test test = testService.getTestByLocalizedName("TEST C en", Locale.ENGLISH);
        assertNull(test);
        oclImportInitializer.performOclImport(oclDirPath, tempFile.getAbsolutePath());
        test = testService.getTestByLocalizedName("TEST C en", Locale.ENGLISH);
        System.out.println("Test Result : " + testService.getResultType(test));
        assertNotNull(test);
    }
}
