package com.society.jaswanti.service;

import com.society.jaswanti.entity.MaintenanceBill;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfInvoiceGenerator {

    public byte[] generateInvoice(MaintenanceBill bill) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Jaswanti Residency - Maintenance Invoice");
                contentStream.endText();

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 650);
                contentStream.showText("Bill ID: " + bill.getId());
                contentStream.newLineAtOffset(0, -20);
                String roomNum = bill.getRoom() != null ? bill.getRoom().getRoomNumber() : "N/A";
                contentStream.showText("Room: " + roomNum);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Billing Month: " + bill.getBillingMonth());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Base Amount: INR " + bill.getAmount());
                contentStream.newLineAtOffset(0, -20);
                double lateFee = bill.getLateFee() != null ? bill.getLateFee() : 0.0;
                contentStream.showText("Late Fee: INR " + lateFee);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Total Amount: INR " + (bill.getAmount() + lateFee));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Status: " + bill.getStatus().name());
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF invoice", e);
        }
    }
}
