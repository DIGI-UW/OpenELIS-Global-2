package org.openelisglobal.barcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.Barcode;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.openelisglobal.barcode.labeltype.BlankLabel;
import org.openelisglobal.barcode.labeltype.BlockLabel;
import org.openelisglobal.barcode.labeltype.Label;
import org.openelisglobal.barcode.labeltype.OrderLabel;
import org.openelisglobal.barcode.labeltype.SlideLabel;
import org.openelisglobal.barcode.labeltype.SpecimenLabel;
import org.openelisglobal.barcode.service.BarcodeLabelInfoService;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AltYearAccessionValidator;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.valueholder.Test;

/**
 * Class for taking lists of Label objects and turning them into a printable
 * format
 *
 * @author Caleb
 */
public class BarcodeLabelMaker {

    public enum BarcodeType {
        BARCODE, QR;

        public static BarcodeType fromString(String value) {
            for (BarcodeType type : BarcodeType.values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return BARCODE;
        }
    }

    private BarcodeType barcodeType = BarcodeType.BARCODE;

    public BarcodeType getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(BarcodeType barcodeType) {
        this.barcodeType = barcodeType;
    }

    // number of columns for label layout grid
    private static int NUM_COLUMNS = 20;

    // stores labels between generation and creating pdf
    private ArrayList<Label> labels;

    // whether max print numbers should be ignored
    private String override;

    // for audit trail when incrementing num printed
    private String sysUserId;

    private BarcodeLabelInfoService barcodeLabelService;

    /**
     * Lazy initialization of barcodeLabelService. Initializes on first use to
     * ensure SpringContext is ready.
     * 
     * @return BarcodeLabelInfoService instance
     */
    private BarcodeLabelInfoService getBarcodeLabelService() {
        if (barcodeLabelService == null) {
            try {
                barcodeLabelService = SpringContext.getBean(BarcodeLabelInfoService.class);
            } catch (Exception e) {
                LogEvent.logError("BarcodeLabelMaker", "getBarcodeLabelService",
                        "Failed to get BarcodeLabelInfoService from SpringContext: " + e.getMessage());
                throw new RuntimeException("BarcodeLabelInfoService not available", e);
            }
        }
        return barcodeLabelService;
    }

    private static final Set<Integer> ENTERED_STATUS_SAMPLE_LIST = new HashSet<>();
    private static volatile boolean initialized = false;

    /**
     * Lazy initialization of ENTERED_STATUS_SAMPLE_LIST. Initializes on first use
     * to ensure SpringContext is ready. Thread-safe using double-checked locking
     * pattern.
     * 
     * @return Set containing the status ID for SampleStatus.Entered, or empty set
     *         if initialization fails
     */
    private static Set<Integer> getEnteredStatusSampleList() {
        if (!initialized) {
            synchronized (ENTERED_STATUS_SAMPLE_LIST) {
                if (!initialized) {
                    try {
                        IStatusService statusService = SpringContext.getBean(IStatusService.class);
                        if (statusService != null) {
                            String statusId = statusService.getStatusID(SampleStatus.Entered);

                            if (statusId != null && !statusId.equals("-1") && !statusId.trim().isEmpty()) {
                                ENTERED_STATUS_SAMPLE_LIST.add(Integer.parseInt(statusId));
                            } else {
                                LogEvent.logError("BarcodeLabelMaker", "getEnteredStatusSampleList",
                                        "SampleStatus.Entered not found in database. Status ID: " + statusId);
                            }
                        } else {
                            LogEvent.logError("BarcodeLabelMaker", "getEnteredStatusSampleList",
                                    "IStatusService bean not available from SpringContext");
                        }
                    } catch (NumberFormatException e) {
                        LogEvent.logError("BarcodeLabelMaker", "getEnteredStatusSampleList",
                                "Failed to parse status ID: " + e.getMessage());
                    } catch (Exception e) {
                        LogEvent.logError("BarcodeLabelMaker", "getEnteredStatusSampleList",
                                "Failed to initialize ENTERED_STATUS_SAMPLE_LIST: " + e.getMessage());
                    } finally {
                        // Mark as initialized to prevent repeated attempts, even if initialization
                        // failed
                        // This prevents performance issues from repeated synchronization attempts
                        initialized = true;
                    }
                }
            }
        }
        return ENTERED_STATUS_SAMPLE_LIST;
    }

