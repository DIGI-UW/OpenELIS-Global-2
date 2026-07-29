package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.hibernate.ObjectNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link AnalyzerTypeService} /
 * {@link AnalyzerTypeServiceImpl}. Uses dedicated fixture with high analyzer
 * PKs (910+) so inserts in tests do not collide with DBUnit-loaded rows or
 * PostgreSQL {@code analyzer_seq}.
 */
public class AnalyzerTypeServiceTest extends BaseWebContextSensitiveTest {

    // Centralized Fixture Constants
    private static final String TYPE_COBAS = "901";
    private static final String TYPE_COBAS_NAME = "Cobas 6800 Type";
    private static final String TYPE_COBAS_PLUGIN = "oe.plugin.analyzer.Cobas6800";
    private static final String TYPE_COBAS_INSTANCE_NAME = "Cobas 6800";

    private static final String TYPE_ABL800 = "902";
    private static final String TYPE_ABL800_NAME = "ABL800 Type";
    private static final String TYPE_ABL800_PLUGIN = "oe.plugin.analyzer.ABL800";
    private static final String TYPE_ABL800_INSTANCE_NAME = "ABL800 FLEX";
    private static final String ANALYZER_ABL800_ID = "911";

    private static final String TYPE_NO_INSTANCES = "904";
    private static final String TYPE_NO_INSTANCES_NAME = "Empty Instance Type";

    private static final String TYPE_INACTIVE = "905";

    private static final String TYPE_GENERIC_PLUGIN = "906";

    private static final String UNKNOWN_TYPE_ID = "99999";

    @Autowired
    private AnalyzerTypeService analyzerTypeService;

