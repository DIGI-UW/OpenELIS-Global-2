package org.openelisglobal.reports.service.send.sample;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.send.sample.valueholder.SampleTransmissionSequence;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleTransmissionSequenceServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleTransmissionSequenceService transmissionSequenceService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-transmission-sequence.xml");
    }

    @Test
    public void getSampleTransmissionSequenceById_shouldReturnCorrectSequence() {
        SampleTransmissionSequence sequence = transmissionSequenceService.get("9001");

        Assert.assertNotNull("Sequence should not be null", sequence);
        Assert.assertEquals("9001", sequence.getId());
        Assert.assertNotNull("Last updated should be populated", sequence.getLastupdated());
        Assert.assertEquals("2024-01-15 08:30:00.0", sequence.getLastupdated().toString());
    }

    @Test(expected = org.hibernate.ObjectNotFoundException.class)
    public void getNonExistentSequence_shouldThrowException() {
        transmissionSequenceService.get("9999");
    }

    @Test
    public void getAllSequences_shouldReturnAllSeededSequences() {
        List<SampleTransmissionSequence> sequences = transmissionSequenceService.getAll();

        Assert.assertNotNull(sequences);
        Assert.assertEquals("Should retrieve exactly 2 seeded sequences", 2, sequences.size());

        boolean has9001 = sequences.stream().anyMatch(s -> "9001".equals(s.getId()));
        boolean has9002 = sequences.stream().anyMatch(s -> "9002".equals(s.getId()));

        Assert.assertTrue("Should contain sequence 9001", has9001);
        Assert.assertTrue("Should contain sequence 9002", has9002);
    }

    @Test
    public void insert_shouldGenerateSequenceIdAndIncreaseSize() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "hl7_transmission_sequence" });
        Assert.assertEquals(0, transmissionSequenceService.getAll().size());

        SampleTransmissionSequence newSequence = new SampleTransmissionSequence();
        String generatedId = transmissionSequenceService.insert(newSequence);

        Assert.assertNotNull("Generated ID should not be null", generatedId);
        Assert.assertEquals("Database size should increase to 1", 1, transmissionSequenceService.getAll().size());

        SampleTransmissionSequence retrieved = transmissionSequenceService.get(generatedId);
        Assert.assertNotNull("Should be able to retrieve the newly inserted sequence", retrieved);
        Assert.assertEquals(generatedId, retrieved.getId());
        Assert.assertNotNull("Hibernate should populate the lastupdated timestamp", retrieved.getLastupdated());
    }

    @Test
    public void update_shouldPersistChanges() {
        SampleTransmissionSequence sequence = transmissionSequenceService.get("9002");
        Assert.assertNotNull("Sequence should exist", sequence);

        transmissionSequenceService.update(sequence);

        SampleTransmissionSequence updated = transmissionSequenceService.get("9002");
        Assert.assertNotNull("Updated sequence should still be retrievable", updated);
        Assert.assertEquals("9002", updated.getId());
    }

    @Test(expected = org.hibernate.ObjectNotFoundException.class)
    public void delete_shouldRemoveSequence() {
        Assert.assertEquals("Initial size should be 2", 2, transmissionSequenceService.getAll().size());

        SampleTransmissionSequence sequence = transmissionSequenceService.get("9001");
        Assert.assertNotNull("Sequence should exist before deletion", sequence);

        transmissionSequenceService.delete(sequence);

        Assert.assertEquals("Size should be 1 after deletion", 1, transmissionSequenceService.getAll().size());

        transmissionSequenceService.get("9001");
    }

    @Test
    public void save_shouldInsertNewSequenceIfIdIsNull() {
        SampleTransmissionSequence sequence = new SampleTransmissionSequence();
        SampleTransmissionSequence saved = transmissionSequenceService.save(sequence);

        Assert.assertNotNull("Saved sequence should not be null", saved);
        Assert.assertNotNull("Generated ID should not be null", saved.getId());
        Assert.assertEquals("Size should increase to 3", 3, transmissionSequenceService.getCount().intValue());
    }

    @Test
    public void getCount_shouldReturnTotalNumberOfSequences() {
        Integer count = transmissionSequenceService.getCount();
        Assert.assertEquals("Should count 2 seeded sequences", Integer.valueOf(2), count);
    }

    @Test
    public void getNextAndHasNext_shouldReturnNextSequence() {
        Assert.assertTrue("Should have next sequence", transmissionSequenceService.hasNext("9001"));
        Assert.assertFalse("Should not have next sequence after last", transmissionSequenceService.hasNext("9002"));

        SampleTransmissionSequence next = transmissionSequenceService.getNext("9001");
        Assert.assertNotNull("Next sequence should not be null", next);
        Assert.assertEquals("9002", next.getId());
    }

    @Test
    public void getPreviousAndHasPrevious_shouldReturnPreviousSequence() {
        Assert.assertTrue("Should have previous sequence", transmissionSequenceService.hasPrevious("9002"));
        Assert.assertFalse("Should not have previous sequence before first",
                transmissionSequenceService.hasPrevious("9001"));

        SampleTransmissionSequence previous = transmissionSequenceService.getPrevious("9002");
        Assert.assertNotNull("Previous sequence should not be null", previous);
        Assert.assertEquals("9001", previous.getId());
    }
}
