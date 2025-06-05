package org.openelisglobal.testReflex;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.dao.ReflexRuleDAO;
import org.openelisglobal.testreflex.dao.TestReflexDAO;
import org.openelisglobal.testreflex.service.TestReflexServiceImpl;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.valueholder.TestResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestReflexServiceImplTest {

    @Mock
    private TestReflexDAO baseObjectDAO = mock(TestReflexDAO.class);

    @Mock
    ReflexRuleDAO reflexRuleDAO= mock(ReflexRuleDAO.class);

    @Mock
    ReflexRule reflexRule= mock(ReflexRule.class);

    @Mock
    Analyte  analyte = new Analyte();

    @Mock
    AuditableBaseObjectServiceImpl<TestReflex, String> auditableBaseObjectService;

    @Mock
    private AnalyteService analyteService;



    @InjectMocks
    private TestReflexServiceImpl testReflexService;

    private  List<TestReflex> expectedTestReflexList;
    private  TestResult testResult;
    private  TestReflex testReflex;
    private String testResultId;
    private String analyteId;
    private String testId;

    @Before
    public void init(){
        expectedTestReflexList= Arrays.asList(mock(TestReflex.class));
        testResult= mock(TestResult.class);
        testReflex = mock(TestReflex.class);
        analyteService= mock(AnalyteService.class);
        testResultId= "testResultId";
        analyteId= "analyteId";
        testId = "testId";

        String testId = "someId_123";
        when(testReflex.getId()).thenReturn(testId);
    }

    @Test
    public void testGetData_ReturnsVoid(){

        testReflexService.getData(testReflex);

        verify(baseObjectDAO, times(1)).getData(testReflex);
    }
    
    @Test
    public void testGetPageOfTestReflexs_ReturnsListOfTestReflex(){
        int startingRecNo=1;

        when(baseObjectDAO.getPageOfTestReflexs(startingRecNo)).thenReturn(expectedTestReflexList);

        List<TestReflex> actualTestReflexList = testReflexService.getPageOfTestReflexs(startingRecNo);

        assertEquals(expectedTestReflexList, actualTestReflexList);
        verify(baseObjectDAO, times(1)).getPageOfTestReflexs(startingRecNo);
    }

    @Test
    public void testGetTestReflexesByTestResult_ReturnsListOfTestReflex(){


        when(baseObjectDAO.getTestReflexesByTestResult(any(TestResult.class))).thenReturn(expectedTestReflexList);

        List<TestReflex> actualTestReflexList = testReflexService.getTestReflexesByTestResult(testResult);

        assertEquals(actualTestReflexList, expectedTestReflexList);
        verify(baseObjectDAO, times(1)).getTestReflexesByTestResult(testResult);
    }

    @Test
    public void testGetTestReflexsByTestAndFlag_ReturnsListOfTestReflex(){
        String testId= "testId";
        String flag= "UC";

        when(baseObjectDAO.getTestReflexsByTestAndFlag(testId, flag)).thenReturn(expectedTestReflexList);

        List<TestReflex> actualTestReflexList = testReflexService.getTestReflexsByTestAndFlag(testId, flag);

        assertEquals(actualTestReflexList, expectedTestReflexList);
        verify(baseObjectDAO, times(1)).getTestReflexsByTestAndFlag(testId, flag);

    }

    @Test
    public void testGetTotalTestReflexCount_ReturnsInteger(){
        Integer expectedTestReflexCount =10;
        when(baseObjectDAO.getTotalTestReflexCount()).thenReturn(expectedTestReflexCount);

        Integer actualTestReflexCount= testReflexService.getTotalTestReflexCount();

        assertEquals(expectedTestReflexCount, actualTestReflexCount);
        verify(baseObjectDAO, times(1)).getTotalTestReflexCount();
    }

    @Test
    public void testGetAllTestReflexs_ReturnsListOfTestReflex(){
        
        when(baseObjectDAO.getAllTestReflexs()).thenReturn(expectedTestReflexList);

        List<TestReflex> actualTestReflexList = testReflexService.getAllTestReflexs();

        assertEquals(expectedTestReflexList, actualTestReflexList);
        verify(baseObjectDAO, times(1)).getAllTestReflexs();
    }
    @Test
    public void testIsIsReflexedTest_ReturnsBoolean(){
        Analysis analysis = mock(Analysis.class);
        boolean reflexTestStatus=false;

        when(baseObjectDAO.isReflexedTest(any(Analysis.class))).thenReturn(reflexTestStatus);
        
        boolean actualReflexTestStatus = testReflexService.isReflexedTest(analysis);

        assertEquals(reflexTestStatus, actualReflexTestStatus);
        verify(baseObjectDAO, times(1)).isReflexedTest(analysis);
    }

    @Test
    public void testGetFlaggedTestReflexesByTestResult(){
        String flag = "UC";

        when(baseObjectDAO.getFlaggedTestReflexesByTestResult(testResult,flag)).thenReturn(expectedTestReflexList);
        List<TestReflex> actualTestReflexList = testReflexService.getFlaggedTestReflexesByTestResult(testResult, flag);

        assertEquals(expectedTestReflexList, actualTestReflexList);
        verify(baseObjectDAO, times(1)).getFlaggedTestReflexesByTestResult(testResult, flag);

    }

    @Test
    public void testGetTestReflexesByTestResultAndTestAnalyte(){
        TestAnalyte testAnalyte = new TestAnalyte();
        when(baseObjectDAO.getTestReflexesByTestResultAndTestAnalyte(testResult,testAnalyte)).thenReturn(expectedTestReflexList);

        List<TestReflex> actualTestReflexList = testReflexService.getTestReflexesByTestResultAndTestAnalyte(testResult, testAnalyte);

        assertEquals(expectedTestReflexList, actualTestReflexList);
        verify(baseObjectDAO, times(1)).getTestReflexesByTestResultAndTestAnalyte(testResult, testAnalyte);
    }
    @Test
    public void testGetTestReflexsByTestResultAnalyteTest(){

        when(baseObjectDAO.getTestReflexsByTestResultAnalyteTest(testResultId, analyteId, testId)).thenReturn(expectedTestReflexList);

        List<TestReflex> actualTestReflexList = testReflexService.getTestReflexsByTestResultAnalyteTest(testResultId, analyteId, testId);
        assertEquals(expectedTestReflexList, actualTestReflexList);
        verify(baseObjectDAO, times(1)).getTestReflexsByTestResultAnalyteTest(testResultId, analyteId, testId);

    }

    @Test
    public void testInsert_WhenNoduplicateExists() {
        TestReflex testReflex = mock(TestReflex.class); // or use a real object
        when(baseObjectDAO.duplicateTestReflexExists(testReflex)).thenReturn(false);
        when(auditableBaseObjectService.insert(testReflex)).thenReturn("12345");

        // Act
        String result = testReflexService.insert(testReflex);

        // Assert
        assertEquals("12345", result);
        verify(baseObjectDAO).duplicateTestReflexExists(testReflex);
        verify(auditableBaseObjectService).insert(testReflex);

    }

    private boolean duplicateTestReflexExists(TestReflex testReflex) {
        boolean result= baseObjectDAO.duplicateTestReflexExists(testReflex);
        System.out.println(result);
       return result;
    }


    @Test
    public void testSave(){

    }

    @Test
    public void testUpdate(){

    }

    @Test
    public void testGetTestReflexsByTestAnalyteId(){
        String testAnalyteId = "analyteId";

        when(baseObjectDAO.getTestReflexsByTestAnalyteId(testAnalyteId)).thenReturn(expectedTestReflexList);

        List<TestReflex> actualTestReflexList = testReflexService.getTestReflexsByTestAnalyteId(testAnalyteId);

        assertEquals(expectedTestReflexList, actualTestReflexList);
        verify(baseObjectDAO, times(1)).getTestReflexsByTestAnalyteId(testAnalyteId);

    }

    @Test
    public void testSaveOrUpdateReflexRule_WhenIdIsNull(){
        when(reflexRule.getId()).thenReturn(null);
        reflexRuleDAO.insert(reflexRule);

        verify(reflexRuleDAO, never()).update(reflexRule);
        verify(reflexRuleDAO).insert(reflexRule);

    }

    @Test
    public void testSaveOrUpdateReflexRule_WhenIdIsNotNull(){

        when(reflexRule.getId()).thenReturn(10);

        reflexRuleDAO.update(reflexRule);

        verify(reflexRuleDAO, never()).insert(reflexRule);
        verify(reflexRuleDAO).update(reflexRule);

    }

    @Test
    public void testGetAllReflexRules(){
        List<ReflexRule> expectedReflexRuleList= Arrays.asList(mock(ReflexRule.class));

        when(reflexRuleDAO.getAll()).thenReturn(expectedReflexRuleList);

        List<ReflexRule> actualReflexRuleList= testReflexService.getAllReflexRules();

        assertEquals(expectedReflexRuleList, actualReflexRuleList);
        verify(reflexRuleDAO, times(1)).getAll();

    }

    @Test
    public void testDeactivateReflexRule(){

    }

    @Test
    public void testGetTestReflexsByAnalyteAndTest(){

        when(baseObjectDAO.getTestReflexsByAnalyteAndTest(analyteId, testId)).thenReturn(expectedTestReflexList);
        List<TestReflex> actualTestReflexList = testReflexService.getTestReflexsByAnalyteAndTest(analyteId, testId);

        assertEquals(expectedTestReflexList, actualTestReflexList);
        verify(baseObjectDAO, times(1)).getTestReflexsByAnalyteAndTest(analyteId, testId);
    }

    @Test
    public void testGetReflexRuleByAnalyteId(){
        ReflexRule expectedReflexRule = mock(ReflexRule.class);
        when(reflexRuleDAO.getReflexRuleByAnalyteId(analyteId)).thenReturn(expectedReflexRule);

        ReflexRule actualReflexRule= testReflexService.getReflexRuleByAnalyteId(analyteId);

        assertEquals(expectedReflexRule, actualReflexRule);
        verify(reflexRuleDAO, times(1)).getReflexRuleByAnalyteId(analyteId);
    }




}
