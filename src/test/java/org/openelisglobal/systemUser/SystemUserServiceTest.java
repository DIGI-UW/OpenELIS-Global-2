package org.openelisglobal.systemUser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;

public class SystemUserServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SystemUserService systemUserService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/system-user.xml");
    }

    public void testDataInDataBase() {
        List<SystemUser> systemUsers = systemUserService.getAll();
        Assert.assertFalse("The system_users table should not be empty!", systemUsers.isEmpty());
        for (SystemUser systemUser : systemUsers) {
            Assert.assertNotNull("System user last name should not be null!", systemUser.getLastName());
            Assert.assertFalse("System user last name should not be empty!", systemUser.getLastName().trim().isEmpty());
        }
    }

    @Test
    public void getAllSystemUsers_shouldReturnAllSystemUsers() {
        List<SystemUser> systemUsers = systemUserService.getAllSystemUsers();
        assertTrue(systemUsers.size() == 4);
    }

    @Test
    public void delete_shouldInactivateUser() {
        SystemUser systemUser = systemUserService.get("3");
        systemUserService.delete(systemUser);
        SystemUser updatedUser = systemUserService.getUserById("3");
        assertEquals("N", updatedUser.getIsActive());
    }

    @Test
    public void saveSystemUser_shouldSaveSystemUser() {
        SystemUser systemUser = new SystemUser();
        systemUser.setFirstName("Robert");
        systemUser.setLastName("Derick");
        systemUser.setLoginName("Debert");
        systemUser.setIsActive("Y");
        systemUser.setExternalId("1234EXT");
        systemUser.setIsEmployee("Y");
        systemUserService.save(systemUser);
        List<SystemUser> systemUsers = systemUserService.getAllSystemUsers();
        assertTrue(systemUsers.size() == 5);

    }

    @Test
    public void getData_shouldReturnSystemUser() {
        SystemUser systemUser = systemUserService.get("3");
        systemUserService.getData(systemUser);
        assertEquals("John", systemUser.getFirstName());
        assertEquals("Doe", systemUser.getLastName());
        assertEquals("jdoe", systemUser.getLoginName());
    }

    @Test
    public void update_shouldUpdateSystemUser() {
        SystemUser systemUser = systemUserService.get("3");
        systemUser.setFirstName("James");
        systemUser.setLastName("Grant");
        systemUser.setLoginName("jgrant");
        systemUserService.update(systemUser);
        SystemUser updatedUser = systemUserService.getUserById("3");
        assertEquals("James", updatedUser.getFirstName());
        assertEquals("Grant", updatedUser.getLastName());
        assertEquals("jgrant", updatedUser.getLoginName());
    }

    @Test
    public void insert_shouldInsertSystemUser() {
        SystemUser systemUser = new SystemUser();
        systemUser.setFirstName("Robert");
        systemUser.setLastName("Derick");
        systemUser.setLoginName("Debert");
        systemUser.setIsActive("Y");
        systemUser.setExternalId("1234EXT");
        systemUser.setIsEmployee("Y");
        systemUserService.insert(systemUser);
        List<SystemUser> systemUsers = systemUserService.getAllSystemUsers();
        assertTrue(systemUsers.size() == 5);
    }

    @Test
    public void getTotalSystemUserCount_shouldReturnTotalSystemUserCount() {
        Integer count = systemUserService.getTotalSystemUserCount();
        assertEquals(4, count.intValue());
    }

    @Test
    public void getuserById_shouldReturnUserById() {
        SystemUser systemUser = systemUserService.getUserById("3");
        assertEquals("John", systemUser.getFirstName());
        assertEquals("Doe", systemUser.getLastName());
        assertEquals("jdoe", systemUser.getLoginName());
    }

    @Test
    public void getDataForLoginUser_shouldReturnDataForLoginUser() {
        SystemUser systemUser = systemUserService.getDataForLoginUser("jdoe");
        assertEquals("John", systemUser.getFirstName());
        assertEquals("Doe", systemUser.getLastName());
        assertEquals("jdoe", systemUser.getLoginName());
    }

    @Test
    public void getPageOfSystemUsers_shouldReturnPageOfSystemUsers() {
        List<SystemUser> systemUsers = systemUserService.getPageOfSystemUsers(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(systemUsers.size() <= expectedPages);
    }

    @Test
    public void getPagesOfSearchedUsers_shouldReturnPagesOfSearchedUsers() {
        SystemUser systemUser = systemUserService.get("3");
        String firstName = systemUser.getFirstName();
        List<SystemUser> systemUsers = systemUserService.getPagesOfSearchedUsers(1, firstName);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(systemUsers.size() <= expectedPages);
    }

    @Test
    public void getTotalSearchedUserCount_shouldReturnTotalSearchedUserCount() {
        Integer count = systemUserService.getTotalSearchedUserCount("John");
        assertEquals(2, count.intValue());
    }
}