    public BarcodeLabelMaker() {
        labels = new ArrayList<>();
        this.barcodeType = BarcodeType
                .fromString(ConfigurationProperties.getInstance().getPropertyValue(Property.BAR_CODE_TYPE));
    }

    public BarcodeLabelMaker(Label label) {
        labels = new ArrayList<>();
        labels.add(label);
        // Initialize barcodeType from configuration (same as no-arg constructor)
        this.barcodeType = BarcodeType
                .fromString(ConfigurationProperties.getInstance().getPropertyValue(Property.BAR_CODE_TYPE));
    }

    public BarcodeLabelMaker(ArrayList<Label> labels) {
        this.labels = labels;
        // Initialize barcodeType from configuration (same as no-arg constructor)
        this.barcodeType = BarcodeType
                .fromString(ConfigurationProperties.getInstance().getPropertyValue(Property.BAR_CODE_TYPE));
    }

    public void generateGenericBarcodeLabel(String code, String type) {
        if ("block".equals(type)) {
            BlockLabel label = new BlockLabel(code);
            labels.add(label);
        } else if ("slide".equals(type)) {
            SlideLabel label = new SlideLabel(code);
            labels.add(label);
        }
    }

    public void generatePrePrintLabels(Integer numSetsOfLabels, Integer numOrderLabelsPerSet,
            Integer numSpecimenLabelsPerSet, String facilityName, List<Test> tests, String startingAt)
            throws LIMSInvalidConfigurationException {
        IAccessionNumberGenerator accessionValidator = null;
        if (Boolean
                .valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.USE_ALT_ACCESSION_PREFIX))) {
            accessionValidator = AccessionNumberUtil.getAltAccessionNumberGenerator();
            ((AltYearAccessionValidator) accessionValidator).setOverrideStartingAt(startingAt);
        }
        for (int i = 0; i < numSetsOfLabels; ++i) {
            String accessionNumber = genNextPrePrintedAccessionNumber(accessionValidator, startingAt);
            OrderLabel orderLabel = new OrderLabel(accessionNumber, facilityName);
            orderLabel.setNumLabels(numOrderLabelsPerSet);
            // orderLabel.linkBarcodeLabelInfo();
            // get sysUserId from login module
            // orderLabel.setSysUserId(sysUserId);
            // if (orderLabel.checkIfPrintable() || "true".equals(override)) {
            labels.add(orderLabel);

            SpecimenLabel specimenLabel = new SpecimenLabel(accessionNumber, facilityName, tests);
            specimenLabel.setNumLabels(numSpecimenLabelsPerSet);
            // specimenLabel.linkBarcodeLabelInfo();
            // get sysUserId from login module
            // specimenLabel.setSysUserId(sysUserId);
            // if (specimenLabel.checkIfPrintable() || "true".equals(override)) {
            labels.add(specimenLabel);
            // }
        }
    }

    private String genNextPrePrintedAccessionNumber(IAccessionNumberGenerator accessionValidator, String startingAt)
            throws LIMSInvalidConfigurationException {
        if (accessionValidator == null) {
            accessionValidator = AccessionNumberUtil.getMainAccessionNumberGenerator();
        }
        // if (GenericValidator.isBlankOrNull(startingAt)) {
        return accessionValidator.getNextAvailableAccessionNumber("", true);
        // } else {
        // return accessionValidator.getNextAccessionNumber("", true);
        // }
    }

    /**
     * Create all labels that were requested and place them into the provided list
     * of labels
     *
     * @param labels    A list that the generated labels should be placed into
     * @param labNo     The labNo that should appear on the bar codes
     * @param patientId The id belonging to the patient that should appear on the
     *                  label
     * @param type      The type of bar code that should be created
     * @param quantity  The number of labels that should be printed
     * @param request   This is used to attribute this action to a user
     */
    public void generateLabels(String labNo, String type, String quantity, String override) {

        /*
         * LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "labNo: "
         * + labNo + "\n" + "patientId: " + patientId + "\n" + "type: " + type + "\n" +
         * "quantity: " + quantity + "\n" + "override: " + override);
         */

        SampleService sampleService = SpringContext.getBean(SampleService.class);
        SampleItemService sampleItemService = SpringContext.getBean(SampleItemService.class);
        if ("default".equals(type)) {
            // add 2 order label per default
            Sample sample = sampleService.getSampleByAccessionNumber(labNo);
            OrderLabel orderLabel = new OrderLabel(sampleService.getPatient(sample), sample, labNo);
            orderLabel.setNumLabels(Integer
                    .parseInt(ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_ORDER_PRINTED)));
            orderLabel.linkBarcodeLabelInfo();
            // get sysUserId from login module
            orderLabel.setSysUserId(sysUserId);
            if (orderLabel.checkIfPrintable() || "true".equals(override)) {
                labels.add(orderLabel);
            }

            // 1 specimen label per sampleitem
            List<SampleItem> sampleItemList = sampleItemService.getSampleItemsBySampleIdAndStatus(sample.getId(),
                    getEnteredStatusSampleList());
            for (SampleItem sampleItem : sampleItemList) {
                SpecimenLabel specLabel = new SpecimenLabel(sampleService.getPatient(sample), sample, sampleItem,
                        labNo);
                specLabel.setNumLabels(Integer.parseInt(
                        ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_SPECIMEN_PRINTED)));
                specLabel.linkBarcodeLabelInfo();
                // get sysUserId from login module
                specLabel.setSysUserId(sysUserId);
                if (specLabel.checkIfPrintable() || "true".equals(override)) {
                    labels.add(specLabel);
                }
            }
            // order case
        } else if ("order".equals(type)) {
            Sample sample = sampleService.getSampleByAccessionNumber(labNo);
            OrderLabel orderLabel = new OrderLabel(sampleService.getPatient(sample), sample, labNo);
            orderLabel.setNumLabels(Integer.parseInt(quantity));
            orderLabel.linkBarcodeLabelInfo();
            // get sysUserId from login module
            orderLabel.setSysUserId(sysUserId);
            if (orderLabel.checkIfPrintable() || "true".equals(override)) {
                labels.add(orderLabel);
            }

            // specimen case
        } else if ("specimen".equals(type)) {
            String specimenNumber = labNo.substring(labNo.lastIndexOf(".") + 1);
            labNo = labNo.substring(0, labNo.lastIndexOf("."));
            Sample sample = sampleService.getSampleByAccessionNumber(labNo);
            List<SampleItem> sampleItemList = sampleItemService.getSampleItemsBySampleIdAndStatus(sample.getId(),
                    getEnteredStatusSampleList());
            for (SampleItem sampleItem : sampleItemList) {
                // get only the sample item matching the specimen number
                if (sampleItem.getSortOrder().equals(specimenNumber)) {
                    SpecimenLabel specLabel = new SpecimenLabel(sampleService.getPatient(sample), sample, sampleItem,
                            labNo);
                    specLabel.setNumLabels(Integer.parseInt(quantity));
                    specLabel.linkBarcodeLabelInfo();
                    // get sysUserId from login module
                    specLabel.setSysUserId(sysUserId);
                    if (specLabel.checkIfPrintable() || "true".equals(override)) {
                        labels.add(specLabel);
                    }
                }
            }

            // blank case
        } else if ("blank".equals(type)) {
            BlankLabel blankLabel = new BlankLabel(labNo);
            blankLabel.linkBarcodeLabelInfo();
            // get sysUserId from login module
            blankLabel.setSysUserId(sysUserId);
            if (blankLabel.checkIfPrintable() || "true".equals(override)) {
                labels.add(blankLabel);
            }
        }
    }

    /**
     * Creates a PDF as a stream of all the stored labels
     *
     * @return Stream of all labels that have been generated
     */
    public ByteArrayOutputStream createLabelsAsStream() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (labels.isEmpty()) {
            LogEvent.logError("BarcodeLabelMaker", "createLabelsAsStream", "Labels list is empty!");
            return stream;
        }
        try {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, stream);
            document.open();

            for (Label label : labels) {
                for (int i = 0; i < label.getNumLabels(); ++i) {
                    // a ratio is used with set width so that font size
                    // does not need to be adjusted
                    float ratio = label.getHeight() / label.getWidth();
                    label.pdfWidth = 350;
                    label.pdfHeight = label.pdfWidth * ratio;
                    drawLabel(label, writer, document);
                    // label.incrementNumPrinted();
                }
            }
            document.close();
            writer.close();
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return stream;
    }

    /**
     * Creates a PDF as a stream of all the stored labels
     *
     * @return Stream of all labels that have been generated
     */
    public ByteArrayOutputStream createLabelsAsStreamWithMaximumPrints() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (labels.isEmpty()) {
            return stream;
        }
        try {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, stream);
            document.open();
            for (Label label : labels) {
                for (int i = 0; i < label.getNumLabels(); ++i) {
                    if (label.checkIfPrintable() || "true".equals(override)) {
                        // a ratio is used with set width so that font size
                        // does not need to be adjusted
                        float ratio = label.getHeight() / label.getWidth();
                        label.pdfWidth = 350;
                        label.pdfHeight = label.pdfWidth * ratio;
                        drawLabel(label, writer, document);
                        label.incrementNumPrinted();
                    }
                }
                getBarcodeLabelService().save(label.getLabelInfo());
            }
            document.close();
            writer.close();
        } catch (DocumentException | IOException e) {
            LogEvent.logDebug(e);
        }

        return stream;
    }

    // parse label info to draw label and add to document
    /**
     * Draws a label as a PDF page and adds the page to the document
     *
     * @param label    The label to draw as a pdf "page"
     * @param writer   The writer for the pdf stream
     * @param document The document to add the label "page" to
     * @throws DocumentException
     * @throws IOException
     */
    private void drawLabel(Label label, PdfWriter writer, Document document) throws DocumentException, IOException {
        // set up document and grid
        Rectangle rec = new Rectangle(label.pdfWidth, label.pdfHeight);
        document.setPageSize(rec);
        document.newPage();

        if (barcodeType == BarcodeType.BARCODE) {
            PdfPTable table = new PdfPTable(NUM_COLUMNS);
            table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            table.setTotalWidth(label.pdfWidth - (2 * label.getMargin()));
            table.setLockedWidth(true);

            // add above fields into table
            Iterable<LabelField> fields = label.getAboveFields();
            if (fields != null) {
                for (LabelField field : fields) {
                    if (field.isStartNewline()) {
                        table.completeRow();
                    }
                    table.addCell(createFieldAsPDFField(label, field));
                }
                table.completeRow();
            }

            // add bar code
            if (label.getScaledBarcodeSpace() != NUM_COLUMNS) {
                table.addCell(createSpacerCell((NUM_COLUMNS - label.getScaledBarcodeSpace()) / 2));
                table.addCell(create128Barcode(label, writer, label.getScaledBarcodeSpace()));
                table.addCell(createSpacerCell((NUM_COLUMNS - label.getScaledBarcodeSpace()) / 2));
            } else {
                table.addCell(create128Barcode(label, writer, label.getScaledBarcodeSpace()));
            }

            // add below fields into table
            Iterable<LabelField> belowFields = label.getBelowFields();
            if (belowFields != null) {
                for (LabelField field : belowFields) {
                    if (field.isStartNewline()) {
                        table.completeRow();
                    }
                    table.addCell(createFieldAsPDFField(label, field));
                }
                table.completeRow();
            }

            // convert table to image, scale image, and center it on document
            document.add(scaleCentreTableAsImage(label, writer, table));
        } else {
            // QR code layout with QR on left, fields on right
            PdfPTable mainTable = new PdfPTable(2); // 2 columns for QR and fields
            mainTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            mainTable.setTotalWidth(label.pdfWidth - (2 * label.getMargin()));
            mainTable.setLockedWidth(true);
            float[] columnWidths = { 0.4f, 0.6f }; // 40% for QR, 60% for fields
            mainTable.setWidths(columnWidths);

            // Left column - QR Code
            PdfPCell qrCell = createQRCode(label, writer, 1);
            qrCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            qrCell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
            mainTable.addCell(qrCell);

            // Right column - Fields Table
            PdfPTable fieldsTable = new PdfPTable(1);
            fieldsTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            fieldsTable.setWidthPercentage(100);

            // Add code text in larger font and bold
            com.itextpdf.text.Font boldFont = new com.itextpdf.text.Font(label.getValueFont());
            boldFont.setSize(15); // Larger font size
            boldFont.setStyle(com.lowagie.text.Font.BOLD);

            String codeForText = label.getCode();
            if (codeForText == null) {
                codeForText = "";
            }
            Paragraph codeText = new Paragraph(codeForText, boldFont);
            codeText.setAlignment(Paragraph.ALIGN_CENTER);
            PdfPCell codeCell = new PdfPCell(codeText);
            codeCell.setBorder(Rectangle.NO_BORDER);
            codeCell.setPadding(1);
            fieldsTable.addCell(codeCell);

            // Add above fields
            Iterable<LabelField> fields = label.getAboveFields();
            if (fields != null) {
                for (LabelField field : fields) {
                    fieldsTable.addCell(createFieldAsPDFField(label, field));
                }
            }

            // Add below fields
            Iterable<LabelField> belowFields = label.getBelowFields();
            if (belowFields != null) {
                for (LabelField field : belowFields) {
                    fieldsTable.addCell(createFieldAsPDFField(label, field));
                }
            }

            PdfPCell fieldsCell = new PdfPCell(fieldsTable);
            fieldsCell.setBorder(Rectangle.NO_BORDER);
            fieldsCell.setPadding(5);
            mainTable.addCell(fieldsCell);

            document.add(scaleCentreTableAsImage(label, writer, mainTable));
        }
    }

    /**
     * Converts table to a scaled, centered image
     *
     * @param label  The label to specify the dimensions
     * @param writer For the stream
     * @param table  The table to convert and scale
     * @return An image representation of the table
     * @throws BadElementException
     */
    private Image scaleCentreTableAsImage(Label label, PdfWriter writer, PdfPTable table) throws BadElementException {
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate template = cb.createTemplate(table.getTotalWidth(), table.getTotalHeight());
        table.writeSelectedRows(0, -1, 0, table.getTotalHeight(), template);
        Image labelAsImage = Image.getInstance(template);
        labelAsImage.scaleAbsoluteHeight(label.pdfHeight - (2 * label.getMargin()));
        labelAsImage.setAbsolutePosition(((label.pdfWidth) - labelAsImage.getScaledWidth()) / 2,
                ((label.pdfHeight) - labelAsImage.getScaledHeight()) / 2);
        return labelAsImage;
    }

    /**
     * Create code 128 bar code with bar code as text below bar code
     *
     * @param label   The label containing the bar code
     * @param writer  For the stream
     * @param colspan The number of columns the bar code will span
     * @return Cell containing bar code with bar code text
     * @throws DocumentException
     * @throws IOException
     */
    private PdfPCell create128Barcode(Label label, PdfWriter writer, int colspan)
            throws DocumentException, IOException {

        Barcode128 barcode = new Barcode128();
        barcode.setCodeType(Barcode.CODE128);
        String code = label.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = ""; // Use empty string if code is null
        }
        barcode.setCode(code);
        String codeLabel = label.getCodeLabel();
        if (codeLabel == null) {
            codeLabel = code;
        }
        barcode.setAltText(codeLabel);
        // shrink bar code height inversely with number of text rows
        barcode.setBarHeight((10 - (label.getNumTextRowsBefore() + label.getNumTextRowsAfter())) * 30 / 10);
        PdfPCell cell = new PdfPCell(barcode.createImageWithBarcode(writer.getDirectContent(), null, null), true);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setColspan(colspan);
        cell.setPadding(1);
        return cell;
    }

    /**
     * Create code 128 bar code without bar code as text below bar code Recommended
     * for large fonts
     *
     * @param label   The label containing the bar code
     * @param writer  For the stream
     * @param colspan The number of columns the bar code will span
     * @return Cell containing bar code
     * @throws DocumentException
     * @throws IOException
     */
    @SuppressWarnings("unused")
    private PdfPCell create128BarcodeNoText(Label label, PdfWriter writer, int colspan)
            throws DocumentException, IOException {
        Barcode128 barcode = new Barcode128();
        barcode.setCodeType(Barcode.CODE128);
        String code = label.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = ""; // Use empty string if code is null
        }
        barcode.setCode(code);
        barcode.setFont(null);
        // shrink bar code height inversely with number of text rows
        barcode.setBarHeight((10 - (label.getNumTextRowsBefore() + label.getNumTextRowsAfter())) * 30 / 10);
        PdfPCell cell = new PdfPCell(barcode.createImageWithBarcode(writer.getDirectContent(), null, null), true);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setColspan(colspan);
        cell.setPadding(1);
        return cell;
    }

    /**
     * Create a cell from a Field object
     *
     * @param label Contains font to use
     * @param field To base values, and formats on
     * @return Cell containing field
     */
    private PdfPCell createFieldAsPDFField(Label label, LabelField field) {
        Paragraph fieldPDF = new Paragraph();
        // add field name if applicable
        if (field.isDisplayFieldName()) {
            Chunk name = new Chunk(field.getName() + ": ");
            name.setFont(label.getValueFont());
            fieldPDF.add(name);
        }
        // add value
        Chunk value = new Chunk(field.getValue());
        value.setFont(label.getValueFont());
        // add underline to value if applicable
        if (field.isUnderline()) {
            Chunk underline = new Chunk(new LineSeparator(0.5f, 100, null, 0, -1));
            value.setUnderline(0.5f, -1);
            fieldPDF.add(value);
            fieldPDF.add(underline);
        } else {
            fieldPDF.add(value);
        }
        // construct and configure cell
        PdfPCell cell = new PdfPCell(fieldPDF);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setColspan(field.getColspan());
        cell.setPadding(1);

        return cell;
    }

    /**
     * Creates a blank cell to fill space
     *
     * @param colspan The number of columns cell should take up
     * @return Cell containing nothing
     */
    private PdfPCell createSpacerCell(int colspan) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setColspan(colspan);
        return cell;
    }

    /**
     * Get patient by id
     *
     * @param personKey The PK for the patient
     * @return The corresponding patient
     */
    private Patient getPatientForID(String personKey) {
        Patient patient = new Patient();
        patient.setId(personKey);
        PatientService patientService = SpringContext.getBean(PatientService.class);
        patientService.getData(patient);
        if (patient.getId() == null) {
            return null;
        } else {
            return patient;
        }
    }

    private PdfPCell createQRCode(Label label, PdfWriter writer, int colspan) throws DocumentException, IOException {
        try {
            // Increased base size for QR code
            int qrSize = 1000; // Further increased for higher quality

            String code = label.getCode();
            if (code == null || code.trim().isEmpty()) {
                code = ""; // Use empty string if code is null
            }

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(code, BarcodeFormat.QR_CODE, qrSize, qrSize);

            BufferedImage qrImage = new BufferedImage(qrSize, qrSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = qrImage.createGraphics();
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, qrSize, qrSize);
            graphics.setColor(java.awt.Color.BLACK);

            for (int x = 0; x < qrSize; x++) {
                for (int y = 0; y < qrSize; y++) {
                    if (bitMatrix.get(x, y)) {
                        graphics.fillRect(x, y, 1, 1);
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "png", baos);
            Image qrCodeImage = Image.getInstance(baos.toByteArray());

            // Adjust cell to take up more space with minimal margins
            float availableWidth = (label.pdfWidth * 0.4f) - 4; // Reduced margin from 20 to 4
            float availableHeight = label.pdfHeight - 4; // Reduced margin from 20 to 4

            // Scale QR code to fill more space
            qrCodeImage.scaleToFit(availableWidth, availableHeight);

            PdfPCell cell = new PdfPCell(qrCodeImage, true);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setColspan(colspan);
            cell.setPadding(1); // Reduced padding from 5 to 2
            cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);

            if (label.getCodeLabel() != null && !label.getCodeLabel().isEmpty()) {
                Paragraph textPara = new Paragraph(label.getCodeLabel(), label.getValueFont());
                cell.addElement(textPara);
            }

            return cell;

        } catch (Exception e) {
            LogEvent.logError(e);
            throw new DocumentException("Failed to create QR code: " + e.getMessage());
        }
    }

    public String getOverride() {
        return override;
    }

    public void setOverride(String override) {
        this.override = override;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public void generateBlockLabel(Integer blockNumber) {
        // TODO Auto-generated method stub

    }
}