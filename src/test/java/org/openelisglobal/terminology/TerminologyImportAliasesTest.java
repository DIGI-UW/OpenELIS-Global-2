package org.openelisglobal.terminology;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.configuration.service.CatalogReferenceResolver;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionaryterminology.service.AnswerTerminologyConfigurationHandler;
import org.openelisglobal.dictionaryterminology.service.DictionaryTerminologyMappingService;
import org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testterminology.service.TerminologyConfigurationHandler;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.springframework.test.util.ReflectionTestUtils;

public class TerminologyImportAliasesTest {
    @Test
    public void answerImportKeepsCanonicalAliasRegardlessOfExistingRowOrder() throws Exception {
        for (boolean reverse : new boolean[] { false, true }) {
            DictionaryService dictionary = mock(DictionaryService.class);
            DictionaryTerminologyMappingService mappings = mock(DictionaryTerminologyMappingService.class);
            Dictionary answer = new Dictionary();
            answer.setId("42");
            when(dictionary.getDictionaryEntryByNameAndCategoryName("Detected", "Results")).thenReturn(answer);
            DictionaryTerminologyMapping alias = new DictionaryTerminologyMapping();
            alias.setSource("LOINC");
            alias.setCode("LA11882-0");
            alias.setRelationship("SAME_AS");
            DictionaryTerminologyMapping uri = new DictionaryTerminologyMapping();
            uri.setSource(TerminologySystems.LOINC);
            uri.setCode("LA11882-0");
            uri.setRelationship("BROADER_THAN");
            when(mappings.getActiveByDictionaryId("42"))
                    .thenReturn(reverse ? List.of(uri, alias) : List.of(alias, uri));
            AnswerTerminologyConfigurationHandler handler = new AnswerTerminologyConfigurationHandler();
            ReflectionTestUtils.setField(handler, "dictionaryService", dictionary);
            ReflectionTestUtils.setField(handler, "mappingService", mappings);
            handler.processConfiguration(
                    new ByteArrayInputStream(("category,dictEntry,source,code\n" + "Results,Detected,CPHL,DET\n")
                            .getBytes(StandardCharsets.UTF_8)),
                    "answers.csv");
            ArgumentCaptor<List> saved = ArgumentCaptor.forClass(List.class);
            verify(mappings).saveMappingsForDictionary(eq("42"), saved.capture(), eq("1"));
            DictionaryTerminologyMapping kept = (DictionaryTerminologyMapping) saved.getValue().get(0);
            assertEquals("LOINC", kept.getSource());
            assertEquals("SAME_AS", kept.getRelationship());
            assertEquals(2, saved.getValue().size());
            assertEquals("http://loinc.org", uri.getSource());
        }
    }

    @Test
    public void testImportNormalizesAnExistingUriWithoutMutatingTheManagedRow() throws Exception {
        TestService tests = mock(TestService.class);
        TestTerminologyMappingService mappings = mock(TestTerminologyMappingService.class);
        CatalogReferenceResolver resolver = mock(CatalogReferenceResolver.class);
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("42");
        when(resolver.resolveTest(eq("Glucose"), eq(""), anyString())).thenReturn(test);
        when(tests.getTypeOfSamples(test)).thenReturn(List.of());
        TestTerminologyMapping uri = new TestTerminologyMapping();
        uri.setSource(TerminologySystems.LOINC);
        uri.setCode("2345-7");
        uri.setRelationship("SAME_AS");
        when(mappings.getActiveByTestId("42")).thenReturn(List.of(uri));
        TerminologyConfigurationHandler handler = new TerminologyConfigurationHandler();
        ReflectionTestUtils.setField(handler, "testService", tests);
        ReflectionTestUtils.setField(handler, "terminologyService", mappings);
        ReflectionTestUtils.setField(handler, "resolver", resolver);
        handler.processConfiguration(new ByteArrayInputStream(
                ("testName,source,code\n" + "Glucose,http://loinc.org,2345-7\n").getBytes(StandardCharsets.UTF_8)),
                "tests.csv");
        ArgumentCaptor<List> saved = ArgumentCaptor.forClass(List.class);
        verify(mappings).saveMappingsForTest(eq("42"), saved.capture(), eq("1"));
        assertEquals(1, saved.getValue().size());
        TestTerminologyMapping kept = (TestTerminologyMapping) saved.getValue().get(0);
        assertEquals("LOINC", kept.getSource());
        assertEquals("http://loinc.org", uri.getSource());
    }
}
