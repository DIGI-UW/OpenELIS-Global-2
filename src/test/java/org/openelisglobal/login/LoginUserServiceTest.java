package org.openelisglobal.login;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;

public class LoginUserServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    LoginUserService lUserService;

    private Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2[ya]?\\$\\d\\d\\$[./0-9A-Za-z]{53}");

    String DEFAULT_ADMIN_USER_NAME = "admin";

    @Before
    public void setUp() throws Exception {
        String hashedPassword = "$2a$10$C3tY6/gZxjALzvEOkV1N/.DQyKMOoq/voG91Of3lh1t3X.iZ/O9qC";

        Assert.assertTrue(BCRYPT_PATTERN.matcher(hashedPassword).matches());

        executeDataSetWithStateManagement("testdata/system-user.xml");
    }

    @Test
    public void isUserAdmin_shouldReturnisUserAdmin() {
        LoginUser login = lUserService.get(1);

        Assert.assertTrue(lUserService.isUserAdmin(login));
    }

    @Test
    public void getPasswordExpiredDayNo_shouldReturnPasswordExpiredDayNo() {
        LoginUser login = lUserService.get(4);
        LocalDate expiryDate = login.getPasswordExpiredDate().toLocalDate();
        LocalDate today = LocalDate.now();
        long expiryDays = ChronoUnit.DAYS.between(today, expiryDate);
        Assert.assertEquals(expiryDays, lUserService.getPasswordExpiredDayNo(login));
    }

    @Test
    public void getUserProfile_shouldReturnUserProfile() {
        Date PED = Date.valueOf("2029-04-09");
        LoginUser login = lUserService.getUserProfile("Herman");

        Assert.assertEquals(PED, login.getPasswordExpiredDate());
    }

    @Test
    public void getSystemUserId_shouldReturnSystemUserId() {
        LoginUser login = lUserService.get(1);

        Assert.assertEquals(1, lUserService.getSystemUserId(login));
    }

    @Test
    public void getValidatedLogin_validCredentials_shouldReturnUser() {
        Optional<LoginUser> result = lUserService.getValidatedLogin("admin", "admin@123!");

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("admin", result.get().getLoginName());
    }

    @Test
    public void defaultAdminExists_shouldConfirmDefaultAdminExists() {

        Assert.assertTrue(lUserService.defaultAdminExists());
    }

    @Test
    public void nonDefaultAdminExists_shouldConfirmNonDefaultAdminExists() {

        Assert.assertTrue(lUserService.nonDefaultAdminExists());
    }

    @Test
    public void isHashedPassword_shouldConfirmIfHashedPassword() {

        Assert.assertTrue(
                lUserService.isHashedPassword("$2a$10$C3tY6/gZxjALzvEOkV1N/.DQyKMOoq/voG91Of3lh1t3X.iZ/O9qC"));
    }
}
