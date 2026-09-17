package org.openelisglobal.sample.validator;

import java.util.Iterator;
import org.apache.commons.validator.GenericValidator;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class SamplePatientEntryFormValidator implements Validator {

    private static final String WORKFLOW_TYPE_FIELD = "workflowType";
    private static final String WORKFLOW_ENVIRONMENTAL = "environmental";
    private static final String WORKFLOW_VECTOR = "vector";

    @Override
    public boolean supports(Class<?> clazz) {
        return SamplePatientEntryForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        SamplePatientEntryForm form = (SamplePatientEntryForm) target;

        // sampleXML
        if (!GenericValidator.isBlankOrNull(form.getSampleXML())) {
            validateSampleXML(form.getSampleXML(), collectionDateRelation(form), errors);
        }

    }

    /**
     * Clinical specimens can only have been collected in the past, so the clinical
     * path keeps {@link DateRelation#PAST}. Environmental and vector collections
     * are planned field activities whose collection date may legitimately be in the
     * future, so the relation is relaxed to {@link DateRelation#ANY} for those two
     * workflows only. The discriminator is the order's {@code workflowType}, the
     * same value {@code SamplePatientUpdateData.initSampleData} uses to derive
     * {@code Sample.domain}; forms that carry no order item (batch entry setup)
     * fall back to the strict clinical rule.
     */
    private DateRelation collectionDateRelation(SamplePatientEntryForm form) {
        SampleOrderItem sampleOrder = form.getSampleOrderItems();
        if (sampleOrder == null) {
            return DateRelation.PAST;
        }
        String workflowType = sampleOrder.getEnvironmentalFieldAsString(WORKFLOW_TYPE_FIELD);
        if (WORKFLOW_ENVIRONMENTAL.equalsIgnoreCase(workflowType) || WORKFLOW_VECTOR.equalsIgnoreCase(workflowType)) {
            return DateRelation.ANY;
        }
        return DateRelation.PAST;
    }

    private void validateSampleXML(String sampleXML, DateRelation collectionDateRelation, Errors errors) {
        try {
            Document sampleDom = DocumentHelper.parseText(sampleXML);
            for (Iterator<Element> iter = sampleDom.getRootElement().elementIterator("sample"); iter.hasNext();) {
                Element sampleItem = iter.next();
                validateSampleItem(sampleItem, collectionDateRelation, errors);
                if (errors.hasErrors()) {
                    return;
                }
            }
        } catch (DocumentException e) {
            errors.reject("batchentry.error.sampleXML.invalid");
        }
    }

    private void validateSampleItem(Element sampleItem, DateRelation collectionDateRelation, Errors errors) {
        // validate test ids
        String[] testIDs = sampleItem.attributeValue("tests").split(",");
        for (int j = 0; j < testIDs.length; ++j) {
            ValidationHelper.validateIdField(testIDs[j], "sampleXML", "sampleXML tests", errors, false);
            if (errors.hasErrors()) {
                return;
            }
        } // validate panel ids
        String[] panelIDs = sampleItem.attributeValue("panels").split(",");
        for (int j = 0; j < panelIDs.length; ++j) {
            ValidationHelper.validateIdField(panelIDs[j], "sampleXML", "sampleXML panels", errors, false);
            if (errors.hasErrors()) {
                return;
            }
        }
        // validate date not required
        String collectionDate = sampleItem.attributeValue("date").trim();
        ValidationHelper.validateDateField(collectionDate, "sampleXML", "sampleXML date", errors,
                collectionDateRelation, false);
        if (errors.hasErrors()) {
            return;
        }

        // validate time
        String collectionTime = sampleItem.attributeValue("time").trim();
        ValidationHelper.validateTimeField(collectionTime, "sampleXML", "sampleXML time", errors, false);
        if (errors.hasErrors()) {
            return;
        }

        // validate sample id
        String sampleId = sampleItem.attributeValue("sampleID");
        ValidationHelper.validateIdField(sampleId, "sampleXML", "sampleXML sampleID", errors, true);
    }
}