    @Autowired
    private AnalyzerService analyzerService; // For round-trip persistence verification

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analyzer-type-service.xml");
    }

    @Test
    public void getAnalyzerTypeByName_shouldReturnCorrectType() {
        AnalyzerType type = analyzerTypeService.getAnalyzerTypeByName(TYPE_COBAS_NAME);

        Assert.assertNotNull(type);
        Assert.assertEquals(TYPE_COBAS, type.getId());
        Assert.assertEquals(TYPE_COBAS_PLUGIN, type.getPluginClassName());
    }

    @Test
    public void getByPluginClassName_shouldReturnMatchingType() {
        Optional<AnalyzerType> typeOpt = analyzerTypeService.getByPluginClassName(TYPE_ABL800_PLUGIN);

        Assert.assertTrue(typeOpt.isPresent());
        Assert.assertEquals(TYPE_ABL800, typeOpt.get().getId());
        Assert.assertEquals(TYPE_ABL800_NAME, typeOpt.get().getName());
    }

    @Test
    public void findMatchingType_shouldReturnMatchingTypeWhenPatternMatches() {
        AnalyzerType newType = new AnalyzerType();
        newType.setName("Regex Type");
        newType.setPluginClassName("oe.plugin.analyzer.RegexType");
        newType.setIdentifierPattern("REGEX_.*");
        newType.setSysUserId(TEST_SYS_USER_ID);
        analyzerTypeService.insert(newType);

        Optional<AnalyzerType> matchedOpt = analyzerTypeService.findMatchingType("REGEX_123");

        Assert.assertTrue(matchedOpt.isPresent());
        Assert.assertEquals("Regex Type", matchedOpt.get().getName());
    }

    @Test
    public void findMatchingType_shouldReturnEmptyWhenNoMatch() {
        Optional<AnalyzerType> matchedOpt = analyzerTypeService.findMatchingType("NONEXISTENT");

        Assert.assertFalse(matchedOpt.isPresent());
    }

    @Test
    public void getAllActiveTypes_shouldReturnOnlyActiveTypes() {
        List<AnalyzerType> activeTypes = analyzerTypeService.getAllActiveTypes();

        // Resilient assertions: verify known active types are present and inactive are
        // absent
        Assert.assertTrue(activeTypes.stream().anyMatch(t -> TYPE_COBAS.equals(t.getId())));
        Assert.assertTrue(activeTypes.stream().anyMatch(t -> TYPE_GENERIC_PLUGIN.equals(t.getId())));
        Assert.assertFalse(activeTypes.stream().anyMatch(t -> TYPE_INACTIVE.equals(t.getId())));
    }

    @Test
    public void getGenericPluginTypes_shouldReturnOnlyGenericPluginTypes() {
        List<AnalyzerType> genericTypes = analyzerTypeService.getGenericPluginTypes();

        Assert.assertTrue(genericTypes.stream().anyMatch(t -> TYPE_GENERIC_PLUGIN.equals(t.getId())));
        Assert.assertTrue(genericTypes.stream().allMatch(AnalyzerType::isGenericPlugin));
    }

    @Test
    public void getAllWithInitializedInstances_shouldReturnAllTypesWithEagerInstances() {
        List<AnalyzerType> types = analyzerTypeService.getAllWithInitializedInstances();

        AnalyzerType cobas = types.stream().filter(t -> TYPE_COBAS.equals(t.getId())).findFirst().orElse(null);
        Assert.assertNotNull(cobas);
        Assert.assertEquals(1, cobas.getInstances().size());
        Assert.assertEquals(TYPE_COBAS_INSTANCE_NAME, cobas.getInstances().get(0).getName());

        AnalyzerType emptyType = types.stream().filter(t -> TYPE_NO_INSTANCES.equals(t.getId())).findFirst()
                .orElse(null);
        Assert.assertNotNull(emptyType);
        Assert.assertTrue(emptyType.getInstances().isEmpty());
    }

    @Test
    public void getInstancesForType_shouldThrowWhenTypeIdNotFound() {
        ObjectNotFoundException thrown = Assert.assertThrows(ObjectNotFoundException.class, () -> {
            analyzerTypeService.getInstancesForType(UNKNOWN_TYPE_ID);
        });
        Assert.assertTrue(thrown.getMessage().contains(UNKNOWN_TYPE_ID));
    }

    @Test
    @Transactional
    public void getInstancesForType_shouldReturnInstancesWithinTransaction() {
        List<Analyzer> instances = analyzerTypeService.getInstancesForType(TYPE_COBAS);

        Assert.assertEquals(1, instances.size());
        Assert.assertEquals(TYPE_COBAS_INSTANCE_NAME, instances.get(0).getName());
    }

    @Test
    @Transactional
    public void getInstancesForType_shouldReturnEmptyWhenTypeHasNoInstances() {
        List<Analyzer> instances = analyzerTypeService.getInstancesForType(TYPE_NO_INSTANCES);

        Assert.assertTrue(instances.isEmpty());
    }

    @Test
    public void getByIdWithInitializedInstances_shouldLoadInstancesForType() {
        AnalyzerType type = analyzerTypeService.getByIdWithInitializedInstances(TYPE_COBAS);

        Assert.assertNotNull(type);
        Assert.assertEquals(1, type.getInstances().size());
        Assert.assertEquals(TYPE_COBAS_INSTANCE_NAME, type.getInstances().get(0).getName());
    }

    @Test
    public void getOrCreateDefaultInstance_shouldReturnExistingInstance() {
        AnalyzerType type = analyzerTypeService.getByIdWithInitializedInstances(TYPE_ABL800);
        Assert.assertNotNull(type);

        Analyzer defaultInstance = analyzerTypeService.getOrCreateDefaultInstance(type);

        Assert.assertNotNull(defaultInstance);
        Assert.assertEquals(ANALYZER_ABL800_ID, defaultInstance.getId());
        Assert.assertEquals(TYPE_ABL800_INSTANCE_NAME, defaultInstance.getName());
    }

    @Test
    public void getOrCreateDefaultInstance_shouldCreateAndPersistNewInstanceWhenNoneExists() {
        AnalyzerType typeWithoutInstances = analyzerTypeService.getByIdWithInitializedInstances(TYPE_NO_INSTANCES);
        Assert.assertTrue(typeWithoutInstances.getInstances().isEmpty());

        // Act
        Analyzer defaultInstance = analyzerTypeService.getOrCreateDefaultInstance(typeWithoutInstances);

        // Assert inline creation
        Assert.assertNotNull(defaultInstance);
        Assert.assertNotNull(defaultInstance.getId());
        Assert.assertEquals(TYPE_NO_INSTANCES_NAME, defaultInstance.getName());
        Assert.assertTrue(defaultInstance.isActive());
        Assert.assertEquals(TYPE_NO_INSTANCES, defaultInstance.getAnalyzerType().getId());

        // Assert database round-trip persistence
        Analyzer persisted = analyzerService.get(defaultInstance.getId());
        Assert.assertNotNull(persisted);
        Assert.assertEquals(defaultInstance.getName(), persisted.getName());
        Assert.assertEquals(TYPE_NO_INSTANCES, persisted.getAnalyzerType().getId());
    }
}
