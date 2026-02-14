package org.openelisglobal.barcode.labeltype;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.barcode.util.BarcodeConfigUtil;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
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
            FhirUtil fhirUtil = SpringContext.getBean(FhirUtil.class);
            IGenericClient client = fhirUtil.getLocalFhirClient();
            Bundle bundle = client.search().forResource(QuestionnaireResponse.class)
                    .where(QuestionnaireResponse.QUESTIONNAIRE.hasId("73f19a23-3899-4c4f-b7a1-8f945b72ded8"))
                    .returnBundle(Bundle.class).execute();
            if (!bundle.getEntry().isEmpty()) {
                QuestionnaireResponse response = (QuestionnaireResponse) bundle.getEntry().getFirst().getResource();
                for (QuestionnaireResponseItemComponent component : response.getItem()) {
                    if ("b1e193ef-21fa-4642-a401-d57a56831cb5".equals(component.getLinkId())) {
                        aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.specimenType"),
                                component.getAnswerFirstRep().getValueStringType().getValue(), 4));
                    }
                }

            }
        }
        // if (useCaseNumber) {

        // }
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
