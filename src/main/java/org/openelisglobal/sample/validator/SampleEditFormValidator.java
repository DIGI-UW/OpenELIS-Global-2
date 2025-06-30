package org.openelisglobal.sample.validator;

import java.util.Iterator;
import org.apache.commons.validator.GenericValidator;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class SampleEditFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return SampleEditForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        SampleEditForm form = (SampleEditForm) target;

        // maxAccessionNumber
        String[] maxAccessionNumberArray = form.getMaxAccessionNumber().split("-");
        if (!ValidationResults.SUCCESS.equals(AccessionNumberUtil.correctFormat(maxAccessionNumberArray[0], false))) {
            errors.rejectValue("maxAccessionNumber", "error.field.accession.format");
        } else if (!GenericValidator.isInt(maxAccessionNumberArray[1])) {
            errors.rejectValue("maxAccessionNumber", "error.field.accession.format");
        }

        validateSampleXML(form.getSampleXML(), errors);

        ValidationHelper.validateOptionField(form.getIdSeparator(), "idSeperator", errors,
                new String[] { ConfigurationProperties.getInstance().getPropertyValue("default.idSeparator") });

        ValidationHelper.validateOptionField(form.getAccessionFormat(), "accessionFormat", errors,
                new String[] { ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat) });

        int changeableLength = AccessionNumberUtil.getChangeableLength();
        ValidationHelper.validateFieldMinMax(form.getEditableAccession(), "editableAccession", errors, changeableLength,
                changeableLength);

        int invariantLength = AccessionNumberUtil.getInvarientLength();
        ValidationHelper.validateFieldMinMax(form.getNonEditableAccession(), "nonEditableAccession", errors,
                invariantLength, invariantLength);

        ValidationHelper.validateFieldMinMax(form.getMaxAccessionLength(), "maxAccessionLength", errors,
                changeableLength + invariantLength, changeableLength + invariantLength);
    }

    @SuppressWarnings("unchecked")
    private void validateSampleXML(String sampleXML, Errors errors) {
        try {
            Document sampleDom = DocumentHelper.parseText(sampleXML);
            for (Iterator<Element> iter = sampleDom.getRootElement().elementIterator("sample"); iter.hasNext();) {
                Element sampleItem = iter.next();
                validateSampleItem(sampleItem, errors);
                if (errors.hasErrors()) {
                    return;
                }
            }
        } catch (DocumentException e) {
            errors.reject("batchentry.error.sampleXML.invalid");
        }
    }

    private void validateSampleItem(Element sampleItem, Errors errors) {
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
        ValidationHelper.validateDateField(collectionDate, "sampleXML", "sampleXML date", errors, DateRelation.PAST,
                false);
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
