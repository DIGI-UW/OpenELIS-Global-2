package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import java.nio.file.*;
import java.util.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.Test;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.testsupport.PdfText;

public class MicroPatientReportPdfTest {
    @Test
    public void realTemplatesCompileAndLongIsolateGridRetainsFinalRowOnLaterPages() throws Exception {
        var parent = JasperCompileManager.compileReport("src/main/resources/reports/PatientClinicalReport.jrxml");
        assertTrue(Arrays.stream(parent.getFields()).anyMatch(f -> f.getName().equals("microbiologyRows")));
        JasperCompileManager.compileReport("src/main/resources/reports/MicrobiologyBlock.jrxml");
        var deployedParent = (JasperReport) net.sf.jasperreports.engine.util.JRLoader
                .loadObject(new java.io.File("src/main/resources/reports/PatientClinicalReport.jasper"));
        assertTrue(Arrays.stream(deployedParent.getFields()).anyMatch(f -> f.getName().equals("microbiologyRows")));
        var block = (JasperReport) net.sf.jasperreports.engine.util.JRLoader
                .loadObject(new java.io.File("src/main/resources/reports/MicrobiologyBlock.jasper"));
        var rows = new ArrayList<MicroPatientReportRow>();
        rows.add(new MicroPatientReportRow("Initial testing", null, null, null, null, null, false));
        rows.add(new MicroPatientReportRow(null, "Gram stain: Gram negative bacilli", null, null, null, null, false));
        rows.add(new MicroPatientReportRow("Culture and identification", null, null, null, null, null, false));
        rows.add(new MicroPatientReportRow(null, "ISO-1: Escherichia coli", null, null, null, null, false));
        rows.add(new MicroPatientReportRow("Susceptibility testing", null, null, null, null, null, false));
        for (int i = 0; i < 70; i++)
            rows.add(new MicroPatientReportRow(null, null, "Agent " + i, "64 ug/mL", "Resistant", "CLSI 2026", true));
        rows.add(new MicroPatientReportRow(null, "ISO-2: Klebsiella pneumoniae", null, null, null, null, false));
        rows.add(new MicroPatientReportRow(null, null, "Final agent", "4 ug/mL", "Susceptible", "EUCAST 2026", true));
        rows.add(new MicroPatientReportRow("Additional testing", null, null, null, null, null, false));
        rows.add(new MicroPatientReportRow(null, "External note for the requesting clinician.", null, null, null, null,
                false));
        var print = JasperFillManager.fillReport(block, new HashMap<>(), new JRBeanCollectionDataSource(rows));
        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        String text = PdfText.of(pdf);
        assertTrue(text, text.indexOf("Initial testing") < text.indexOf("Culture and identification"));
        assertTrue(text, text.indexOf("Culture and identification") < text.indexOf("Susceptibility testing"));
        assertTrue(text, text.indexOf("Susceptibility testing") < text.indexOf("Additional testing"));
        assertTrue(text, text.contains("Final agent 4 ug/mL Susceptible EUCAST 2026"));
        assertTrue(text, text.contains("Agent 69 64 ug/mL Resistant CLSI 2026"));
        assertTrue(text, text.contains("External note for the requesting clinician."));
        assertTrue(PdfText.pageCount(pdf) > 1);
        Files.createDirectories(Path.of("target/amr-m6-evidence"));
        Files.write(Path.of("target/amr-m6-evidence/microbiology-report.pdf"), pdf);
    }
}
