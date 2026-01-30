package org.openelisglobal.barcode.labeltype;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;

public class SlideLabel extends Label {

        public SlideLabel(Patient patient, Sample sample, PathologySample pathologySample, PathologySlide slide,
                        String labNo) {
                // set dimensions
                try {
                        width = Float
                                        .parseFloat(
                                                        ConfigurationProperties.getInstance().getPropertyValue(
                                                                        Property.SLIDE_LABEL_BARCODE_WIDTH));
                        height = Float
                                        .parseFloat(ConfigurationProperties.getInstance()
                                                        .getPropertyValue(Property.SLIDE_LABEL_BARCODE_HEIGHT));
                        // TODO determine which specific exceptions to catch
                } catch (Exception e) {
                        LogEvent.logError("SlideLabel", "SlideLabel SlideLabel()", e.toString());
                }
                boolean usePatientId = "true".equals(ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.SLIDE_LABEL_FIELD_PATIENT_ID));
                boolean useSlideId = "true".equals(ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.SLIDE_LABEL_FIELD_SLIDE_ID));
                boolean useStaintype = "true".equals(ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.SLIDE_LABEL_FIELD_STAIN_TYPE));
                boolean useBlockId = "true".equals(ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.SLIDE_LABEL_FIELD_BLOCK_ID));
                boolean useCaseNumber = "true".equals(ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.SLIDE_LABEL_FIELD_CASE_NUMBER));

                // adding fields above bar code
                aboveFields = new ArrayList<>();
                // adding fields below bar code
                belowFields = new ArrayList<>();

                if (usePatientId)
                        aboveFields.add(getAvailableIdField(patient));
                if (useSlideId)
                        aboveFields.add(new LabelField(MessageUtil.getMessage("barcode.label.info.slideNumber"),
                                        String.valueOf(slide.getSlideNumber()), 4));

                // if (useStaintype) {

                // }

                // if (useBlockId) {

                // }

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
                int max = 0;
                max = Integer
                                .parseInt(ConfigurationProperties.getInstance()
                                                .getPropertyValue(Property.MAX_SLIDE_LABEL_PRINTED));
                return max;
        }
}
