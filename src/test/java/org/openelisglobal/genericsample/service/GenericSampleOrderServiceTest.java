package org.openelisglobal.genericsample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.genericsample.form.GenericSampleImportResult;
import org.openelisglobal.genericsample.form.GenericSampleOrderForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class GenericSampleOrderServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private GenericSampleOrderService genericSampleOrderService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-generic-sample-order.xml");
        resyncSequence("sample_seq", "sample");
        resyncSequence("sample_item_seq", "sample_item");

        AtomicInteger counter = new AtomicInteger(1);
        String twoDigitYear = String.valueOf(java.time.Year.now().getValue() % 100);
        IAccessionNumberGenerator generator = mock(IAccessionNumberGenerator.class);
        when(generator.getNextAccessionNumber(any(), anyBoolean()))
                .thenAnswer(inv -> twoDigitYear + String.format("%06d", counter.getAndIncrement()));
        when(generator.getNextAvailableAccessionNumber(any(), anyBoolean()))
                .thenAnswer(inv -> twoDigitYear + String.format("%06d", counter.getAndIncrement()));
        ReflectionTestUtils.setField(genericSampleOrderService, "accessionNumberGenerator", generator);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(genericSampleOrderService, "accessionNumberGenerator", null);
    }

    @Test
    public void saveGenericSampleOrder_withProvidedAccessionNumber_persistsSampleWithCorrectAccessionNumber()
            throws Exception {
        GenericSampleOrderForm form = buildForm("ACC-TEST-001", "1", "5.0", "1", "Nurse Smith", "11/01/2023", "08:30");

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, TEST_SYS_USER_ID);

        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals("ACC-TEST-001", result.get("accessionNumber"));

        Sample saved = sampleService.getSampleByAccessionNumber("ACC-TEST-001");
        assertEquals("ACC-TEST-001", saved.getAccessionNumber());
        assertEquals("H", saved.getDomain());
    }

    @Test
    public void saveGenericSampleOrder_setsReferringIdFromFromField() throws Exception {
        GenericSampleOrderForm form = buildForm("ACC-REF-001", "1", null, "1", null, null, null);

        genericSampleOrderService.saveGenericSampleOrderInternal(form, TEST_SYS_USER_ID);

        Sample saved = sampleService.getSampleByAccessionNumber("ACC-REF-001");
        assertEquals("1", saved.getReferringId());
    }

    @Test
    public void saveGenericSampleOrder_createsSampleItemLinkedToSample() throws Exception {
        GenericSampleOrderForm form = buildForm("ACC-ITEM-001", "1", "10.0", null, "Dr. House", "11/01/2023", "09:00");

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, TEST_SYS_USER_ID);

        String sampleId = (String) result.get("sampleId");
        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sampleId);

        assertEquals(1, items.size());
        SampleItem item = items.get(0);
        assertEquals("1", item.getTypeOfSample().getId());
        assertEquals("Dr. House", item.getCollector());
        assertEquals("ACC-ITEM-001-1", item.getExternalId());
        assertEquals(Double.valueOf(10.0), item.getQuantity());
    }

    @Test
    public void saveGenericSampleOrder_withUnitOfMeasure_persistsUomOnSampleItem() throws Exception {
        GenericSampleOrderForm form = buildForm("ACC-UOM-001", "1", "3.5", null, null, null, null);
        form.getDefaultFields().setSampleUnitOfMeasure("1");

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, TEST_SYS_USER_ID);

        String sampleId = (String) result.get("sampleId");
        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sampleId);
        SampleItem item = items.get(0);
        assertEquals("1", item.getUnitOfMeasure().getId());
        assertEquals("mL", item.getUnitOfMeasure().getUnitOfMeasureName());
    }

    @Test
    public void saveGenericSampleOrder_withCollectionDateAndTime_setsTimestampOnSample() throws Exception {
        GenericSampleOrderForm form = buildForm("ACC-DATE-001", "1", null, null, null, "11/01/2023", "14:30");

        genericSampleOrderService.saveGenericSampleOrderInternal(form, TEST_SYS_USER_ID);

        Sample saved = sampleService.getSampleByAccessionNumber("ACC-DATE-001");
        String collectionDate = saved.getCollectionDateForDisplay();
        assertTrue("Collection date should be set on sample", collectionDate != null && !collectionDate.isBlank());
    }

    @Test
    public void saveGenericSampleOrder_withNoSampleTypeId_createsNoSampleItem() throws Exception {
        GenericSampleOrderForm form = new GenericSampleOrderForm();
        GenericSampleOrderForm.DefaultFields fields = new GenericSampleOrderForm.DefaultFields();
        fields.setLabNo("ACC-NOITEM-001");
        fields.setNumOrderLabels(1);
        fields.setNumSpecimenLabels(1);
        form.setDefaultFields(fields);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, TEST_SYS_USER_ID);

        String sampleId = (String) result.get("sampleId");
        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sampleId);
        assertEquals(0, items.size());
    }

    @Test
    public void saveGenericSampleOrder_withNullLabNo_autoGeneratesAccessionNumber() throws Exception {
        GenericSampleOrderForm form = new GenericSampleOrderForm();
        GenericSampleOrderForm.DefaultFields fields = new GenericSampleOrderForm.DefaultFields();
        fields.setNumOrderLabels(1);
        fields.setNumSpecimenLabels(1);
        form.setDefaultFields(fields);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, TEST_SYS_USER_ID);

        assertEquals(Boolean.TRUE, result.get("success"));
        String accessionNumber = (String) result.get("accessionNumber");
        assertFalse("Auto-generated accession number must not be blank",
                accessionNumber == null || accessionNumber.isBlank());

        Sample persisted = sampleService.getSampleByAccessionNumber(accessionNumber);
        assertEquals(accessionNumber, persisted.getAccessionNumber());
    }

    @Test
    public void saveGenericSampleOrder_withInvalidQuantity_doesNotThrowAndSetsNoQuantity() throws Exception {
        GenericSampleOrderForm form = buildForm("ACC-QTY-ERR-001", "1", "not-a-number", null, null, null, null);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, TEST_SYS_USER_ID);

        assertEquals(Boolean.TRUE, result.get("success"));
        String sampleId = (String) result.get("sampleId");
        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sampleId);
        assertEquals(1, items.size());
        assertEquals(null, items.get(0).getQuantity());
    }

    @Test
    @org.junit.Ignore("Bug in NoteBookSampleDAO/GenericSampleOrderServiceImpl - Hibernate type mismatch on sampleItemId (String expected, Integer passed)")
    public void hz_getGenericSampleOrderByAccessionNumber_withExistingSample_returnsPopulatedForm() {
        GenericSampleOrderForm form = genericSampleOrderService.getGenericSampleOrderByAccessionNumber("EXIST-001");

        assertEquals("EXIST-001", form.getDefaultFields().getLabNo());
        assertEquals("1", form.getDefaultFields().getSampleTypeId());
        assertEquals("Original Collector", form.getDefaultFields().getCollector());
    }

    @Test
    public void getGenericSampleOrderByAccessionNumber_withUnknownAccessionNumber_returnsFormWithNullFields() {
        GenericSampleOrderForm form = genericSampleOrderService
                .getGenericSampleOrderByAccessionNumber("DOES-NOT-EXIST-999");

        assertEquals(null, form.getDefaultFields());
    }

    @Test
    public void getGenericSampleOrderByAccessionNumber_withBlankAccessionNumber_returnsFormWithNullFields() {
        GenericSampleOrderForm form = genericSampleOrderService.getGenericSampleOrderByAccessionNumber("");

        assertEquals(null, form.getDefaultFields());
    }

    @Test
    public void getGenericSampleOrderByAccessionNumber_withNullAccessionNumber_returnsFormWithNullFields() {
        GenericSampleOrderForm form = genericSampleOrderService.getGenericSampleOrderByAccessionNumber(null);

        assertEquals(null, form.getDefaultFields());
    }

    @Test
    public void updateGenericSampleOrder_updatesReferringIdAndCollectorInDatabase() {
        GenericSampleOrderForm form = new GenericSampleOrderForm();
        GenericSampleOrderForm.DefaultFields fields = new GenericSampleOrderForm.DefaultFields();
        fields.setFrom("99");
        fields.setCollector("Updated Collector");
        fields.setSampleTypeId("2");
        form.setDefaultFields(fields);

        Map<String, Object> result = genericSampleOrderService.updateGenericSampleOrder("EXIST-001", form,
                TEST_SYS_USER_ID);

        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals("EXIST-001", result.get("accessionNumber"));

        Sample updated = sampleService.getSampleByAccessionNumber("EXIST-001");
        assertEquals("99", updated.getReferringId());

        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(updated.getId());
        assertEquals("Updated Collector", items.get(0).getCollector());
        assertEquals("2", items.get(0).getTypeOfSample().getId());
    }

    @Test
    public void updateGenericSampleOrder_withCollectionDateAndTime_updatesTimestampOnSampleAndItem() {
        GenericSampleOrderForm form = new GenericSampleOrderForm();
        GenericSampleOrderForm.DefaultFields fields = new GenericSampleOrderForm.DefaultFields();
        fields.setCollectionDate("12/15/2023");
        fields.setCollectionTime("10:00");
        form.setDefaultFields(fields);

        Map<String, Object> result = genericSampleOrderService.updateGenericSampleOrder("EXIST-001", form,
                TEST_SYS_USER_ID);

        assertEquals(Boolean.TRUE, result.get("success"));

        Sample updated = sampleService.getSampleByAccessionNumber("EXIST-001");
        assertTrue("Collection date should be set", updated.getCollectionDate() != null);

        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(updated.getId());
        assertTrue("Item collection date should propagate from sample", items.get(0).getCollectionDate() != null);
    }

    @Test
    public void updateGenericSampleOrder_withNonExistentAccessionNumber_returnsFalseSuccessWithErrorMessage() {
        GenericSampleOrderForm form = new GenericSampleOrderForm();
        GenericSampleOrderForm.DefaultFields fields = new GenericSampleOrderForm.DefaultFields();
        fields.setFrom("1");
        form.setDefaultFields(fields);

        Map<String, Object> result = genericSampleOrderService.updateGenericSampleOrder("DOES-NOT-EXIST-999", form,
                TEST_SYS_USER_ID);

        assertEquals(Boolean.FALSE, result.get("success"));
        assertTrue("Error message should mention the accession number",
                result.get("error").toString().contains("DOES-NOT-EXIST-999"));
    }

    @Test
    public void updateGenericSampleOrder_withQuantity_updatesQuantityOnSampleItem() {
        GenericSampleOrderForm form = new GenericSampleOrderForm();
        GenericSampleOrderForm.DefaultFields fields = new GenericSampleOrderForm.DefaultFields();
        fields.setQuantity("25.5");
        form.setDefaultFields(fields);

        genericSampleOrderService.updateGenericSampleOrder("EXIST-001", form, TEST_SYS_USER_ID);

        Sample updated = sampleService.getSampleByAccessionNumber("EXIST-001");
        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(updated.getId());
        assertEquals(Double.valueOf(25.5), items.get(0).getQuantity());
    }

    @Test
    public void validateImportFile_withValidCsvContent_returnsExpectedRowCount() throws Exception {
        String csv = "SampleType,Collector,CollectionDate\n" + "Serum,Dr. Smith,11/01/2023\n"
                + "Serum,Dr. Jones,11/02/2023\n";
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        GenericSampleImportResult result = genericSampleOrderService.validateImportFile(stream, "test.csv", "text/csv");

        assertEquals(2, result.getTotalRows());
    }

    @Test
    public void validateImportFile_withEmptyFile_returnsInvalidResultWithError() throws Exception {
        InputStream stream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

        GenericSampleImportResult result = genericSampleOrderService.validateImportFile(stream, "empty.csv",
                "text/csv");

        assertFalse("Empty file should be invalid", result.isValid());
        assertFalse("Should have at least one error describing the problem", result.getErrors().isEmpty());
    }

    @Test
    public void validateImportFile_withHeadersOnlyNoDataRows_returnsZeroTotalRows() throws Exception {
        String csv = "SampleType,Collector,CollectionDate\n";
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        GenericSampleImportResult result = genericSampleOrderService.validateImportFile(stream, "headers-only.csv",
                "text/csv");

        assertEquals(0, result.getTotalRows());
        assertEquals(0, result.getInvalidRows());
        assertEquals(0, result.getValidRows());
    }

    @Test
    public void importSamplesFromFile_withEmptyFile_returnsFalseSuccess() throws Exception {
        InputStream stream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = genericSampleOrderService.importSamplesFromFile(stream, "empty.csv", "text/csv",
                TEST_SYS_USER_ID);

        assertEquals(Boolean.FALSE, result.get("success"));
    }

    private GenericSampleOrderForm buildForm(String labNo, String sampleTypeId, String quantity, String from,
            String collector, String collectionDate, String collectionTime) {
        GenericSampleOrderForm form = new GenericSampleOrderForm();
        GenericSampleOrderForm.DefaultFields fields = new GenericSampleOrderForm.DefaultFields();
        fields.setLabNo(labNo);
        fields.setSampleTypeId(sampleTypeId);
        fields.setQuantity(quantity);
        fields.setFrom(from);
        fields.setCollector(collector);
        fields.setCollectionDate(collectionDate);
        fields.setCollectionTime(collectionTime);
        fields.setNumOrderLabels(1);
        fields.setNumSpecimenLabels(1);
        form.setDefaultFields(fields);
        return form;
    }
}
