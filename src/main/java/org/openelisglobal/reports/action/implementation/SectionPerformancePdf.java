package org.openelisglobal.reports.action.implementation;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.SectionPerformanceData;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

/**
 * The section performance report: the waiting days of each category as a
 * horizontal bar chart.
 */
final class SectionPerformancePdf {

    private SectionPerformancePdf() {
    }

    static byte[] render(List<SectionPerformanceData> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph(DateUtil.getCurrentDateAsText() + "  " + DateUtil.getCurrentTimeAsText(),
                new Font(Font.HELVETICA, 9)));
        Paragraph title = new Paragraph("Durée d'attente des demandes en jours",
                new Font(Font.HELVETICA, 12, Font.BOLD));
        title.setSpacingAfter(10);
        document.add(title);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (SectionPerformanceData item : items) {
            dataset.addValue(item.getCategoryValue(), "days", item.getCategoryLabel());
        }
        JFreeChart chart = ChartFactory.createBarChart(null, null, MessageUtil.getMessage("report.label.days"), dataset,
                PlotOrientation.HORIZONTAL, false, false, false);
        float width = document.right() - document.left();
        float height = 360;
        PdfTemplate template = writer.getDirectContent().createTemplate(width, height);
        Graphics2D graphics = template.createGraphics(width, height);
        chart.draw(graphics, new Rectangle2D.Double(0, 0, width, height));
        graphics.dispose();
        document.add(Image.getInstance(template));
        document.close();
        return out.toByteArray();
    }
}
