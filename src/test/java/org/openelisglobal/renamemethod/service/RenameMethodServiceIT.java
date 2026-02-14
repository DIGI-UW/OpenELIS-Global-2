package org.openelisglobal.renamemethod.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.hibernate.ObjectNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.renamemethod.valueholder.RenameMethod;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service integration test for RenameMethod: validates service → persistence → response
 * end-to-end. Covers valid rename, duplicate method name, invalid/empty name, and non-existent
 * method ID.
 */
public class RenameMethodServiceIT extends BaseWebContextSensitiveTest {

    @Autowired
    private RenameMethodService renameMethodService;

    @Autowired
    private LocalizationService localizationService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/method.xml");
    }

    @Test
    public void update_shouldPersistNewName_whenValidRename() {
        RenameMethod method = renameMethodService.get("1");
        assertNotNull("Fixture method 1 should exist", method);
        assertEquals("therapy", method.getMethodName());

        method.setMethodName("therapy-renamed");
        method.setSysUserId("1");
        RenameMethod updated = renameMethodService.update(method);

        assertNotNull(updated);
        assertEquals("therapy-renamed", updated.getMethodName());

        RenameMethod fromDb = renameMethodService.get("1");
        assertNotNull(fromDb);
        assertEquals("therapy-renamed", fromDb.getMethodName());
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void update_shouldThrowException_whenDuplicateMethodName() {
        RenameMethod method = renameMethodService.get("1");
        assertNotNull("Fixture method 1 should exist", method);
        method.setMethodName("imagining");
        method.setSysUserId("1");
        renameMethodService.update(method);
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void insert_shouldThrowException_whenDuplicateMethodName() {
        RenameMethod method = new RenameMethod();
        method.setMethodName("therapy");
        method.setDescription("duplicate description");
        Localization loc = localizationService.get("1");
        assertNotNull(loc);
        method.setLocalization(loc);
        renameMethodService.insert(method);
    }

    @Test
    public void update_shouldThrowException_whenMethodNameIsNull() {
        RenameMethod method = renameMethodService.get("2");
        assertNotNull(method);
        method.setMethodName(null);
        method.setSysUserId("1");
        try {
            renameMethodService.update(method);
            fail("Expected exception when method name is null");
        } catch (NullPointerException e) {
            // duplicateMethodExists calls getMethodName().toLowerCase()
        } catch (Exception e) {
            // other persistence/validation exception acceptable
        }
    }

    @Test
    public void get_shouldThrowException_whenNonExistentMethodId() {
        try {
            renameMethodService.get("99999");
            fail("Expected ObjectNotFoundException for non-existent id");
        } catch (ObjectNotFoundException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void insert_shouldPersistAndReturnId_whenValidMethod() {
        Localization loc = localizationService.get("1");
        assertNotNull(loc);
        RenameMethod method = new RenameMethod();
        method.setMethodName("new-method-name");
        method.setDescription("New method description");
        method.setIsActive("Y");
        method.setLocalization(loc);

        String id = renameMethodService.insert(method);
        assertNotNull(id);

        RenameMethod fromDb = renameMethodService.get(id);
        assertNotNull(fromDb);
        assertEquals("new-method-name", fromDb.getMethodName());
        assertEquals("New method description", fromDb.getDescription());
    }

    @Test
    public void delete_shouldSetInactive_whenMethodExists() {
        RenameMethod method = renameMethodService.get("3");
        assertNotNull(method);
        assertEquals("Y", method.getIsActive());

        method.setSysUserId("1");
        renameMethodService.delete(method);

        RenameMethod inactive = renameMethodService.get("3");
        assertNotNull(inactive);
        assertEquals("N", inactive.getIsActive());
    }

    @Test
    public void getLocalizationForRenameMethod_shouldReturnLocalization_whenMethodExists() {
        Localization loc = renameMethodService.getLocalizationForRenameMethod("1");
        assertNotNull(loc);
        assertNotNull(loc.getEnglish());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void getLocalizationForRenameMethod_shouldThrow_whenMethodDoesNotExist() {
        renameMethodService.getLocalizationForRenameMethod("99999");
    }
}
