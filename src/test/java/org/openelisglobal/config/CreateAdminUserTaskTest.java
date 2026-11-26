package org.openelisglobal.config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.security.DaemonContextExecutor;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

public class CreateAdminUserTaskTest {
    @Rule
    public TemporaryFolder files = new TemporaryFolder();
    private final LoginUserService logins = mock(LoginUserService.class);
    private final CreateAdminUserTask task = new CreateAdminUserTask();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private Field factoryField;
    private Object previousFactory;
    private Path passwordFile;

    @Before
    public void setUp() throws Exception {
        factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        previousFactory = factoryField.get(null);
        AutowireCapableBeanFactory factory = mock(AutowireCapableBeanFactory.class);
        when(factory.getBean(DefaultConfigurationProperties.class))
                .thenReturn(mock(DefaultConfigurationProperties.class));
        factoryField.set(null, factory);
        DaemonContextExecutor executor = mock(DaemonContextExecutor.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).executeAsDaemon(any(Runnable.class));
        ReflectionTestUtils.setField(task, "loginService", logins);
        ReflectionTestUtils.setField(task, "daemonContextExecutor", executor);
        ReflectionTestUtils.setField(task, "daemonSysUserId", "17");
        passwordFile = files.getRoot().toPath().resolve("initial-password");
        ReflectionTestUtils.setField(task, "initialPasswordFile", passwordFile.toString());
        doAnswer(invocation -> {
            ((LoginUser) invocation.getArgument(0)).setPassword(encoder.encode(invocation.getArgument(1)));
            return null;
        }).when(logins).hashPassword(any(LoginUser.class), anyString());
    }

    @After
    public void restoreFactory() throws Exception {
        factoryField.set(null, previousFactory);
    }

    @Test
    public void freshAdministratorUsesInstallationPasswordAndStoresOnlyHash() throws Exception {
        Files.writeString(passwordFile, "PrivateAbc123!$\r\n");
        initialize();
        ArgumentCaptor<LoginUser> captured = ArgumentCaptor.forClass(LoginUser.class);
        verify(logins).insert(captured.capture());
        LoginUser login = captured.getValue();
        assertEquals("admin", login.getLoginName());
        assertEquals("Y", login.getIsAdmin());
        assertEquals("17", login.getSysUserId());
        assertTrue(encoder.matches("PrivateAbc123!$", login.getPassword()));
        assertFalse(encoder.matches("adminADMIN!", login.getPassword()));
        assertNotEquals("PrivateAbc123!$", login.getPassword());
    }

    @Test
    public void restartDoesNotReadSecretOrChangeExistingAdministrator() {
        when(logins.defaultAdminExists()).thenReturn(true);
        initialize(); // file deliberately absent
        verify(logins, never()).hashPassword(any(), anyString());
        verify(logins, never()).insert(any());
    }

    @Test
    public void existingNamedAdministratorPreventsBootstrap() throws Exception {
        Files.writeString(passwordFile, "invalid");
        when(logins.nonDefaultAdminExists()).thenReturn(true);
        initialize();
        verify(logins, never()).hashPassword(any(), anyString());
        verify(logins, never()).insert(any());
    }

    @Test
    public void missingSecretStopsFreshStartupWithoutFallback() {
        assertThrows(IllegalStateException.class, this::initialize);
        verify(logins, never()).insert(any());
    }

    @Test
    public void blankWeakOversizedAndHashedSecretsAreRejectedWithoutDisclosure() throws Exception {
        for (String value : new String[] { "", "\r\n", "weak", "Ab1!".repeat(19), "A".repeat(300),
                "admin:$2a$12$notAPlaintextPassword", "Abc123!\nInjected" }) {
            Files.writeString(passwordFile, value);
            IllegalStateException failure = assertThrows(IllegalStateException.class, this::initialize);
            if (!value.isEmpty())
                assertFalse(failure.getMessage().contains(value));
        }
        verify(logins, never()).hashPassword(any(), anyString());
        verify(logins, never()).insert(any());
    }

    @Test
    public void directoryCannotBeUsedAsPasswordFile() throws Exception {
        Files.createDirectory(passwordFile);
        assertThrows(IllegalStateException.class, this::initialize);
        verify(logins, never()).insert(any());
    }

    private void initialize() {
        ReflectionTestUtils.invokeMethod(task, "ensureAdminUserIsCreated");
    }
}
