package org.openelisglobal.barcode.labeltype;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.barcode.util.BarcodeConfigUtil;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;

public class BlockLabel extends Label {

    public BlockLabel(Patient patient, Sample sample, PathologySample pathologySample, PathologyBlock block,
            String labNo) {
        this(patient, sample, pathologySample, block, labNo, null);
    }

    public BlockLabel(Patient patient, Sample sample, PathologySample pathologySample, PathologyBlock block,
            String labNo, String specimenType) {
        // set dimensions (safe parsing for admin-configured DB values)
        width = BarcodeConfigUtil.parseFloatSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.BLOCK_LABEL_BARCODE_WIDTH), 2.0f);
        height = BarcodeConfigUtil.parseFloatSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.BLOCK_LABEL_BARCODE_HEIGHT), 2.0f);
        boolean usePatientId = "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.BLOCK_LABEL_FIELD_PATIENT_ID));
        boolean useBlockId = "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.BLOCK_LABEL_FIELD_BLOCK_ID));
        boolean useSpecimenType = "true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.BLOCK_LABEL_FIELD_SPECIMEN_TYPE));
        boolean useCaseNumber = "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.BLOCK_LABEL_FIELD_CASE_NUMBER));

        // adding fields above bar code
        aboveFields = new ArrayList<>();
        // adding fields below bar code
        belowFields = new ArrayList<>();

        if (usePatientId)
            aboveFields.add(getAvailableIdField(patient));

        if (useBlockId)
            aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.blockNumber"),
                    String.valueOf(block.getBlockNumber()), 4));
        if (useSpecimenType) {
            aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.specimenType"),
                    StringUtils.defaultString(specimenType), 4));
        }
        if (useCaseNumber) {
            aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.caseNumber"),
                    pathologySample != null && pathologySample.getId() != null ? String.valueOf(pathologySample.getId())
                            : "",
                    4));
        }
        // adding bar code
        setCode(labNo);
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
                ConfigurationProperties.getInstance().getPropertyValue(Property.MAX_BLOCK_LABEL_PRINTED), 10);
    }
}
