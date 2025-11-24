package org.openelisglobal.dictionary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;

@ExtendWith(MockitoExtension.class)
public class DictionaryConfigurationHandlerTest {

    @Mock
    private DictionaryService dictionaryService;

    @Mock
    private DictionaryCategoryService dictionaryCategoryService;

    @InjectMocks
    private DictionaryConfigurationHandler handler;

    private DictionaryCategory testCategory;

    @BeforeEach
    public void setup() {
        testCategory = new DictionaryCategory();
        testCategory.setId("1");
        testCategory.setCategoryName("Test Category");
        testCategory.setDescription("Test Description");
    }

    @Test
    public void testGetDomainName() {
        assertEquals("dictionaries", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testProcessConfiguration_NewCategory_NewDictionaries() throws Exception {
        // Given
        String csv = "category,dictEntry,localAbbreviation,isActive,sortOrder,loincCode\n"
                + "Test Category,Test Entry 1,TE1,Y,1,12345-6\n" + "Test Category,Test Entry 2,TE2,Y,2,\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock category service to return null (category doesn't exist)
        when(dictionaryCategoryService.getDictionaryCategoryByName("Test Category")).thenReturn(null);
        when(dictionaryCategoryService.insert(any(DictionaryCategory.class))).thenReturn("1");
        when(dictionaryCategoryService.get("1")).thenReturn(testCategory);

        // Mock dictionary service to return null (dictionaries don't exist)
        when(dictionaryService.getDictionaryByDictEntry(anyString())).thenReturn(null);
        when(dictionaryService.insert(any(Dictionary.class))).thenReturn("1", "2");

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(dictionaryCategoryService, times(1)).insert(any(DictionaryCategory.class));
        verify(dictionaryService, times(2)).insert(any(Dictionary.class));
    }

    @Test
    public void testProcessConfiguration_ExistingCategory_UpdateDictionaries() throws Exception {
        // Given
        String csv = "category,dictEntry,localAbbreviation,isActive,sortOrder\n"
                + "Test Category,Existing Entry,EE,Y,1\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock category service to return existing category
        when(dictionaryCategoryService.getDictionaryCategoryByName("Test Category")).thenReturn(testCategory);

        // Mock existing dictionary
        Dictionary existingDict = new Dictionary();
        existingDict.setId("1");
        existingDict.setDictEntry("Existing Entry");
        existingDict.setDictionaryCategory(testCategory);

        when(dictionaryService.getDictionaryByDictEntry("Existing Entry")).thenReturn(existingDict);
        when(dictionaryService.update(any(Dictionary.class))).thenReturn(existingDict);

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(dictionaryCategoryService, times(0)).insert(any(DictionaryCategory.class));
        verify(dictionaryService, times(1)).update(any(Dictionary.class));
        verify(dictionaryService, times(0)).insert(any(Dictionary.class));
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() {
        // Given
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            handler.processConfiguration(inputStream, "test.csv");
        });

        assertEquals("Dictionary configuration file test.csv is empty", exception.getMessage());
    }

    @Test
    public void testProcessConfiguration_MissingCategoryColumn_ThrowsException() {
        // Given
        String csv = "dictEntry,localAbbreviation\n" + "Test Entry,TE\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            handler.processConfiguration(inputStream, "test.csv");
        });

