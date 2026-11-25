package org.openelisglobal.spi.report;

import java.util.Objects;

/**
 * The rendered report a {@link ReportProvider} returns: the bytes, their
 * content type and a file name.
 */
public final class ReportOutput {

    private final byte[] content;
    private final String contentType;
    private final String fileName;

    public ReportOutput(byte[] content, String contentType, String fileName) {
        this.content = Objects.requireNonNull(content, "content").clone();
        this.contentType = Objects.requireNonNull(contentType, "contentType");
        this.fileName = fileName;
    }

    public static ReportOutput pdf(byte[] content, String fileName) {
        return new ReportOutput(content, "application/pdf", fileName);
    }

    public byte[] getContent() {
        return content.clone();
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * The file name offered to the browser; {@code null} lets the core choose one
     * from the report id.
     */
    public String getFileName() {
        return fileName;
    }
}
