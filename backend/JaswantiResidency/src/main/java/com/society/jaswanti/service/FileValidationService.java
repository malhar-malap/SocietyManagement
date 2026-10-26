package com.society.jaswanti.service;

import com.society.jaswanti.exception.InvalidFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileValidationService {

    private final long maxFileSizeBytes;

    public FileValidationService(@Value("${app.upload.max-file-size-bytes:5242880}") long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /**
     * Validates image files (JPEG, PNG, WEBP).
     * @return Detected MIME type.
     */
    public String validateImage(MultipartFile file) {
        return validateFile(file, false);
    }

    /**
     * Validates image files or PDF documents (JPEG, PNG, WEBP, PDF).
     * @return Detected MIME type.
     */
    public String validateImageOrPdf(MultipartFile file) {
        return validateFile(file, true);
    }

    public String validateFile(MultipartFile file, boolean allowPdf) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new InvalidFileException("Uploaded file cannot be empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new InvalidFileException(String.format("File size (%d bytes) exceeds maximum allowed limit of %d MB",
                    file.getSize(), maxFileSizeBytes / (1024 * 1024)));
        }

        byte[] headerBytes;
        try {
            byte[] allBytes = file.getBytes();
            if (allBytes.length == 0) {
                throw new InvalidFileException("Uploaded file cannot be empty");
            }
            headerBytes = allBytes;
        } catch (IOException e) {
            throw new InvalidFileException("Could not read uploaded file content: " + e.getMessage());
        }

        String detectedMimeType = detectMimeType(headerBytes);
        if (detectedMimeType == null) {
            throw new InvalidFileException("Invalid or unrecognized file format. Only JPEG, PNG, WEBP"
                    + (allowPdf ? ", and PDF" : "") + " files are allowed.");
        }

        if ("application/pdf".equals(detectedMimeType) && !allowPdf) {
            throw new InvalidFileException("PDF files are not permitted for this upload. Only JPEG, PNG, and WEBP images are allowed.");
        }

        return detectedMimeType;
    }

    /**
     * Inspects magic bytes (file signature) to detect actual content type.
     */
    public String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 3) {
            return null;
        }

        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.length >= 8 &&
                (bytes[0] & 0xFF) == 0x89 &&
                bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G' &&
                bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "image/png";
        }

        // WEBP: "RIFF" .... "WEBP"
        if (bytes.length >= 12 &&
                bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }

        // PDF: "%PDF-" (25 50 44 46 2D)
        if (bytes.length >= 5 &&
                bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-') {
            return "application/pdf";
        }

        return null;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }
}
