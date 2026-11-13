package org.openelisglobal.fhir.search.bundleProviders;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

/**
 * Every FHIR search accepted the client's {@code _count} as the page size with
 * no upper bound, so one request could load a whole table (SCAN-084).
 */
/*
 * The provider reads its default page size from the application configuration
 * at class load.
 */
public class BaseFhirBundleProviderPageSizeTest extends BaseWebContextSensitiveTest {

    private final BaseFhirBundleProvider<String, Patient> provider = new BaseFhirBundleProvider<>() {
        @Override
        protected List<String> loadEntities(int offset, int pageSize) {
            return List.of();
        }

        @Override
        protected long countEntities() {
            return 0;
        }

        @Override
        protected Patient transformEntity(String entity) {
            return new Patient();
        }
    };

    @Test
    public void aCountBeyondTheMaximum_isAnsweredWithTheMaximum() {
        provider.setCurrentPage(0, 1_000_000);

        assertEquals(Integer.valueOf(BaseFhirBundleProvider.MAX_PAGE_SIZE), provider.getCurrentPageSize());
    }

    @Test
    public void aCountWithinTheMaximum_isHonoured() {
        provider.setCurrentPage(0, 20);

        assertEquals(Integer.valueOf(20), provider.getCurrentPageSize());
    }
}
