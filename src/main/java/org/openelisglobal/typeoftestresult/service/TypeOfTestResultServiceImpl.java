package org.openelisglobal.typeoftestresult.service;

import jakarta.annotation.PostConstruct;
import java.util.EnumSet;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.typeoftestresult.dao.TypeOfTestResultDAO;
import org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class TypeOfTestResultServiceImpl extends AuditableBaseObjectServiceImpl<TypeOfTestResult, String>
        implements TypeOfTestResultService {

    public enum ResultType {
        REMARK("R"), DICTIONARY("D"), TITER("T"), NUMERIC("N"), ALPHA("A"), MULTISELECT("M"),
        CASCADING_MULTISELECT("C");

        String DBValue;
        String id;

        @Component
        public static class TypeOfTestResultServiceInjector {

            @Autowired
            private TypeOfTestResultService typeOfTestResultService;

            @PostConstruct
            @Transactional(readOnly = true)
            private void construct() {
                for (ResultType resultType : EnumSet.allOf(ResultType.class)) {
                    resultType.id = typeOfTestResultService.getTypeOfTestResultByType(resultType.DBValue).getId();
                }
            }
        }

        ResultType(String dbValue) {
            DBValue = dbValue;
        }

        public String getCharacterValue() {
            return DBValue;
        }

        public String getId() {
            return id;
        }

        public boolean matches(String type) {
            return DBValue.equals(type);
        }

        public static boolean isDictionaryVariant(String type) {
            return !GenericValidator.isBlankOrNull(type) && "DMC".contains(type);
        }

        public static boolean isMultiSelectVariant(String type) {
            return !GenericValidator.isBlankOrNull(type) && "MC".contains(type);
        }

        public static boolean isTextOnlyVariant(String type) {
            return !GenericValidator.isBlankOrNull(type) && "AR".contains(type);
        }

        public static boolean isTextOnlyVariant(ResultType type) {
            return "AR".contains(type.getCharacterValue());
        }

        public static boolean isNumericById(String resultTypeId) {
            return NUMERIC.getId().equals(resultTypeId);
        }

        public static boolean isNumeric(ResultType type) {
            return "N".equals(type.getCharacterValue());
        }

        public static boolean isNumeric(String type) {
            return "N".equals(type);
        }

        public static boolean isDictionaryVarientById(String resultTypeId) {
            return DICTIONARY.getId().equals(resultTypeId) || MULTISELECT.getId().equals(resultTypeId)
                    || CASCADING_MULTISELECT.getId().equals(resultTypeId);
        }
    }

    @Autowired
    protected TypeOfTestResultDAO baseObjectDAO;

    TypeOfTestResultServiceImpl() {
        super(TypeOfTestResult.class);
    }

    @Override
    protected TypeOfTestResultDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultType getResultTypeById(String id) {
        for (ResultType type : ResultType.values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfTestResult getTypeOfTestResultByType(TypeOfTestResult typeOfTestResult) {
        return getBaseObjectDAO().getTypeOfTestResultByType(typeOfTestResult);
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfTestResult getTypeOfTestResultByType(String type) {
        return getMatch("testResultType", type).orElse(null);
    }

    @Override
    public String insert(TypeOfTestResult typeOfTestResult) {
        if (getBaseObjectDAO().duplicateTypeOfTestResultExists(typeOfTestResult)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + typeOfTestResult.getDescription());
        }
        return super.insert(typeOfTestResult);
    }

    @Override
    public TypeOfTestResult save(TypeOfTestResult typeOfTestResult) {
        if (getBaseObjectDAO().duplicateTypeOfTestResultExists(typeOfTestResult)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + typeOfTestResult.getDescription());
        }
        return super.save(typeOfTestResult);
    }

    @Override
    public TypeOfTestResult update(TypeOfTestResult typeOfTestResult) {
        if (getBaseObjectDAO().duplicateTypeOfTestResultExists(typeOfTestResult)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + typeOfTestResult.getDescription());
        }
        return super.update(typeOfTestResult);
    }
}
