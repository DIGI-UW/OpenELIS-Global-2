package org.openelisglobal.testmethod.service;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.testmethod.dao.TestMethodDAO;
import org.openelisglobal.testmethod.service.TestMethodService.InlineCreateData;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;
import org.openelisglobal.testmethod.valueholder.TestMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestMethodServiceImpl extends AuditableBaseObjectServiceImpl<TestMethod, String>
        implements TestMethodService {

    @Autowired
    protected TestMethodDAO baseObjectDAO;

    @Autowired
    private MethodService methodService;

    @Autowired
    private LocalizationService localizationService;

    TestMethodServiceImpl() {
        super(TestMethod.class);
    }

    @Override
    protected TestMethodDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestMethod> getActiveTestMethodsByTestId(String testId) {
        return baseObjectDAO.getActiveTestMethodsByTestId(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean testMethodLinkExists(String testId, String methodId) {
        return baseObjectDAO.testMethodLinkExists(testId, methodId);
    }

    @Override
    @Transactional
    public TestMethod linkMethod(TestMethod testMethod) {
        if (testMethod.getIsDefaultMethod()) {
            baseObjectDAO.clearDefaultsForTest(testMethod.getTestId(), testMethod.getSysUserId());
        }
        insert(testMethod);
        return testMethod;
    }

    @Override
    @Transactional
    public TestMethod updateLink(TestMethod testMethod) {
        if (testMethod.getIsDefaultMethod()) {
            baseObjectDAO.clearDefaultsForTest(testMethod.getTestId(), testMethod.getSysUserId());
        }
        baseObjectDAO.updateIsDefaultAndEffectiveDate(testMethod.getId(), testMethod.getIsDefaultMethod(),
                testMethod.getEffectiveDate());
        return testMethod;
    }

    @Override
    @Transactional
    public void removeLink(String testMethodId, String sysUserId) {
        baseObjectDAO.deactivateLink(testMethodId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getDefaultMethodId(String testId) {
        return baseObjectDAO.getActiveTestMethodsByTestId(testId).stream().filter(TestMethod::getIsDefaultMethod)
                .map(TestMethod::getMethodId).findFirst().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdValuePair> getMethodDisplayListForTest(String testId) {
        List<TestMethod> links = baseObjectDAO.getActiveTestMethodsByTestId(testId);
        if (links.isEmpty()) {
            return null;
        }
        return links.stream().map(tm -> {
            Method m = methodService.get(tm.getMethodId());
            return m != null ? new IdValuePair(m.getId(), m.getLocalizedValue()) : null;
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TestMethodDto linkMethodDto(TestMethod testMethod) {
        return toDto(linkMethod(testMethod));
    }

    @Override
    @Transactional
    public TestMethodDto updateLinkDto(TestMethod testMethod) {
        return toDto(updateLink(testMethod));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestMethodDto> getLinkedMethodDtos(String testId) {
        return baseObjectDAO.getActiveTestMethodsByTestId(testId).stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TestMethodDto createAndLinkMethod(String testId, InlineCreateData data) {
        Localization localization = new Localization();
        localization.setDescription("method name");
        localization.setSysUserId(data.sysUserId);
        // Seed all active locales — English value for "en", French for "fr",
        // English as fallback for any other locale installed on this deployment.
        for (java.util.Locale locale : localization.getAllActiveLocales()) {
            String lang = locale.getLanguage();
            if ("fr".equals(lang)) {
                localization.setLocalizedValue(lang, data.nameFrench);
            } else {
                localization.setLocalizedValue(lang, data.nameEnglish);
            }
        }

        Method method = new Method();
        method.setMethodName(data.nameEnglish);
        method.setDescription(data.nameEnglish);
        method.setCode(data.code.toUpperCase());
        method.setIsActive(IActionConstants.YES);
        method.setNameKey("method." + data.nameEnglish.replaceAll(" ", "_"));
        method.setSysUserId(data.sysUserId);

        localizationService.insert(localization);
        method.setLocalization(localization);
        methodService.insert(method);

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.METHODS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.METHODS_INACTIVE);

        TestMethod tm = new TestMethod();
        tm.setTestId(testId);
        tm.setMethodId(method.getId());
        tm.setIsDefaultMethod(data.isDefault);
        tm.setEffectiveDate(data.effectiveDate);
        tm.setIsActive(IActionConstants.YES);
        tm.setSysUserId(data.sysUserId);
        return toDto(linkMethod(tm));
    }

    private TestMethodDto toDto(TestMethod tm) {
        TestMethodDto dto = new TestMethodDto();
        dto.id = tm.getId();
        dto.methodId = tm.getMethodId();
        dto.isDefault = tm.getIsDefaultMethod();
        dto.effectiveDate = tm.getEffectiveDate() != null ? tm.getEffectiveDate().toString() : null;
        Method m = methodService.get(tm.getMethodId());
        if (m != null) {
            dto.methodName = m.getLocalizedValue();
            dto.methodCode = m.getCode();
        }
        return dto;
    }

    @Override
    @Transactional
    public void copyMethodsFromTest(String sourceTestId, String targetTestId, String sysUserId) {
        List<TestMethod> sourceLinks = baseObjectDAO.getActiveTestMethodsByTestId(sourceTestId);
        for (TestMethod source : sourceLinks) {
            if (!baseObjectDAO.testMethodLinkExists(targetTestId, source.getMethodId())) {
                TestMethod copy = new TestMethod();
                copy.setTestId(targetTestId);
                copy.setMethodId(source.getMethodId());
                copy.setEffectiveDate(source.getEffectiveDate());
                copy.setIsDefaultMethod(false);
                copy.setIsActive(IActionConstants.YES);
                copy.setSysUserId(sysUserId);
                insert(copy);
            }
        }
    }
}