        assertEquals("Dictionary configuration file test.csv must have a 'category' column", exception.getMessage());
    }

    @Test
    public void testProcessConfiguration_MissingDictEntryColumn_ThrowsException() {
        // Given
        String csv = "category,localAbbreviation\n" + "Test Category,TE\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            handler.processConfiguration(inputStream, "test.csv");
        });

        assertEquals("Dictionary configuration file test.csv must have a 'dictEntry' column", exception.getMessage());
    }

    @Test
    public void testProcessConfiguration_MinimalColumns() throws Exception {
        // Given
        String csv = "category,dictEntry\n" + "Test Category,Entry 1\n" + "Test Category,Entry 2\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock category service
        when(dictionaryCategoryService.getDictionaryCategoryByName("Test Category")).thenReturn(null);
        when(dictionaryCategoryService.insert(any(DictionaryCategory.class))).thenReturn("1");
        when(dictionaryCategoryService.get("1")).thenReturn(testCategory);

        // Mock dictionary service
        when(dictionaryService.getDictionaryByDictEntry(anyString())).thenReturn(null);
        when(dictionaryService.insert(any(Dictionary.class))).thenReturn("1", "2");

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(dictionaryService, times(2)).insert(any(Dictionary.class));
    }

    @Test
    public void testProcessConfiguration_QuotedFields() throws Exception {
        // Given
        String csv = "category,dictEntry,localAbbreviation\n" + "\"Test Category\",\"Entry with, comma\",\"ABC\"\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock category service
        when(dictionaryCategoryService.getDictionaryCategoryByName("Test Category")).thenReturn(null);
        when(dictionaryCategoryService.insert(any(DictionaryCategory.class))).thenReturn("1");
        when(dictionaryCategoryService.get("1")).thenReturn(testCategory);

        // Mock dictionary service
        when(dictionaryService.getDictionaryByDictEntry("Entry with, comma")).thenReturn(null);
        when(dictionaryService.insert(any(Dictionary.class))).thenReturn("1");

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(dictionaryService, times(1)).insert(any(Dictionary.class));
    }

    @Test
    public void testProcessConfiguration_EmptyLinesIgnored() throws Exception {
        // Given
        String csv = "category,dictEntry\n" + "Test Category,Entry 1\n" + "\n" + "Test Category,Entry 2\n" + "\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock category service
        when(dictionaryCategoryService.getDictionaryCategoryByName("Test Category")).thenReturn(null);
        when(dictionaryCategoryService.insert(any(DictionaryCategory.class))).thenReturn("1");
        when(dictionaryCategoryService.get("1")).thenReturn(testCategory);

        // Mock dictionary service
        when(dictionaryService.getDictionaryByDictEntry(anyString())).thenReturn(null);
        when(dictionaryService.insert(any(Dictionary.class))).thenReturn("1", "2");

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then - should only process 2 entries despite empty lines
        verify(dictionaryService, times(2)).insert(any(Dictionary.class));
    }

    @Test
    public void testProcessConfiguration_CaseInsensitiveHeaders() throws Exception {
        // Given
        String csv = "CATEGORY,DictEntry,LocalAbbreviation\n" + "Test Category,Entry 1,E1\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock category service
        when(dictionaryCategoryService.getDictionaryCategoryByName("Test Category")).thenReturn(null);
        when(dictionaryCategoryService.insert(any(DictionaryCategory.class))).thenReturn("1");
        when(dictionaryCategoryService.get("1")).thenReturn(testCategory);

        // Mock dictionary service
        when(dictionaryService.getDictionaryByDictEntry(anyString())).thenReturn(null);
        when(dictionaryService.insert(any(Dictionary.class))).thenReturn("1");

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(dictionaryService, times(1)).insert(any(Dictionary.class));
    }

    @Test
    public void testProcessConfiguration_MultipleCategories() throws Exception {
        // Given
        String csv = "category,dictEntry\n" + "Category A,Entry A1\n" + "Category A,Entry A2\n"
                + "Category B,Entry B1\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        DictionaryCategory categoryA = new DictionaryCategory();
        categoryA.setId("1");
        categoryA.setCategoryName("Category A");

        DictionaryCategory categoryB = new DictionaryCategory();
        categoryB.setId("2");
        categoryB.setCategoryName("Category B");

        // Mock category service for multiple categories
        when(dictionaryCategoryService.getDictionaryCategoryByName("Category A")).thenReturn(null);
        when(dictionaryCategoryService.getDictionaryCategoryByName("Category B")).thenReturn(null);
        when(dictionaryCategoryService.insert(any(DictionaryCategory.class))).thenReturn("1", "2");
        when(dictionaryCategoryService.get("1")).thenReturn(categoryA);
        when(dictionaryCategoryService.get("2")).thenReturn(categoryB);

        // Mock dictionary service
        when(dictionaryService.getDictionaryByDictEntry(anyString())).thenReturn(null);
        when(dictionaryService.insert(any(Dictionary.class))).thenReturn("1", "2", "3");

        // When
        handler.processConfiguration(inputStream, "test.csv");

        // Then
        verify(dictionaryCategoryService, times(2)).insert(any(DictionaryCategory.class));
        verify(dictionaryService, times(3)).insert(any(Dictionary.class));
    }
}
