package com.feedstartup.service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Wraps the PDF parsing work (page count + cover thumbnail) so the rest of the app never
 * has to know it is PDFBox under the hood.
 */
public interface PdfProcessingService {

    /**
     * Reads the PDF's page count and renders its first page to {@code thumbnailOutputPath} as a
     * PNG - the only format publication covers are stored in (see
     * PublicationLanguage#thumbnailFileName).
     */
    PdfMetadata process(Path pdfPath, Path thumbnailOutputPath) throws IOException;

    record PdfMetadata(int pageCount) {}
}
