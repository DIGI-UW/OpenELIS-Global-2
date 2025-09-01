package org.openelisglobal.ocl;

import static org.junit.Assert.fail;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class OclInnitializerTest extends BaseWebContextSensitiveTest {
    private static final Logger log = LoggerFactory.getLogger(OclZipImporterIntegrationTest.class);

    @Autowired
    private OclZipImporter oclZipImporter;

    @Autowired
    OclImportInitializer oclImportInitializer;

    private static String oclDirPath;

    @Before
    public void setUp() {
        if (oclZipImporter == null) {
            fail("OclZipImporter bean not autowired. Check Spring configuration.");
        }
        oclDirPath = this.getClass().getClassLoader().getResource("ocl").getFile();
    }

    @Test
    public void testImportOclPackage_validZip() throws IOException {
        oclImportInitializer.performOclImport(oclDirPath);
    }

}
