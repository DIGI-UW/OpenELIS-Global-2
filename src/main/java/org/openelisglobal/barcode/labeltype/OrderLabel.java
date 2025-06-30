package org.openelisglobal.barcode.labeltype;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Stores values and formatting for Order Labels
 *
 * @author Caleb
 */
public class OrderLabel extends Label {

    public OrderLabel(String labNo, String facility) {
        // set dimensions
        try {
            width = Float
                    .parseFloat(ConfigurationProperties.getInstance().getPropertyValue(Property.ORDER_BARCODE_WIDTH));
            height = Float
                    .parseFloat(ConfigurationProperties.getInstance().getPropertyValue(Property.ORDER_BARCODE_HEIGHT));
        } catch (Exception e) {
            LogEvent.logError("OrderLabel", "OrderLabel OrderLabel()", e.toString());
        }
        // adding fields above bar code
        aboveFields = new ArrayList<>();
        LabelField labelField = new LabelField(MessageUtil.getMessage("barcode.label.info.patientname"), "", 12);
        labelField.setDisplayFieldName(true);
        labelField.setUnderline(true);
        aboveFields.add(labelField);

        labelField = new LabelField(MessageUtil.getMessage("barcode.label.info.patientdob"), "", 8);
        labelField.setDisplayFieldName(true);
        labelField.setUnderline(true);
        aboveFields.add(labelField);

        // aboveFields.add(getAvailableIdField(patient));
        LabelField siteField = new LabelField(MessageUtil.getMessage("barcode.label.info.site"),
                StringUtils.substring(facility, 0, 20), 8);
        siteField.setDisplayFieldName(true);
        aboveFields.add(siteField);

        // adding bar code
        if (AccessionFormat.ALPHANUM.toString()
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
            setCodeLabel(AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(labNo));
        }
        setCode(labNo);
    }

    /**
     * @param patient Who to include on order label
     * @param sample  What to include on order label
     * @param labNo   Code to include in bar code
     */
    public OrderLabel(Patient patient, Sample sample, String labNo) {
        // set dimensions
        width = Float.parseFloat(ConfigurationProperties.getInstance().getPropertyValue(Property.ORDER_BARCODE_WIDTH));
        height = Float
                .parseFloat(ConfigurationProperties.getInstance().getPropertyValue(Property.ORDER_BARCODE_HEIGHT));
        // get information to display above bar code
        Person person = patient.getPerson();
        SampleOrderService sampleOrderService = new SampleOrderService(sample);
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

        // adding bar code
        if (AccessionFormat.ALPHANUM.toString()
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
            setCodeLabel(AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(labNo));
        }
        setCode(labNo);
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
        return new LabelField(MessageUtil.getMessage("barcode.label.info.patientid"), "", 6);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.barcode.labeltype.Label#getNumTextRowsBefore()
     */
    @Override
    public int getNumTextRowsBefore() {
        int numRows = 0;
        int curColumns = 0;
        boolean completeRow = true;
        Iterable<LabelField> fields = getAboveFields();
        for (LabelField field : fields) {
            // add to num row if start on newline
            if (field.isStartNewline() && !completeRow) {
                ++numRows;
                curColumns = 0;
            }
            curColumns += field.getColspan();
            if (curColumns > 10) {
                // TO DO: (caleb) throw error
                // row is completed, add to num row
            } else if (curColumns == 10) {
                completeRow = true;
                curColumns = 0;
                ++numRows;
            } else {
                completeRow = false;
            }
        }
        // add to num row if last row was incomplete
        if (!completeRow) {
            ++numRows;
        }

        return numRows;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.barcode.labeltype.Label#getNumTextRowsAfter()
     */
    @Override
    public int getNumTextRowsAfter() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.barcode.labeltype.Label#getMaxNumLabels()
     */
    @Override
    public int getMaxNumLabels() {
        int max = 0;
        try {
            max = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue(Property.MAX_ORDER_PRINTED));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
        return max;
    }
}
