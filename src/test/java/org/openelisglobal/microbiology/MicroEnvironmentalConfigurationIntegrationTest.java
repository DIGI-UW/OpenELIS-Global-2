package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.microbiology.service.MicroCultureConfigurationHandler;
import org.openelisglobal.microbiology.service.MicroReportingTrackConfigurationHandler;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeConfigurationHandler;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.springframework.beans.factory.annotation.Autowired;

@org.springframework.transaction.annotation.Transactional
public class MicroEnvironmentalConfigurationIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicroReportingTrackConfigurationHandler tracks;
    @Autowired
    private MicroCultureConfigurationHandler cultures;
    @Autowired
    private DictionaryService dictionaries;
    @Autowired
    private ObservationHistoryTypeConfigurationHandler observations;
    @Autowired
    private ObservationHistoryTypeService types;
    @Autowired
    private org.openelisglobal.labelpreset.service.LabelPresetConfigurationHandler labelConfiguration;
    @Autowired
    private org.openelisglobal.labelpreset.service.LabelPresetService labels;
    @Autowired
    private org.openelisglobal.microbiology.service.MicroSettingsConfigurationHandler settingConfiguration;
    @Autowired
    private org.openelisglobal.siteinformation.service.SiteInformationService settings;
    @Autowired
    private org.openelisglobal.microbiology.service.MicroPatientOriginConfigurationHandler originConfiguration;
    @Autowired
    private org.openelisglobal.microbiology.dao.MicroPatientOriginDAO origins;

    @Test
    public void configuredPatientOriginsReuseHistoricalIdentifiersAndKeepLocalMappings() throws Exception {
        loadOrigins();
        var origin = origins.getByCodes(java.util.List.of("INPATIENT")).get(0);
        String id = origin.getId();
        origin.setWhonetCode("LOCAL");
        origins.update(origin);
        loadOrigins();
        var reloaded = origins.getByCodes(java.util.List.of("INPATIENT")).get(0);
        assertEquals(id, reloaded.getId());
        assertEquals("LOCAL", reloaded.getWhonetCode());
        assertEquals(6, origins.getByCodes(java.util.List.of("INPATIENT", "OUTPATIENT", "INTENSIVE_CARE", "EMERGENCY",
                "LONG_TERM_CARE", "UNKNOWN")).size());
    }

    private void loadOrigins() throws Exception {
        try (var csv = getClass().getResourceAsStream("/configuration/microbiology-patient-origins/default.csv")) {
            originConfiguration.processConfiguration(csv, "default.csv");
        }
    }

    @Test
    public void configuredDefaultsPreserveSiteLabelEditsAndEnabledSelfValidationProtection() throws Exception {
        loadDefaults();
        var plate = labels.list(null, null).stream().filter(p -> p.getName().equals("Culture Plate Label")).findFirst()
                .orElseThrow();
        assertTrue(plate.getPrintsPerContainer());
        var form = new org.openelisglobal.labelpreset.form.LabelPresetForm();
        form.setName(plate.getName());
        form.setHeightMm(25);
        form.setWidthMm(77);
        form.setBarcodeType(org.openelisglobal.labelpreset.valueholder.BarcodeType.QR);
        form.setPrintsPerContainer(true);
        form.setPrintsPerSample(false);
        labels.update(plate.getId(), form, "1");
        var setting = settings.getSiteInformationByName("blockSelfValidation");
        setting.setValue("true");
        setting.setSysUserId("1");
        settings.update(setting);
        loadDefaults();
        var reloaded = labels.list(null, null).stream().filter(p -> p.getName().equals("Culture Plate Label"))
                .findFirst().orElseThrow();
        assertEquals(plate.getId(), reloaded.getId());
        assertEquals(Integer.valueOf(77), reloaded.getWidthMm());
        assertEquals("true", settings.getSiteInformationByName("blockSelfValidation").getValue());
        assertEquals(setting.getId(), settings.getSiteInformationByName("blockSelfValidation").getId());
        assertEquals(3, labels.list(null, null).stream().filter(
                p -> java.util.Set.of("Culture Plate Label", "Isolate Label", "AST Panel Label").contains(p.getName()))
                .count());
    }

    private void loadDefaults() throws Exception {
        try (var csv = getClass().getResourceAsStream("/configuration/label-presets/microbiology.csv")) {
            labelConfiguration.processConfiguration(csv, "microbiology.csv");
        }
        try (var csv = getClass().getResourceAsStream("/configuration/microbiology-settings/default.csv")) {
            settingConfiguration.processConfiguration(csv, "default.csv");
        }
    }

    @Test
    public void packagedCatalogsLoadThroughConfigurationAndReloadWithoutDuplicatingConcepts() throws Exception {
        load();
        String phone = types.getByName("envContactPhone").getId();
        String site = types.getByName("envSamplingSiteId").getId();
        String bacterial = dictionaries.getDictionaryEntryByNameAndCategoryName("Bacterial", "Reporting track").getId();
        String aerobic = dictionaries.getDictionaryEntryByNameAndCategoryName("Aerobic", "Culture atmosphere").getId();
        String reading = dictionaries.getDictionaryEntryByNameAndCategoryName("Growth observed", "Culture reading")
                .getId();
        var local = new org.openelisglobal.dictionary.valueholder.Dictionary();
        local.setDictionaryCategory(dictionaries.get(aerobic).getDictionaryCategory());
        local.setDictEntry("Site-specific atmosphere");
        local.setIsActive("Y");
        local.setSysUserId("1");
        String localId = dictionaries.insert(local);
        int typeCount = types.getAll().size();
        int trackCount = dictionaries.getActiveSortedEntriesByCategoryName("Reporting track").size();
        load();
        assertEquals(phone, types.getByName("envContactPhone").getId());
        assertEquals(site, types.getByName("envSamplingSiteId").getId());
        assertEquals(bacterial,
                dictionaries.getDictionaryEntryByNameAndCategoryName("Bacterial", "Reporting track").getId());
        assertEquals(typeCount, types.getAll().size());
        assertEquals(trackCount, dictionaries.getActiveSortedEntriesByCategoryName("Reporting track").size());
        assertEquals(aerobic,
                dictionaries.getDictionaryEntryByNameAndCategoryName("Aerobic", "Culture atmosphere").getId());
        assertEquals(reading,
                dictionaries.getDictionaryEntryByNameAndCategoryName("Growth observed", "Culture reading").getId());
        assertEquals(localId, dictionaries
                .getDictionaryEntryByNameAndCategoryName("Site-specific atmosphere", "Culture atmosphere").getId());
        assertEquals(5, dictionaries.getActiveSortedEntriesByCategoryName("Culture atmosphere").size());
        assertEquals(3, dictionaries.getActiveSortedEntriesByCategoryName("Culture reading").size());
        assertEquals(3, dictionaries.getActiveSortedEntriesByCategoryName("Culture quantity").size());
        assertEquals(3, dictionaries.getActiveSortedEntriesByCategoryName("Extend incubation reason").size());
    }

    private void load() throws Exception {
        try (var csv = getClass().getResourceAsStream("/configuration/microbiology-cultures/default.csv")) {
            assertNotNull(csv);
            cultures.processConfiguration(csv, "default.csv");
        }
        try (var csv = getClass()
                .getResourceAsStream("/configuration/observation-history-types/environmental-observation-types.csv")) {
            assertNotNull(csv);
            observations.processConfiguration(csv, "environmental-observation-types.csv");
        }
        try (var csv = getClass().getResourceAsStream("/configuration/microbiology-reporting-tracks/default.csv")) {
            assertNotNull(csv);
            tracks.processConfiguration(csv, "default.csv");
        }
    }
}
