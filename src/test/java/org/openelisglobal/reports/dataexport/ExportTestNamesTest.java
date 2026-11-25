package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.reports.dataexport.service.ExportTestNames;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * OGC-1680: a test with no translation in any language the export can use keeps
 * its catalog description as it is, with no sample type added, as the screens
 * show it.
 */
public class ExportTestNamesTest {

    @org.junit.Test
    public void aTestWithNoTranslationKeepsItsDescription() {
        Test test = new Test();
        test.setDescription("Glucose(Serum)");
        TypeOfSample serum = new TypeOfSample();
        serum.setDescription("Serum");

        assertEquals("Glucose(Serum)", ExportTestNames.name(test, Locale.ENGLISH));
        assertEquals("Glucose(Serum)", new ExportTestNames().withSpecimen(test, serum, Locale.FRENCH));
    }

    @org.junit.Test
    public void aTranslationInAnotherLanguageOnlyIsNotUsed() {
        Test test = new Test();
        test.setDescription("Glucose(Serum)");
        Localization name = new Localization();
        name.setLocalizedValue("fr", "Glycémie");
        test.setLocalizedTestName(name);

        assertEquals("Glycémie", ExportTestNames.name(test, Locale.FRENCH));
        assertEquals("Glucose(Serum)", ExportTestNames.name(test, Locale.ENGLISH));
    }

    /**
     * A result component's label follows the job's language when it has a
     * translation (OGC-1610), else keeps the label as entered.
     */
    @org.junit.Test
    public void aComponentLabelFollowsTheJobsLanguage() {
        org.openelisglobal.testresultcomponent.valueholder.TestResultComponent component = new org.openelisglobal.testresultcomponent.valueholder.TestResultComponent();
        component.setLabel("HIV-1 viral load");
        Localization label = new Localization();
        label.setLocalizedValue("fr", "Charge virale VIH-1");
        component.setLabelLocalization(label);

        assertEquals("Charge virale VIH-1", ExportTestNames.componentLabel(component, Locale.FRENCH));
        assertEquals("HIV-1 viral load", ExportTestNames.componentLabel(component, Locale.ENGLISH));
        component.setLabelLocalization(null);
        assertEquals("HIV-1 viral load", ExportTestNames.componentLabel(component, Locale.FRENCH));
    }
}
