package org.openelisglobal.dictionary.rest.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.form.DictionaryMenuForm;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class DictionaryMenuRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/dictionary.xml");
    }

    @Test
    public void getDictionaryMenuList_shouldReturnDictionaryMenu() throws Exception {
        MvcResult mvcResult = super.mockMvc.perform(get("/rest/DictionaryMenu").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        List<DictionaryMenuForm> menuList = Arrays.asList(super.mapFromJson(content, DictionaryMenuForm[].class));
        assertThat(menuList.get(0).getMenuList().get(0).getId(), is("1"));
        assertThat(menuList.get(0).getMenuList().get(0).getIsActive(), is("Y"));
        assertThat(menuList.get(0).getMenuList().get(0).getDictEntry(), is("Dictionary Entry 1"));
        assertThat(menuList.get(0).getMenuList().get(0).getSortOrder(), is(1));
        assertThat(menuList.get(0).getMenuList().get(0).getDictionaryCategory().getCategoryName(),
                is("Category Name 1"));
    }

    @Test
    public void fetchDictionaryCategories_shouldFetchDictionaryDescriptions() throws Exception {
        MvcResult mvcResult = super.mockMvc.perform(get("/rest/dictionary-categories")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        List<DictionaryCategory> menuList = Arrays.asList(super.mapFromJson(content, DictionaryCategory[].class));
        assertThat(menuList, notNullValue());
    }

    @Test
    public void showDeleteDictionary_shouldSuccessfullyDeleteDictionary() throws Exception {
        MvcResult getMenu = super.mockMvc.perform(get("/rest/DictionaryMenu").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = getMenu.getResponse().getStatus();
        assertEquals(200, status);
        String content = getMenu.getResponse().getContentAsString();
        List<DictionaryMenuForm> menuList = Arrays.asList(super.mapFromJson(content, DictionaryMenuForm[].class));
        String idToBeDeleted = menuList.get(0).getMenuList().get(2).getId();

        // deleting the selected ID
        MvcResult mvcResult = super.mockMvc.perform(post("/rest/delete-dictionary").param("selectedIDs", idToBeDeleted))
                .andReturn();

        status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        content = mvcResult.getResponse().getContentAsString();
        assertEquals(content, "Dictionary Menu deleted successfully");
    }

    private Dictionary createDictionaryObject() {
        Random random = new Random();
        Dictionary dictionary = new Dictionary();
        dictionary.setId(String.valueOf(random.nextInt()));
        dictionary.setSortOrder(random.nextInt(1000));
        dictionary.setDictionaryCategory(dictionaryCategoryService.getDictionaryCategoryByName("CG"));
        dictionary.setDictEntry("entry for test " + random.nextInt());
        dictionary.setIsActive(random.nextBoolean() ? "Y" : "N");
        dictionary.setLocalAbbreviation("HEC" + random.nextInt());
        return dictionary;
    }
}
