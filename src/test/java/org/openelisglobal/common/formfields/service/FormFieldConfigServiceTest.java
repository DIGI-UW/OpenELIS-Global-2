package org.openelisglobal.common.formfields.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.formfields.AdminFormFields;
import org.openelisglobal.common.formfields.FormField;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

public class FormFieldConfigServiceTest {

    private Path tempDir;
    private FormFieldConfigService service;
    private DefaultConfigurationProperties mockConfigProps;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("formfields_test");

        ApplicationContext mockAppContext = mock(ApplicationContext.class);
        AutowireCapableBeanFactory mockFactory = mock(AutowireCapableBeanFactory.class);
        when(mockAppContext.getAutowireCapableBeanFactory()).thenReturn(mockFactory);

        new SpringContext().setApplicationContext(mockAppContext);

        mockConfigProps = mock(DefaultConfigurationProperties.class);
        when(mockFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(mockConfigProps);

        service = new FormFieldConfigService() {
            @Override
            protected String getConfigDir() {
                return tempDir.toString();
            }
        };
    }

    @After
    public void tearDown() throws IOException {
        Files.walk(tempDir).sorted((a, b) -> b.compareTo(a)).map(Path::toFile).forEach(File::delete);
    }

    @Test
    public void testLoadDefaultFields() throws IOException {
        String json = "{\"AKA\": {\"inUse\": true, \"required\": true, \"labelKey\": \"test.aka\"}}";
        try (FileWriter writer = new FileWriter(new File(tempDir.toFile(), "default.json"))) {
            writer.write(json);
        }

        Map<FormFields.Field, FormField> fields = service.getFormFields();
        assertTrue(fields.containsKey(FormFields.Field.AKA));
        assertTrue(fields.get(FormFields.Field.AKA).getInUse());
        assertTrue(fields.get(FormFields.Field.AKA).getRequired());
        assertEquals("test.aka", fields.get(FormFields.Field.AKA).getLabelKey());
    }

    @Test
    public void testLoadImplementationOverrides() throws IOException {
        String defaultJson = "{\"AKA\": {\"inUse\": true, \"required\": false}, \"StNumber\": {\"inUse\": true}}";
        try (FileWriter writer = new FileWriter(new File(tempDir.toFile(), "default.json"))) {
            writer.write(defaultJson);
        }

        String kenyaJson = "{\"AKA\": {\"inUse\": false, \"required\": true}}";
        try (FileWriter writer = new FileWriter(new File(tempDir.toFile(), "kenya.json"))) {
            writer.write(kenyaJson);
        }

        when(mockConfigProps.getPropertyValue(Property.FormFieldSet)).thenReturn("KENYA");

        Map<FormFields.Field, FormField> fields = service.getFormFields();
        assertFalse(fields.get(FormFields.Field.AKA).getInUse());
        assertTrue(fields.get(FormFields.Field.AKA).getRequired());
        assertTrue(fields.get(FormFields.Field.StNumber).getInUse());
    }

    @Test
    public void testLoadAdminFields() throws IOException {
        String json = "{\"ActionMenu\": true, \"AnalyteMenu\": false}";
        try (FileWriter writer = new FileWriter(new File(tempDir.toFile(), "admin_default.json"))) {
            writer.write(json);
        }

        Map<AdminFormFields.Field, Boolean> fields = service.getAdminFormFields();
        assertTrue(fields.get(AdminFormFields.Field.ActionMenu));
        assertFalse(fields.get(AdminFormFields.Field.AnalyteMenu));
    }
}
