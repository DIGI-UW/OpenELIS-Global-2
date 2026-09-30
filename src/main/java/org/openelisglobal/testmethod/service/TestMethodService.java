package org.openelisglobal.testmethod.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.testmethod.valueholder.TestMethod;

public interface TestMethodService extends BaseObjectService<TestMethod, String> {

    // ── DTOs ─────────────────────────────────────────────────────────────────

    class TestMethodDto {
        public String id;
        public String methodId;
        public String methodName;
        public String methodCode;
        public boolean isDefault;
        public String effectiveDate;
    }

    class InlineCreateData {
        public String nameEnglish;
        public String nameFrench;
        public String code;
        public boolean isDefault;
        public Date effectiveDate;
        public String sysUserId;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    TestMethod get(String id);

    /**
     * Nullable lookup of a test-method link by id — returns null instead of
     * throwing ObjectNotFoundException when the link does not exist, so callers can
     * return 404 rather than surfacing a 500.
     */
    TestMethod findLinkById(String id);

    List<TestMethod> getActiveTestMethodsByTestId(String testId);

    boolean testMethodLinkExists(String testId, String methodId);

    /**
     * Links an existing method to a test. If isDefault is true, clears any existing
     * default for the test first to enforce the single-default invariant.
     */
    TestMethod linkMethod(TestMethod testMethod);

    /**
     * Updates default flag and/or effective date. Enforces single-default invariant
     * if isDefault is being set to true.
     */
    TestMethod updateLink(TestMethod testMethod);

    void removeLink(String testMethodId, String sysUserId);

    void copyMethodsFromTest(String sourceTestId, String targetTestId, String sysUserId);

    String getDefaultMethodId(String testId);

    List<IdValuePair> getMethodDisplayListForTest(String testId);

    List<TestMethodDto> getLinkedMethodDtos(String testId);

    TestMethodDto linkMethodDto(TestMethod testMethod);

    TestMethodDto createAndLinkMethod(String testId, InlineCreateData data);

    TestMethodDto updateLinkDto(TestMethod testMethod);
}
