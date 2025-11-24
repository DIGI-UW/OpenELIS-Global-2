package org.openelisglobal.document.service;

import java.io.InputStream;

/**
 * Service for generating thumbnails from image and PDF documents.
 */
public interface ThumbnailService {
    /**
     * Generate a thumbnail from an image (JPEG/PNG) input stream.
     * @param imageStream The image input stream
     * @param contentType The content type (image/jpeg or image/png)
     * @return InputStream containing the thumbnail image (JPEG format)
     * @throws Exception if thumbnail generation fails
     */
    InputStream generateImageThumbnail(InputStream imageStream, String contentType) throws Exception;

    /**
     * Generate a thumbnail from the first page of a PDF document.
     * @param pdfStream The PDF input stream
     * @return InputStream containing the thumbnail image (JPEG format, 200x200px)
     * @throws Exception if thumbnail generation fails
     */
    InputStream generatePdfThumbnail(InputStream pdfStream) throws Exception;
}

