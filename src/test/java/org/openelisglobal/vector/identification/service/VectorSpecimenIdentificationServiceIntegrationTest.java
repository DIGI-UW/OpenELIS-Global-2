package org.openelisglobal.vector.identification.service;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.BulkIdentifyRequest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.BulkIdentifyResult;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.IdentificationRequest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.IdentificationResult;
import org.openelisglobal.vector.identification.valueholder.VectorSpecimenIdentification;
import org.springframework.beans.factory.annotation.Autowired;

public class VectorSpecimenIdentificationServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private VectorSpecimenIdentificationService identificationService;

    @Autowired
    private DataSource dataSource;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/vector-specimen-identification.xml");
        seedDictionaryData();
    }

    @After
    public void tearDown() throws Exception {
        cleanDictionaryData();
    }

    @Test
    public void getBySampleItemId_preSeededItem_returnsIdentification() {
        Optional<VectorSpecimenIdentification> result = identificationService.getBySampleItemId(803L);

        assertTrue(result.isPresent());
        assertEquals(Long.valueOf(800L), result.get().getVectorSpeciesId());
        assertEquals("MORPHOLOGICAL", result.get().getIdentificationMethod());
        assertEquals("CONFIRMED", result.get().getConfidence());
    }

    @Test
    public void getBySampleItemId_unidentifiedItem_returnsEmpty() {
        Optional<VectorSpecimenIdentification> result = identificationService.getBySampleItemId(801L);

        assertFalse(result.isPresent());
    }

    @Test
    public void getBySampleId_onePreSeeded_returnsOneRow() {
        List<VectorSpecimenIdentification> results = identificationService.getBySampleId(800L);

        assertEquals(1, results.size());
        assertEquals(Long.valueOf(803L), results.get(0).getSampleItemId());
    }

    @Test
    public void countBySampleId_onePreSeeded_returnsOne() {
        long count = identificationService.countBySampleId(800L);

        assertEquals(1L, count);
    }

    @Test
    public void identify_newSpecimen_persistsRowAndAdvancesStatusToInProgress() {
        IdentificationRequest req = buildRequest(801L);

        IdentificationResult result = identificationService.identify(req, "1");

        assertNotNull(result.getIdentification());
        assertEquals(Long.valueOf(801L), result.getIdentification().getSampleItemId());
        assertEquals(VectorSpecimenIdentificationServiceImpl.STATUS_IDENTIFICATION_IN_PROGRESS,
                result.getNewSampleIdentificationStatus());
    }

    @Test
    public void identify_lastUnidentifiedSpecimen_completesStatus() {
        identificationService.identify(buildRequest(801L), "1");

        IdentificationResult last = identificationService.identify(buildRequest(802L), "1");

        assertEquals(VectorSpecimenIdentificationServiceImpl.STATUS_COMPLETE, last.getNewSampleIdentificationStatus());
    }

    @Test
    public void identify_existingRecord_updatesInPlace() {
        IdentificationRequest req = buildRequest(803L);
        req.setConfidence("PRESUMPTIVE");

        IdentificationResult result = identificationService.identify(req, "1");

        assertEquals("PRESUMPTIVE", result.getIdentification().getConfidence());
        assertEquals(1L, identificationService.countBySampleId(800L));
    }

    @Test
    public void bulkIdentify_twoUnidentifiedSpecimens_persistsBothAndUpdatesStatus() {
        BulkIdentifyRequest bulk = new BulkIdentifyRequest();
        bulk.setSampleItemIds(List.of(801L, 802L));
        bulk.setVectorSpeciesId(800L);
        bulk.setIdentificationMethod("MOLECULAR");
        bulk.setConfidence("CONFIRMED");

        BulkIdentifyResult result = identificationService.bulkIdentify(bulk, "1");

        assertEquals(2, result.getIdentifications().size());
        assertEquals(VectorSpecimenIdentificationServiceImpl.STATUS_COMPLETE,
                result.getSampleIdentificationStatuses().get(800L));
        assertEquals(3L, identificationService.countBySampleId(800L));
    }

    @Test
    public void identify_invalidPhysiologicalState_throwsIllegalArgument() {
        IdentificationRequest req = buildRequest(801L);
        req.setPhysiologicalState("NOT_A_REAL_STATE");

        try {
            identificationService.identify(req, "1");
            fail("Expected IllegalArgumentException for unknown physiological state");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("NOT_A_REAL_STATE"));
        }
    }

    private IdentificationRequest buildRequest(long sampleItemId) {
        IdentificationRequest req = new IdentificationRequest();
        req.setSampleItemId(sampleItemId);
        req.setVectorSpeciesId(800L);
        req.setIdentificationMethod("MORPHOLOGICAL");
        req.setConfidence("CONFIRMED");
        return req;
    }

    private void seedDictionaryData() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn
                    .prepareStatement("INSERT INTO clinlims.dictionary_category (id, name, description, local_abbrev)"
                            + " VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING")) {
                ps.setLong(1, 8500L);
                ps.setString(2, "vecPhysiologicalState");
                ps.setString(3, "Vector Physiological State");
                ps.setString(4, "vecPhys");
                ps.executeUpdate();

                ps.setLong(1, 8501L);
                ps.setString(2, "vecLifecycleStages");
                ps.setString(3, "Vector Lifecycle Stages");
                ps.setString(4, "vecLife");
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO clinlims.dictionary"
                    + " (id, dictionary_category_id, dict_entry, local_abbrev, is_active)"
                    + " VALUES (?, ?, ?, ?, 'Y') ON CONFLICT DO NOTHING")) {
                insertDict(ps, 85001L, 8500L, "Blood Fed", "BLOOD_FED");
                insertDict(ps, 85002L, 8500L, "Nulliparous", "NULLIPAROUS");
                insertDict(ps, 85003L, 8500L, "Parous", "PAROUS");
                insertDict(ps, 85004L, 8500L, "Unknown", "PHYS_UNKNOWN");
                insertDict(ps, 85010L, 8501L, "Adult", "ADULT");
                insertDict(ps, 85011L, 8501L, "Larva", "LARVA");
                insertDict(ps, 85012L, 8501L, "Pupa", "PUPA");
                insertDict(ps, 85013L, 8501L, "Unknown", "UNKNOWN");
            }
        }
    }

    private void insertDict(PreparedStatement ps, long id, long catId, String entry, String abbrev) throws Exception {
        ps.setLong(1, id);
        ps.setLong(2, catId);
        ps.setString(3, entry);
        ps.setString(4, abbrev);
        ps.executeUpdate();
    }

    private void cleanDictionaryData() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM clinlims.dictionary WHERE id >= 85000")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn
                    .prepareStatement("DELETE FROM clinlims.dictionary_category WHERE id >= 8500")) {
                ps.executeUpdate();
            }
        }
    }
}
