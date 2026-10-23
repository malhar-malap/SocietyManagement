package com.society.jaswanti.service;

import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.entity.Transaction;
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
                contentStream.showText("Jaswanti Residency CHSL - Maintenance Invoice");
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
                contentStream.showText("Charge Type: " + (bill.getChargeType() != null ? bill.getChargeType().name() : "MAINTENANCE"));
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
                contentStream.showText("Paid Amount: INR " + (bill.getPaidAmount() != null ? bill.getPaidAmount().toPlainString() : "0.00"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Due Date: " + (bill.getDueDate() != null ? bill.getDueDate().toString() : "N/A"));
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

    public byte[] generateReceipt(Transaction transaction) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Jaswanti Residency CHSL - Payment Receipt");
                contentStream.endText();

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 650);
                contentStream.showText("Receipt Number: " + (transaction.getReceiptNumber() != null ? transaction.getReceiptNumber() : "REC-" + transaction.getId()));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Transaction ID: " + transaction.getId());
                contentStream.newLineAtOffset(0, -20);
                String roomNum = "N/A";
                String residentName = "Resident";
                if (transaction.getBill() != null && transaction.getBill().getRoom() != null) {
                    roomNum = transaction.getBill().getRoom().getRoomNumber();
                    if (transaction.getBill().getRoom().getResident() != null) {
                        residentName = transaction.getBill().getRoom().getResident().getName();
                    }
                } else if (transaction.getRecordedBy() != null) {
                    residentName = transaction.getRecordedBy().getName();
                }
                contentStream.showText("Unit / Room: " + roomNum);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Received From: " + residentName);
                contentStream.newLineAtOffset(0, -20);
                BigDecimal amount = transaction.getAmount() != null ? transaction.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                contentStream.showText("Amount Paid: INR " + amount.toPlainString());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Payment Mode: " + (transaction.getPaymentMode() != null ? transaction.getPaymentMode() : "N/A"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Ref / UTR: " + (transaction.getReferenceNumber() != null ? transaction.getReferenceNumber() : "N/A"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Date: " + (transaction.getPaymentDate() != null ? transaction.getPaymentDate().toString() : (transaction.getTimestamp() != null ? transaction.getTimestamp().toLocalDate().toString() : "N/A")));
                contentStream.newLineAtOffset(0, -20);
                String bankAccName = transaction.getBankAccount() != null ? transaction.getBankAccount().getAccountName() : "General Account";
                contentStream.showText("Credited Account: " + bankAccName);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Category: " + (transaction.getCategory() != null ? transaction.getCategory() : "MAINTENANCE"));
                contentStream.newLineAtOffset(0, -30);
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 10);
                contentStream.showText("This is an official computer-generated receipt issued by Jaswanti Residency CHSL.");
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF receipt", e);
        }
    }
}
