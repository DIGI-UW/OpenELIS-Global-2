package org.openelisglobal.unitofmeasure.service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.LocaleChangeListener;
import org.openelisglobal.internationalization.GlobalLocaleResolver;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.unitofmeasure.dao.UnitOfMeasureDAO;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.LocaleResolver;

@Service
@DependsOn({ "springContext" })
public class UnitOfMeasureServiceImpl extends AuditableBaseObjectServiceImpl<UnitOfMeasure, String>
        implements UnitOfMeasureService, LocaleChangeListener {

    private Map<String, String> unitOfMeasureIdToNameMap = null;

    @Autowired
    protected UnitOfMeasureDAO unitOfMeasureDAO;
    @Autowired
    private LocaleResolver localeResolver;

    @PostConstruct
    private void initilaize() {
        if (localeResolver instanceof GlobalLocaleResolver) {
            ((GlobalLocaleResolver) localeResolver).addLocalChangeListener(this);
        }
    }

    @PostConstruct
    private void initializeGlobalVariables() {
        createTestIdToNameMap();
    }

    UnitOfMeasureServiceImpl() {
        super(UnitOfMeasure.class);
    }

    @Override
    protected UnitOfMeasureDAO getBaseObjectDAO() {
        return unitOfMeasureDAO;
    }

    @Override
    public void localeChanged(String locale) {
        testNamesChanged();
    }

    @Override
    public void refreshNames() {
        testNamesChanged();
    }

    public void testNamesChanged() {
        createTestIdToNameMap();
    }

    public synchronized String getUserLocalizedUnitOfMeasureName(String unitOfMeasureId) {
        String name = unitOfMeasureIdToNameMap.get(unitOfMeasureId);
        return name == null ? "" : name;
    }

    private synchronized void createTestIdToNameMap() {
        unitOfMeasureIdToNameMap = new HashMap<>();

        List<UnitOfMeasure> unitOfMeasures = unitOfMeasureDAO.getAll();

        for (UnitOfMeasure unitOfMeasure : unitOfMeasures) {
            unitOfMeasureIdToNameMap.put(unitOfMeasure.getId(),
                    buildUnitOfMeasureName(unitOfMeasure).replace("\n", " "));
        }
    }

    private String buildUnitOfMeasureName(UnitOfMeasure unitOfMeasure) {
        // Localization localization = unitOfMeasure.getLocalization();
        //
        // if( LANGUAGE_LOCALE.equals(
        // ConfigurationProperties.LOCALE.FRENCH.getRepresentation()
        // )){
        // return localization.getFrench();
        // }else{
        // return localization.getEnglish();
        // }
        // }

        // public static List<Test> getTestsInSection(String id) {
        // return TestServiceImpl.getTestsInTestSectionById(id);
        return ""; // just for compile
    }

    @Override
    @Transactional(readOnly = true)
    public UnitOfMeasure getUnitOfMeasureById(String uomId) {
        return getBaseObjectDAO().getUnitOfMeasureById(uomId);
    }

    @Override
    @Transactional(readOnly = true)
    public UnitOfMeasure getUnitOfMeasureByName(UnitOfMeasure unitOfMeasure) {
        return getMatch("unitOfMeasureName", unitOfMeasure.getUnitOfMeasureName()).orElse(null);
    }

    @Override
    public String insert(UnitOfMeasure unitOfMeasure) {
        if (getBaseObjectDAO().duplicateUnitOfMeasureExists(unitOfMeasure)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + unitOfMeasure.getUnitOfMeasureName());
        }
        return super.insert(unitOfMeasure);
    }

    @Override
    public UnitOfMeasure save(UnitOfMeasure unitOfMeasure) {
        if (getBaseObjectDAO().duplicateUnitOfMeasureExists(unitOfMeasure)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + unitOfMeasure.getUnitOfMeasureName());
        }
        return super.save(unitOfMeasure);
    }

    @Override
    public UnitOfMeasure update(UnitOfMeasure unitOfMeasure) {
        if (getBaseObjectDAO().duplicateUnitOfMeasureExists(unitOfMeasure)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + unitOfMeasure.getUnitOfMeasureName());
        }
        return super.update(unitOfMeasure);
    }

    @Override
    public Localization getLocalizationForUnitOfMeasure(String id) {
        UnitOfMeasure unitOfMeasure = getUnitOfMeasureById(id);
        Localization localization = unitOfMeasure != null ? unitOfMeasure.getLocalization() : null;
        Hibernate.initialize(localization);
        return localization;
    }
}
