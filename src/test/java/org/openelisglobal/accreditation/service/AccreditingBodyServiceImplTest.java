package org.openelisglobal.accreditation.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.accreditation.dao.AccreditingBodyDAO;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AccreditingBodyServiceImplTest {

    @Mock
    private AccreditingBodyDAO accreditingBodyDAO;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private AccreditingBodyServiceImpl accreditingBodyService;

    private AccreditingBody testBody;
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        testBody = new AccreditingBody();
        testBody.setId(1L);
        testBody.setCode("ISO15189");
        testBody.setLogoPath(null);

        tempDir = Files.createTempDirectory("accreditation-logos");
        ReflectionTestUtils.setField(accreditingBodyService, "accreditationLogoDir", tempDir.toString());
        when(accreditingBodyDAO.getTableName()).thenReturn("accrediting_body");
    }

    @Test
    public void testUploadLogo_ShouldSaveFileAndUpdatePath() throws IOException {
        when(accreditingBodyDAO.get(1L)).thenReturn(Optional.of(testBody));
        when(multipartFile.getOriginalFilename()).thenReturn("test-logo.png");
        when(accreditingBodyDAO.update(any(AccreditingBody.class))).thenAnswer(i -> i.getArguments()[0]);

        AccreditingBody result = accreditingBodyService.uploadLogo(1L, multipartFile, "user1");

        assertNotNull(result.getLogoPath());
        assertTrue(result.getLogoPath().contains("1_test-logo.png"));
        verify(multipartFile).transferTo(any(java.io.File.class));
        verify(accreditingBodyDAO).update(testBody);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testUploadLogo_WithInvalidId_ShouldThrowException() {
        when(accreditingBodyDAO.get(99L)).thenReturn(Optional.empty());
        accreditingBodyService.uploadLogo(99L, multipartFile, "user1");
    }

    @Test
    public void testRemoveLogo_ShouldDeleteFileAndClearPath() throws IOException {
        Path logoFile = tempDir.resolve("1_test-logo.png");
        Files.createFile(logoFile);
        testBody.setLogoPath(logoFile.toString());

        when(accreditingBodyDAO.get(1L)).thenReturn(Optional.of(testBody));
        when(accreditingBodyDAO.update(any(AccreditingBody.class))).thenAnswer(i -> i.getArguments()[0]);

        AccreditingBody result = accreditingBodyService.removeLogo(1L, "user1");

        assertNull(result.getLogoPath());
        assertFalse(Files.exists(logoFile));
        verify(accreditingBodyDAO).update(testBody);
    }
}
