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
import org.openelisglobal.common.services.SampleOrderService;
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
        // adding fields above bar code
        aboveFields = new ArrayList<>();

        LabelField field;
        field = new LabelField(MessageUtil.getMessage("barcode.label.info.patientname"), "", 12);
        field.setDisplayFieldName(true);
        field.setUnderline(true);
        aboveFields.add(field);

        field = new LabelField(MessageUtil.getMessage("barcode.label.info.patientdob"), "", 8);
        field.setDisplayFieldName(true);
        field.setUnderline(true);
        aboveFields.add(field);

        LabelField siteField = new LabelField(MessageUtil.getMessage("barcode.label.info.site"),
                StringUtils.substring(facilityName, 0, 20), 8);
        siteField.setDisplayFieldName(true);
        aboveFields.add(siteField);

        // getting fields for below bar code
        StringBuilder tests = new StringBuilder();
        String seperator = ""; // separator for appending tests to each other
        for (Test test : testsForSample) {
            tests.append(seperator);
            tests.append(TestServiceImpl.getUserLocalizedTestName(test));
            seperator = ", ";
        }

        // adding fields below bar code
        belowFields = new ArrayList<>();
        String useDateTime = ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_FIELD_DATE);
        String useCollectedBy = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_FIELD_COLLECTED_BY);
        String useSex = ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_FIELD_SEX);
        String useTests = ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_FIELD_TESTS);
        if ("true".equals(useSex)) {
            LabelField sexField = new LabelField(MessageUtil.getMessage("barcode.label.info.patientsex"), "", 4);
            sexField.setDisplayFieldName(true);
            sexField.setUnderline(true);
            belowFields.add(sexField);
        }
        if ("true".equals(useDateTime)) {
            LabelField dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectiondate"), "", 6);
            dateField.setDisplayFieldName(true);
            dateField.setUnderline(true);
            belowFields.add(dateField);
            dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectiontime"), "", 4);
            dateField.setUnderline(true);
            belowFields.add(dateField);
        }
        if ("true".equals(useCollectedBy)) {
            LabelField collectorField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectorid"),
                    StringUtils.substring("", 0, 15), 6);
            collectorField.setDisplayFieldName(true);
            collectorField.setUnderline(true);
            belowFields.add(collectorField);
        }
        if ("true".equals(useTests)) {
            LabelField testsField = new LabelField(MessageUtil.getMessage("barcode.label.info.tests"),
                    StringUtil.replaceNullWithEmptyString(tests.toString()), 20);
            testsField.setStartNewline(true);
            belowFields.add(testsField);
        }

        // add code
        // String sampleCode = sampleItem.getSortOrder();
        // setCode(labNo + "." + sampleCode);
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
        // set dimensions
        width = Float
                .parseFloat(ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_BARCODE_WIDTH));
        height = Float
                .parseFloat(ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_BARCODE_HEIGHT));
        // get information for displaying above bar code
        SampleOrderService sampleOrderService = new SampleOrderService(sample);
        Person person = patient.getPerson();
        String referringFacility = StringUtil
                .replaceNullWithEmptyString(sampleOrderService.getSampleOrderItem().getReferringSiteName());
        String patientName = StringUtil.replaceNullWithEmptyString(person.getLastName()) + ", "
                + StringUtil.replaceNullWithEmptyString(person.getFirstName());
        if (patientName.trim().equals(",")) {
            patientName = " ";
        }
        patientName = StringUtils.substring(patientName.replaceAll("( )+", " "), 0, 30);
        String dob = StringUtil.replaceNullWithEmptyString(patient.getBirthDateForDisplay());

        // adding fields above bar code
        aboveFields = new ArrayList<>();
        aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.patientname"), patientName, 12));
        aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.patientdob"), dob, 8));
        aboveFields.add(getAvailableIdField(patient));
        LabelField siteField = new LabelField(MessageUtil.getMessage("barcode.label.info.site"),
                StringUtils.substring(referringFacility, 0, 20), 8);
        siteField.setDisplayFieldName(true);
        aboveFields.add(siteField);

        // getting fields for below bar code
        Timestamp timestamp = sampleItem.getCollectionDate();
        String collectionDate = DateUtil.convertTimestampToStringDate(timestamp);
        String collectionTime = DateUtil.convertTimestampToStringTime(timestamp);

        String collector = sampleItem.getCollector();
        StringBuilder tests = new StringBuilder();
        String seperator = ""; // separator for appending tests to each other
        List<Analysis> analysisList = analysisService.getAnalysesBySampleItem(sampleItem);
        for (Analysis analysis : analysisList) {
            tests.append(seperator);
            tests.append(TestServiceImpl.getUserLocalizedTestName(analysis.getTest()));
            seperator = ", ";
        }

        // adding fields below bar code
        belowFields = new ArrayList<>();
        String useDateTime = ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_FIELD_DATE);
        String useCollectedBy = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_FIELD_COLLECTED_BY);
        String useSex = ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_FIELD_SEX);
        String useTests = ConfigurationProperties.getInstance().getPropertyValue(Property.SPECIMEN_FIELD_TESTS);
        if ("true".equals(useSex)) {
            LabelField sexField = new LabelField(MessageUtil.getMessage("barcode.label.info.patientsex"),
                    StringUtil.replaceNullWithEmptyString(patient.getGender()), 4);
            sexField.setDisplayFieldName(true);
            belowFields.add(sexField);
        }
        if ("true".equals(useDateTime)) {
            LabelField dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectiondate"),
                    collectionDate, 6);
            dateField.setDisplayFieldName(true);
            belowFields.add(dateField);
            dateField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectiontime"),
                    StringUtil.replaceNullWithEmptyString(collectionTime), 4);
            belowFields.add(dateField);
        }
        if ("true".equals(useCollectedBy)) {
            LabelField collectorField = new LabelField(MessageUtil.getMessage("barcode.label.info.collectorid"),
                    StringUtils.substring(StringUtil.replaceNullWithEmptyString(collector), 0, 15), 6);
            collectorField.setDisplayFieldName(true);
            belowFields.add(collectorField);
        }
        if ("true".equals(useTests)) {
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
     * Get first available id to identify a patient (Subject Number > National Id)
     *
     * @param patient Who to find identification for
     * @return label field containing patient id
     */
    private LabelField getAvailableIdField(Patient patient) {
        PatientService patientPatientService = SpringContext.getBean(PatientService.class);
        PersonService personService = SpringContext.getBean(PersonService.class);
        personService.getData(patient.getPerson());
        String patientId = patientPatientService.getSubjectNumber(patient);
        if (!StringUtil.isNullorNill(patientId)) {
            return new LabelField(MessageUtil.getMessage("barcode.label.info.patientid"),
                    StringUtils.substring(patientId, 0, 25), 12);
        }
        patientId = patientPatientService.getNationalId(patient);
        if (!StringUtil.isNullorNill(patientId)) {
            return new LabelField(MessageUtil.getMessage("barcode.label.info.patientid"),
                    StringUtils.substring(patientId, 0, 25), 12);
        }
        return new LabelField(MessageUtil.getMessage("barcode.label.info.patientid"), "", 12);
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
        int max = 0;
        max = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue(Property.MAX_SPECIMEN_PRINTED));
        return max;
    }
}
