package org.openelisglobal.barcode.labeltype;

import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.barcode.util.BarcodeConfigUtil;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;

/**
 * Stores values and formatting for Specimen Labels
 *
 * @author Caleb
 */
public class SpecimenLabel extends Label {

    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);

    public SpecimenLabel(String labNumber, String facilityName, List<Test> testsForSample) {
        // set dimensions (safe parsing for admin-configured DB values)
        width = BarcodeConfigUtil.parseFloatSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_BARCODE_WIDTH), 2.0f);
        height = BarcodeConfigUtil.parseFloatSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_BARCODE_HEIGHT), 2.0f);

        boolean useDob = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_DOB));
        boolean usePatientId = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_ID));
        boolean usePatientName = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_NAME));
        boolean useCollectionDate = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_COLLECTION_DATE));
        boolean useCollectedBy = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_COLLECTED_BY));
        boolean useTests = "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_TESTS));
        boolean usePatientSex = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_SEX));

        // adding fields above bar code
        aboveFields = new ArrayList<>();

        LabelField field;
        if (usePatientName) {
            field = new LabelField(MessageUtil.getMessage("barcode.label.info.patientName"), "", 12);
            field.setDisplayFieldName(true);
            field.setUnderline(true);
            aboveFields.add(field);
        }
        if (useDob) {
            field = new LabelField(MessageUtil.getMessage("barcode.label.info.patientdob"), "", 8);
            field.setDisplayFieldName(true);
            field.setUnderline(true);
            aboveFields.add(field);
        }
        if (usePatientId) {
            aboveFields.add(getAvailableIdField(null));
        }

        // LabelField siteField = new
        // LabelField(MessageUtil.getMessage("barcode.label.info.site"),
        // StringUtils.substring(facilityName, 0, 20),
        // 8);
        // siteField.setDisplayFieldName(true);
        // aboveFields.add(siteField);

        // adding fields below bar code
        belowFields = new ArrayList<>();
        if (usePatientSex) {
            LabelField sexField = new LabelField(MessageUtil.getMessage("barcode.label.info.patientsex"), "", 4);
            sexField.setDisplayFieldName(true);
            sexField.setUnderline(true);
            belowFields.add(sexField);
        }
        if (useCollectionDate) {
            LabelField dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectiondate"), "", 6);
            dateField.setDisplayFieldName(true);
            dateField.setUnderline(true);
            belowFields.add(dateField);
            dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectiontime"), "", 4);
            dateField.setUnderline(true);
            belowFields.add(dateField);
        }
        if (useCollectedBy) {
            LabelField collectorField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectorid"),
                    StringUtils.substring("", 0, 15), 6);
            collectorField.setDisplayFieldName(true);
            collectorField.setUnderline(true);
            belowFields.add(collectorField);
        }
        if (useTests) {
            StringBuilder tests = new StringBuilder();
            String seperator = ""; // separator for appending tests to each other
            for (Test test : testsForSample) {
                tests.append(seperator);
                tests.append(TestServiceImpl.getUserLocalizedTestName(test));
                seperator = ", ";
            }

            LabelField testsField = new LabelField(MessageUtil.getMessage("barcode.label.info.tests"),
                    StringUtil.replaceNullWithEmptyString(tests.toString()), 20);
            testsField.setStartNewline(true);
            belowFields.add(testsField);
        }

        setCode(labNumber);

        setValueFont(new Font(FontFamily.HELVETICA, 7, Font.NORMAL));
        setNameFont(new Font(FontFamily.HELVETICA, 7, Font.BOLD));
    }

    /**
     * @param patient    Who include on specimen label
     * @param sample     What sample to include on specimen label
     * @param sampleItem What specific sample item to include on specimen label
     * @param labNo      Number to start code with
     */
    public SpecimenLabel(Patient patient, Sample sample, SampleItem sampleItem, String labNo) {
        // set dimensions (safe parsing for admin-configured DB values)
        width = BarcodeConfigUtil.parseFloatSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_BARCODE_WIDTH), 2.0f);
        height = BarcodeConfigUtil.parseFloatSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_BARCODE_HEIGHT), 2.0f);

        boolean useDob = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_DOB));
        boolean usePatientId = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_ID));
        boolean usePatientName = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_NAME));
        boolean useCollectionDate = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_COLLECTION_DATE));
        boolean useCollectedBy = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_COLLECTED_BY));
        boolean useTests = "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_TESTS));
        boolean usePatientSex = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_SEX));

        // adding fields above bar code
        aboveFields = new ArrayList<>();
        if (usePatientName) {
            Person person = patient.getPerson();
            String patientName = StringUtil.replaceNullWithEmptyString(person.getLastName()) + ", "
                    + StringUtil.replaceNullWithEmptyString(person.getFirstName());
            if (patientName.trim().equals(",")) {
                patientName = " ";
            }
            patientName = StringUtils.substring(patientName.replaceAll("( )+", " "), 0, 30);
            aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.patientName"), patientName, 12));
        }
        if (useDob) {
            String dob = StringUtil.replaceNullWithEmptyString(patient.getBirthDateForDisplay());
            aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.patientdob"), dob, 8));
        }
        if (usePatientId)
            aboveFields.add(getAvailableIdField(patient));

        // SampleOrderService sampleOrderService = new SampleOrderService(sample);
        // String referringFacility = StringUtil
        // .replaceNullWithEmptyString(
        // sampleOrderService.getSampleOrderItem().getReferringSiteName());
        // LabelField siteField = new
        // LabelField(MessageUtil.getMessage("barcode.label.info.site"),
        // StringUtils.substring(referringFacility, 0, 20), 8);
        // siteField.setDisplayFieldName(true);
        // aboveFields.add(siteField);

        // adding fields below bar code
        belowFields = new ArrayList<>();

        if (usePatientSex) {
            LabelField sexField = new LabelField(MessageUtil.getMessage("barcode.label.info.patientsex"),
                    StringUtil.replaceNullWithEmptyString(patient.getGender()), 4);
            sexField.setDisplayFieldName(true);
            belowFields.add(sexField);
        }
        if (useCollectionDate) {
            Timestamp timestamp = sampleItem.getCollectionDate();
            String collectionDate = DateUtil.convertTimestampToStringDate(timestamp);
            String collectionTime = DateUtil.convertTimestampToStringTime(timestamp);
            LabelField dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectiondate"),
                    collectionDate, 6);
            dateField.setDisplayFieldName(true);
            belowFields.add(dateField);
            dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectiontime"),
                    StringUtil.replaceNullWithEmptyString(collectionTime), 4);
            belowFields.add(dateField);
        }
        if (useCollectedBy) {
            LabelField collectorField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectorid"),
                    StringUtils.substring(StringUtil.replaceNullWithEmptyString(sampleItem.getCollector()), 0, 15), 6);
            collectorField.setDisplayFieldName(true);
            belowFields.add(collectorField);
        }
        if (useTests) {
            StringBuilder tests = new StringBuilder();
            String seperator = ""; // separator for appending tests to each other
            List<Analysis> analysisList = analysisService.getAnalysesBySampleItem(sampleItem);
            for (Analysis analysis : analysisList) {
                tests.append(seperator);
                tests.append(TestServiceImpl.getUserLocalizedTestName(analysis.getTest()));
                seperator = ", ";
            }
            LabelField testsField = new LabelField(MessageUtil.getMessage("barcode.label.info.tests"),
                    StringUtil.replaceNullWithEmptyString(tests.toString()), 20);
            testsField.setStartNewline(true);
            belowFields.add(testsField);
        }

        // add code
        String sampleCode = sampleItem.getSortOrder();
        setCode(labNo + "." + sampleCode);

        setValueFont(new Font(FontFamily.HELVETICA, 7, Font.NORMAL));
        setNameFont(new Font(FontFamily.HELVETICA, 7, Font.BOLD));
    }

    /**
     * Constructor for generic samples with sample type, quantity, and from
     * information (no patient info).
     *
     * @param sampleItem The sample item to create label for
     * @param labNo      Lab/accession number
     * @param sampleType Sample type description
     * @param quantity   Quantity with unit of measure
     * @param from       Source/origin of the sample
     */
    public SpecimenLabel(SampleItem sampleItem, String labNo, String sampleType, String quantity, String from) {
        // set dimensions (safe parsing for admin-configured DB values)
        width = BarcodeConfigUtil.parseFloatSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_BARCODE_WIDTH), 2.0f);
        height = BarcodeConfigUtil.parseFloatSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_BARCODE_HEIGHT), 2.0f);

        boolean useDob = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_DOB));
        boolean usePatientId = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_ID));
        boolean usePatientName = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_NAME));
        boolean useCollectionDate = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_COLLECTION_DATE));
        boolean useCollectedBy = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_COLLECTED_BY));
        boolean useTests = "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_TESTS));
        boolean usePatientSex = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_SEX));

        // adding fields above bar code
        aboveFields = new ArrayList<>();

        // Sample Type field
        if (!StringUtil.isNullorNill(sampleType)) {
            LabelField sampleTypeField = new LabelField(MessageUtil.getMessage("barcode.label.info.sampletype"),
                    StringUtils.substring(sampleType, 0, 25), 10);
            sampleTypeField.setDisplayFieldName(true);
            aboveFields.add(sampleTypeField);
        }

        // Quantity field
        if (!StringUtil.isNullorNill(quantity)) {
            LabelField quantityField = new LabelField(MessageUtil.getMessage("barcode.label.info.quantity"),
                    StringUtils.substring(quantity, 0, 15), 10);
            quantityField.setDisplayFieldName(true);
            aboveFields.add(quantityField);
        }

        // From/Source field
        if (!StringUtil.isNullorNill(from)) {
            LabelField fromField = new LabelField(MessageUtil.getMessage("barcode.label.info.from"),
                    StringUtils.substring(from, 0, 25), 10);
            fromField.setDisplayFieldName(true);
            aboveFields.add(fromField);
        }

        // adding fields below bar code
        belowFields = new ArrayList<>();

        if (useCollectionDate) {
            Timestamp timestamp = sampleItem.getCollectionDate();
            String collectionDate = DateUtil.convertTimestampToStringDate(timestamp);
            String collectionTime = DateUtil.convertTimestampToStringTime(timestamp);
            LabelField dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectiondate"),
                    collectionDate, 6);
            dateField.setDisplayFieldName(true);
            belowFields.add(dateField);
            dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectiontime"),
                    StringUtil.replaceNullWithEmptyString(collectionTime), 4);
            belowFields.add(dateField);
        }
        if (useCollectedBy) {
            LabelField collectorField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectorid"),
                    StringUtils.substring(StringUtil.replaceNullWithEmptyString(sampleItem.getCollector()), 0, 15), 6);
            collectorField.setDisplayFieldName(true);
            belowFields.add(collectorField);
        }
        if (useTests) {
            // Get tests for this specimen
            StringBuilder tests = new StringBuilder();
            String seperator = "";
            List<Analysis> analysisList = analysisService.getAnalysesBySampleItem(sampleItem);
            for (Analysis analysis : analysisList) {
                tests.append(seperator);
                tests.append(TestServiceImpl.getUserLocalizedTestName(analysis.getTest()));
                seperator = ", ";
            }
            LabelField testsField = new LabelField(MessageUtil.getMessage("barcode.label.info.tests"),
                    StringUtil.replaceNullWithEmptyString(tests.toString()), 20);
            testsField.setStartNewline(true);
            belowFields.add(testsField);
        }

        // add code - use external ID if available, otherwise labNo.sortOrder
        String sampleCode = sampleItem.getExternalId();
        if (StringUtil.isNullorNill(sampleCode)) {
            sampleCode = labNo + "." + sampleItem.getSortOrder();
        }
        setCode(sampleCode);

        setValueFont(new Font(FontFamily.HELVETICA, 7, Font.NORMAL));
        setNameFont(new Font(FontFamily.HELVETICA, 7, Font.BOLD));
    }

    /**
     * Get first available id to identify a patient (Subject Number > National Id)
     *
     * @param patient Who to find identification for
     * @return label field containing patient id
     */
    private LabelField getAvailableIdField(Patient patient) {
        if (patient == null) {
            return new LabelField(MessageUtil.getMessage("barcode.label.info.patientId"), "", 12);
        }
        PatientService patientPatientService = SpringContext.getBean(PatientService.class);
        PersonService personService = SpringContext.getBean(PersonService.class);
        personService.getData(patient.getPerson());
        String patientId = patientPatientService.getSubjectNumber(patient);
        if (!StringUtil.isNullorNill(patientId)) {
            return new LabelField(MessageUtil.getMessage("barcode.label.info.patientId"),
                    StringUtils.substring(patientId, 0, 25), 12);
        }
        patientId = patientPatientService.getNationalId(patient);
        if (!StringUtil.isNullorNill(patientId)) {
            return new LabelField(MessageUtil.getMessage("barcode.label.info.patientId"),
                    StringUtils.substring(patientId, 0, 25), 12);
        }
        return new LabelField(MessageUtil.getMessage("barcode.label.info.patientId"), "", 12);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.barcode.labeltype.Label#getNumTextRowsBefore()
     */
    @Override
    public int getNumTextRowsBefore() {
        Iterable<LabelField> fields = getAboveFields();
        return getNumRows(fields);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.barcode.labeltype.Label#getNumTextRowsAfter()
     */
    @Override
    public int getNumTextRowsAfter() {
        Iterable<LabelField> fields = getBelowFields();
        return getNumRows(fields);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.barcode.labeltype.Label#getMaxNumLabels()
     */
    @Override
    public int getMaxNumLabels() {
        return BarcodeConfigUtil.parseIntSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.MAX_SPECIMEN_LABEL_PRINTED), 10);
    }
}
