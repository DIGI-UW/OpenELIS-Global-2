package org.openelisglobal.sample.action.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests for {@code resolveOrCreateSamplingSiteId}. */
public class SamplePatientUpdateDataSamplingSiteTest extends BaseWebContextSensitiveTest {

    private static final String METHOD = "resolveOrCreateSamplingSiteId";

    private String invoke(String siteId, String siteName, String siteCode, String siteType) {
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        return ReflectionTestUtils.invokeMethod(updateData, METHOD, siteId, siteName, siteCode, siteType);
    }

    @Test
    public void existingSiteId_returnedUnchanged_whenNoMatchingSiteRow() {
        String result = invoke("42", "Some Site", "SOME-CODE", "Water");

        assertEquals("an already-selected site's id must pass through unchanged", "42", result);
    }

    @Test
    public void existingSiteId_editedNameCodeType_persistedToTheSamplingSiteRow() {
        VectorSamplingSiteService siteService = SpringContext.getBean(VectorSamplingSiteService.class);
        String originalCode = "EDIT-ORIG-" + System.identityHashCode(this);
        VectorSamplingSite existing = new VectorSamplingSite();
        existing.setCode(originalCode);
        existing.setName("Original Name");
        existing.setType("Water Source");
        existing.setActive(true);
        existing.setSource("LOCAL");
        existing.setSysUserId("1");
        Integer existingId = siteService.insert(existing);

        String newCode = "EDIT-NEW-" + System.identityHashCode(this);
        String result = invoke(String.valueOf(existingId), "Renamed Site", newCode, "Vector Trap");

        assertEquals("the site id must pass through unchanged — this is an in-place edit, not a new site",
                String.valueOf(existingId), result);
        VectorSamplingSite updated = siteService.get(existingId);
        assertEquals("Renamed Site", updated.getName());
        assertEquals(newCode, updated.getCode());
        assertEquals("Vector Trap", updated.getType());
    }

    @Test
    public void existingSiteId_unchangedFields_doesNotIssueAnUpdate() {
        VectorSamplingSiteService siteService = SpringContext.getBean(VectorSamplingSiteService.class);
        String code = "NOCHANGE-" + System.identityHashCode(this);
        VectorSamplingSite existing = new VectorSamplingSite();
        existing.setCode(code);
        existing.setName("Unchanged Site");
        existing.setType("Water Source");
        existing.setActive(true);
        existing.setSource("LOCAL");
        existing.setSysUserId("1");
        Integer existingId = siteService.insert(existing);

        String result = invoke(String.valueOf(existingId), "Unchanged Site", code, "Water Source");

        assertEquals(String.valueOf(existingId), result);
        VectorSamplingSite unchanged = siteService.get(existingId);
        assertEquals("Unchanged Site", unchanged.getName());
        assertEquals(code, unchanged.getCode());
        assertEquals("Water Source", unchanged.getType());
    }

    @Test
    public void newSite_noExistingCode_createsSiteAndReturnsNewId() {
        VectorSamplingSiteService siteService = SpringContext.getBean(VectorSamplingSiteService.class);
        String code = "NEWSITE-" + System.identityHashCode(this);

        String result = invoke("", "Brand New Site", code, "Water Source");

        assertNotNull("a new site should be created and its id returned", result);
        VectorSamplingSite created = siteService.getByCode(code);
        assertNotNull("the deferred-create site must be persisted under the generated code", created);
        assertEquals(result, String.valueOf(created.getId()));
        assertEquals("Brand New Site", created.getName());
        assertEquals("Water Source", created.getType());
        assertEquals(Boolean.TRUE, created.getActive());
    }

    @Test
    public void newSite_existingCode_dedupsToExistingSiteInsteadOfCreatingDuplicate() {
        VectorSamplingSiteService siteService = SpringContext.getBean(VectorSamplingSiteService.class);
        String code = "DEDUP-" + System.identityHashCode(this);
        VectorSamplingSite existing = new VectorSamplingSite();
        existing.setCode(code);
        existing.setName("Already Exists");
        existing.setActive(true);
        existing.setSource("LOCAL");
        existing.setSysUserId("1");
        Integer existingId = siteService.insert(existing);

        String result = invoke("", "Typed Again With Same Code", code, "Water Source");

        assertEquals("resolving a new-site payload whose code already exists must reuse that site's id, "
                + "not create a duplicate", String.valueOf(existingId), result);
    }

    @Test
    public void blankSiteIdAndBlankCode_returnsSiteIdUnchanged() {
        String result = invoke("", "Name Only, No Code Yet", "", "Water");

        assertEquals("without a code there is nothing to dedup or create against; "
                + "the (blank) siteId must pass through unchanged", "", result);
    }
}
