package com.feedstartup.service.impl;

import com.feedstartup.service.PdfProcessingService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfProcessingServiceImpl implements PdfProcessingService {

    private static final int THUMBNAIL_DPI = 120;

    @Override
    public PdfMetadata process(Path pdfPath, Path thumbnailOutputPath) throws IOException {
        if (!thumbnailOutputPath.getFileName().toString().toLowerCase().endsWith(".png")) {
            throw new IllegalArgumentException("Publication thumbnails are always PNG, not " + thumbnailOutputPath.getFileName());
        }
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            int pageCount = document.getNumberOfPages();

            if (pageCount > 0) {
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage coverImage = renderer.renderImageWithDPI(0, THUMBNAIL_DPI, ImageType.RGB);
                Files.createDirectories(thumbnailOutputPath.getParent());
                if (!ImageIO.write(coverImage, "png", thumbnailOutputPath.toFile())) {
                    throw new IOException("No PNG writer available to save the cover thumbnail");
                }
            }

            return new PdfMetadata(pageCount);
        }
    }
}
