package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.gender.service.GenderService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

public class CSVColumnBuilderDateTimeTest {

    private Object previousFactory;
    private Object previousMessages;
    private Object previousDictionaryTranslator;
    private Object previousGenderTranslator;

    @Before
    public void setUp() {
        previousFactory = ReflectionTestUtils.getField(SpringContext.class, "factory");
        previousMessages = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        previousDictionaryTranslator = ReflectionTestUtils.getField(ResourceTranslator.DictionaryTranslator.class,
                "instance");
        previousGenderTranslator = ReflectionTestUtils.getField(ResourceTranslator.GenderTranslator.class, "instance");

        AutowireCapableBeanFactory factory = mock(AutowireCapableBeanFactory.class);
        when(factory.getBean(ProjectService.class)).thenReturn(mock(ProjectService.class));
        when(factory.getBean(DictionaryService.class)).thenReturn(mock(DictionaryService.class));
        when(factory.getBean(GenderService.class)).thenReturn(mock(GenderService.class));
        DefaultConfigurationProperties properties = mock(DefaultConfigurationProperties.class);
        when(properties.getPropertyValue(Property.DEFAULT_DATE_LOCALE)).thenReturn("en-US");
        when(factory.getBean(DefaultConfigurationProperties.class)).thenReturn(properties);
        ReflectionTestUtils.setField(SpringContext.class, "factory", factory);

        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("timestamp.format.formatKey", Locale.US, "dd/MM/yyyy HH:mm:ss");
        MessageUtil.setMessageSource(messages);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessages);
        ReflectionTestUtils.setField(ResourceTranslator.DictionaryTranslator.class, "instance",
                previousDictionaryTranslator);
        ReflectionTestUtils.setField(ResourceTranslator.GenderTranslator.class, "instance", previousGenderTranslator);
    }

    @Test
    public void sampleExportPreservesTimestampHour() {
        CSVColumnBuilder builder = new CSVColumnBuilder(null) {
            @Override
            public void makeSQL() {
                throw new UnsupportedOperationException("No database query needed for timestamp conversion");
            }
        };
        for (String time : new String[] { "00:15:00", "12:34:56", "18:05:00" }) {
            // DATE_TIME columns call this conversion before escaping the CSV value.
            assertEquals("24/11/2026 " + time, builder.datetimeToLocalDateTime("2026-11-24 " + time));
        }
    }

    @Test
    public void routineExportPreservesTimestampHour() {
        CSVRoutineColumnBuilder builder = new CSVRoutineColumnBuilder(null) {
            @Override
            public void makeSQL() {
                throw new UnsupportedOperationException("No database query needed for timestamp conversion");
            }
        };
        for (String time : new String[] { "00:15:00", "12:34:56", "18:05:00" }) {
            assertEquals("24/11/2026 " + time, builder.datetimeToLocalDateTime("2026-11-24 " + time));
        }
    }
}
