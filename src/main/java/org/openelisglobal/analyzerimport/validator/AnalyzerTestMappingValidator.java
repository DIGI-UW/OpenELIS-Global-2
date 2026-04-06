package org.openelisglobal.analyzerimport.validator;

import java.util.List;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class AnalyzerTestMappingValidator implements Validator {

    protected AnalyzerTestMappingService analyzerTestMappingService = SpringContext
            .getBean(AnalyzerTestMappingService.class);

    @Override
    public boolean supports(Class<?> clazz) {
        return AnalyzerTestMappingValidator.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        AnalyzerTestMapping analyzerTestMapping = (AnalyzerTestMapping) target;

        ValidationHelper.validateIdField(analyzerTestMapping.getAnalyzerId(), "analyzerId", errors, true);

        ValidationHelper.validateFieldAndCharset(analyzerTestMapping.getAnalyzerTestName(), "analyzerTestName", errors,
                true, 30, " a-zA-Z0-9�������������������������Ԍ��ܟ�\\\\\\-%#\\(\\)\\^_");

        ValidationHelper.validateIdField(analyzerTestMapping.getTestId(), "testId", errors, true);
    }

    public void preInsertValidate(AnalyzerTestMapping analyzerTestMapping, Errors errors) {
        validate(analyzerTestMapping, errors);

        List<AnalyzerTestMapping> testMappingList = analyzerTestMappingService.getAll();
        for (AnalyzerTestMapping testMapping : testMappingList) {
            if (analyzerTestMapping.getAnalyzerId().equals(testMapping.getAnalyzerId())
                    && analyzerTestMapping.getAnalyzerTestName().equals(testMapping.getAnalyzerTestName())) {
                errors.reject("error.analyzer.test.name.duplicate");
            }
        }
    }

    public void preUpdateValidate(AnalyzerTestMapping analyzerTestMapping, Errors errors) {
        validate(analyzerTestMapping, errors);

        // ValidationHelper.validateIdField(analyzerTestMapping.getId(), "id", errors,
        // true);
        ValidationHelper.validateIdField(analyzerTestMapping.getStringId(), "id", errors, true);
    }
}
