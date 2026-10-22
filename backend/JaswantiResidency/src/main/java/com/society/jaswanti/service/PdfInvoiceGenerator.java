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
import java.math.BigDecimal;
import java.math.RoundingMode;

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
                BigDecimal baseAmount = bill.getAmount() != null ? bill.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                contentStream.showText("Base Amount: INR " + baseAmount.toPlainString());
                contentStream.newLineAtOffset(0, -20);
                BigDecimal lateFee = bill.getLateFee() != null ? bill.getLateFee().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                contentStream.showText("Late Fee: INR " + lateFee.toPlainString());
                contentStream.newLineAtOffset(0, -20);
                BigDecimal totalAmount = baseAmount.add(lateFee).setScale(2, RoundingMode.HALF_UP);
                contentStream.showText("Total Amount: INR " + totalAmount.toPlainString());
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
