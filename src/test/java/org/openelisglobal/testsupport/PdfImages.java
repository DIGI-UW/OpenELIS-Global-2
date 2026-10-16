package org.openelisglobal.testsupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

/** The size each image is drawn at on a page of a generated PDF. */
public final class PdfImages {

    private PdfImages() {
    }

    /** Drawn sizes in points, rounded, as {@code "WxH"}. */
    public static List<String> drawnSizes(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            Sizes sizes = new Sizes();
            sizes.processPage(document.getPage(page - 1));
            return sizes.sizes;
        }
    }

    /**
     * The top edge of each image drawn on a page, measured down from the top of the
     * page, in points.
     */
    public static List<Float> drawnTops(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            Sizes sizes = new Sizes();
            sizes.processPage(document.getPage(page - 1));
            float height = document.getPage(page - 1).getMediaBox().getHeight();
            List<Float> tops = new ArrayList<>();
            for (float top : sizes.tops) {
                tops.add(height - top);
            }
            return tops;
        }
    }

    private static final class Sizes extends PDFStreamEngine {
        private final List<String> sizes = new ArrayList<>();
        private final List<Float> tops = new ArrayList<>();

        Sizes() {
            addOperator(new Concatenate(this));
            addOperator(new SetMatrix(this));
            addOperator(new Save(this));
            addOperator(new Restore(this));
        }

        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            if ("Do".equals(operator.getName()) && !operands.isEmpty() && operands.get(0) instanceof COSName) {
                PDXObject object = getResources().getXObject((COSName) operands.get(0));
                if (object instanceof PDImageXObject) {
                    Matrix matrix = getGraphicsState().getCurrentTransformationMatrix();
                    sizes.add(Math.round(matrix.getScalingFactorX()) + "x" + Math.round(matrix.getScalingFactorY()));
                    tops.add(matrix.getTranslateY() + matrix.getScalingFactorY());
                }
            }
            super.processOperator(operator, operands);
        }
    }
}
