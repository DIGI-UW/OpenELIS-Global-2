package org.openelisglobal.typeoftestresult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult;
import org.springframework.beans.factory.annotation.Autowired;

public class TypeOfTestResultServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TypeOfTestResultService typeOfTestResultService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/type-of-testresult.xml");
    }

    @Test
    public void testDataInDataBase() {
        List<TypeOfTestResult> typeOfTestResults = typeOfTestResultService.getAll();
        assertNotNull(typeOfTestResults);
        assertTrue(typeOfTestResults.size() > 0);
    }

    @Test
    public void getAll_shouldReturnAllTypeOfTestResults() {
        List<TypeOfTestResult> typeOfTestResults = typeOfTestResultService.getAll();
        assertEquals(7, typeOfTestResults.size());
    }

    @Test
    public void getTypeOfTestResultByType_shouldReturnTypeOfTestResultByType() {
        TypeOfTestResult typeOfTestResult = typeOfTestResultService.getTypeOfTestResultByType("N");
        assertNotNull(typeOfTestResult);
        assertEquals("Numeric", typeOfTestResult.getDescription());
        assertEquals("N", typeOfTestResult.getTestResultType());
    }

    @Test
    public void getResultTypeById_shouldReturnResultTypeById() {
        TypeOfTestResult typeOfTestResult = typeOfTestResultService.getTypeOfTestResultByType("N");
        ResultType resultType = typeOfTestResultService.getResultTypeById(typeOfTestResult.getId());
        assertNotNull(resultType);
        assertEquals(ResultType.REMARK, resultType);
    }

    @Test
    public void insert_shouldInsertTypeOfTestResult() {

        TypeOfTestResult typeOfTestResult = new TypeOfTestResult();
        typeOfTestResult.setDescription("Custom Test");
        typeOfTestResult.setTestResultType("X");
        typeOfTestResult.setHl7Value("CX");

        String id = typeOfTestResultService.insert(typeOfTestResult);

        assertNotNull(id);

        TypeOfTestResult savedResult = typeOfTestResultService.get(id);
        assertEquals("Custom Test", savedResult.getDescription());
        assertEquals("X", savedResult.getTestResultType());
        assertEquals("CX", savedResult.getHl7Value());
    }

    @Test
    public void update_shouldUpdateTypeOfTestResult() {

        TypeOfTestResult typeOfTestResult = typeOfTestResultService.get("1");

        String originalId = typeOfTestResult.getId();
        String originalHl7Value = typeOfTestResult.getHl7Value();

        typeOfTestResult.setDescription("Beta");
        typeOfTestResult.setTestResultType("B");
        typeOfTestResult.setHl7Value("BH");

        typeOfTestResultService.update(typeOfTestResult);

        TypeOfTestResult updatedResult = typeOfTestResultService.get("1");
        assertEquals("Beta", updatedResult.getDescription());
        assertEquals("B", updatedResult.getTestResultType());
        assertEquals("BH", updatedResult.getHl7Value());
    }

    @Test
    public void insert_shouldThrowExceptionForDuplicateRecord() {
        TypeOfTestResult existingType = typeOfTestResultService.getTypeOfTestResultByType("N");
        assertNotNull("Test setup issue: Could not find test result type 'N'", existingType);

        TypeOfTestResult duplicateType = new TypeOfTestResult();
        duplicateType.setDescription("Duplicate Numeric");
        duplicateType.setTestResultType("N");
        duplicateType.setHl7Value("NM");

        try {
            typeOfTestResultService.insert(duplicateType);
            fail("Expected an exception for duplicate record");
        } catch (LIMSDuplicateRecordException e) {

        } catch (LIMSRuntimeException e) {

        }
    }

    @Test
    public void save_shouldSaveNewTypeOfTestResult() {
        TypeOfTestResult typeOfTestResult = new TypeOfTestResult();
        typeOfTestResult.setDescription("Test");
        typeOfTestResult.setTestResultType("Z");
        typeOfTestResult.setHl7Value("TZ");

        TypeOfTestResult savedResult = typeOfTestResultService.save(typeOfTestResult);

        assertNotNull(savedResult);
        assertNotNull(savedResult.getId());
        assertEquals("Test", savedResult.getDescription());
        assertEquals("Z", savedResult.getTestResultType());
        assertEquals("TZ", savedResult.getHl7Value());
    }

    @Test
    public void save_shouldThrowExceptionForDuplicateRecord() {
        TypeOfTestResult existingType = typeOfTestResultService.getTypeOfTestResultByType("N");
        assertNotNull("Test setup issue: Could not find test result type 'N'", existingType);

        TypeOfTestResult duplicateType = new TypeOfTestResult();
        duplicateType.setDescription("Duplicate Numeric");
        duplicateType.setTestResultType("N");
        duplicateType.setHl7Value("NM");

        try {
            typeOfTestResultService.save(duplicateType);
            fail("Expected an exception for duplicate record");
        } catch (LIMSDuplicateRecordException e) {
        } catch (LIMSRuntimeException e) {

        }
    }

    @Test
    public void resultTypeEnumMethods_shouldWorkAsExpected() {
        assertTrue(ResultType.isDictionaryVariant("D"));
        assertTrue(ResultType.isDictionaryVariant("M"));
        assertTrue(ResultType.isDictionaryVariant("C"));

        assertTrue(ResultType.isMultiSelectVariant("M"));
        assertTrue(ResultType.isMultiSelectVariant("C"));

        assertTrue(ResultType.isTextOnlyVariant("A"));
        assertTrue(ResultType.isTextOnlyVariant("R"));

        assertTrue(ResultType.isNumeric("N"));
    }
}