package com.society.jaswanti.service;

import com.society.jaswanti.dto.ImportBillsResponse;
import com.society.jaswanti.entity.BillStatus;
import com.society.jaswanti.entity.ChargeType;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.util.BillingPeriodHelper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExcelBillingService {

    private final MaintenanceBillRepository billRepository;
    private final RoomRepository roomRepository;
    private final MaintenanceBillService maintenanceBillService;

    public ExcelBillingService(MaintenanceBillRepository billRepository,
                               RoomRepository roomRepository,
                               MaintenanceBillService maintenanceBillService) {
        this.billRepository = billRepository;
        this.roomRepository = roomRepository;
        this.maintenanceBillService = maintenanceBillService;
    }

    public byte[] exportBillsToExcel(List<MaintenanceBill> bills) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Maintenance Bills");

            // Header Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Data Styles
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat df = workbook.createDataFormat();
            currencyStyle.setDataFormat(df.getFormat("#,##0.00"));

            String[] headers = {
                    "Bill ID", "Room Number", "Wing", "Category", "Billing Month",
                    "Charge Type", "Base Amount", "Late Fee", "Paid Amount", "Balance Due",
                    "Due Date", "Status"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;
            for (MaintenanceBill bill : bills) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(bill.getId() != null ? bill.getId() : 0);
                row.createCell(1).setCellValue(bill.getRoom() != null ? bill.getRoom().getRoomNumber() : "N/A");
                row.createCell(2).setCellValue(bill.getRoom() != null ? bill.getRoom().getWing() : "N/A");
                row.createCell(3).setCellValue(bill.getRoom() != null && bill.getRoom().getUnitCategory() != null ? bill.getRoom().getUnitCategory() : "RESIDENTIAL");
                row.createCell(4).setCellValue(bill.getBillingMonth() != null ? bill.getBillingMonth() : "");
                row.createCell(5).setCellValue(bill.getChargeType() != null ? bill.getChargeType().name() : "MAINTENANCE");

                Cell c6 = row.createCell(6);
                c6.setCellValue(bill.getAmount() != null ? bill.getAmount().doubleValue() : 0.0);
                c6.setCellStyle(currencyStyle);

                Cell c7 = row.createCell(7);
                c7.setCellValue(bill.getLateFee() != null ? bill.getLateFee().doubleValue() : 0.0);
                c7.setCellStyle(currencyStyle);

                Cell c8 = row.createCell(8);
                c8.setCellValue(bill.getPaidAmount() != null ? bill.getPaidAmount().doubleValue() : 0.0);
                c8.setCellStyle(currencyStyle);

                BigDecimal base = bill.getAmount() != null ? bill.getAmount() : BigDecimal.ZERO;
                BigDecimal late = bill.getLateFee() != null ? bill.getLateFee() : BigDecimal.ZERO;
                BigDecimal paid = bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal due = base.add(late).subtract(paid).max(BigDecimal.ZERO);

                Cell c9 = row.createCell(9);
                c9.setCellValue(due.doubleValue());
                c9.setCellStyle(currencyStyle);

                row.createCell(10).setCellValue(bill.getDueDate() != null ? bill.getDueDate().toString() : "");
                row.createCell(11).setCellValue(bill.getStatus() != null ? bill.getStatus().name() : "PENDING");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export bills to Excel", e);
        }
    }

    public byte[] generateImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Bill Import Template");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Room Number", "Billing Month (YYYY-MM)", "Charge Type", "Amount", "Due Date (YYYY-MM-DD)"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Sample instructional row
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("101");
            sampleRow.createCell(1).setCellValue("2026-07");
            sampleRow.createCell(2).setCellValue("MAINTENANCE");
            sampleRow.createCell(3).setCellValue(2000.00);
            sampleRow.createCell(4).setCellValue("2026-07-15");

            Row sampleRow2 = sheet.createRow(2);
            sampleRow2.createCell(0).setCellValue("G-01");
            sampleRow2.createCell(1).setCellValue("2026-07");
            sampleRow2.createCell(2).setCellValue("NOC");
            sampleRow2.createCell(3).setCellValue(750.00);
            sampleRow2.createCell(4).setCellValue("2026-07-15");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate bill import template", e);
        }
    }

    public ImportBillsResponse importBillsFromExcel(MultipartFile file) throws IOException {
        int totalRows = 0;
        int createdCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        DataFormatter formatter = new DataFormatter();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String roomNumber = formatter.formatCellValue(row.getCell(0)).trim();
                String monthStr = formatter.formatCellValue(row.getCell(1)).trim();
                String chargeTypeStr = formatter.formatCellValue(row.getCell(2)).trim();
                String amountStr = formatter.formatCellValue(row.getCell(3)).trim();
                String dueDateStr = formatter.formatCellValue(row.getCell(4)).trim();

                if (roomNumber.isEmpty() && monthStr.isEmpty()) continue;

                totalRows++;

                try {
                    Optional<Room> roomOpt = roomRepository.findByRoomNumber(roomNumber);
                    if (roomOpt.isEmpty()) {
                        errors.add(String.format("Row %d: Room '%s' does not exist.", r + 1, roomNumber));
                        continue;
                    }
                    Room room = roomOpt.get();

                    YearMonth ym = BillingPeriodHelper.validateCanonical(monthStr);
                    String canonicalMonth = ym.toString();

                    ChargeType chargeType = ChargeType.MAINTENANCE;
                    if (!chargeTypeStr.isEmpty()) {
                        try {
                            chargeType = ChargeType.valueOf(chargeTypeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            errors.add(String.format("Row %d: Invalid charge type '%s'.", r + 1, chargeTypeStr));
                            continue;
                        }
                    }

                    BigDecimal amount = new BigDecimal(amountStr.replaceAll(",", "")).setScale(2, RoundingMode.HALF_UP);
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add(String.format("Row %d: Amount must be positive.", r + 1));
                        continue;
                    }

                    LocalDate dueDate = LocalDate.parse(dueDateStr, DateTimeFormatter.ISO_LOCAL_DATE);

                    // Check duplicate
                    var equivalents = BillingPeriodHelper.getEquivalentRepresentations(ym);
                    if (billRepository.existsByRoomIdAndBillingMonthInAndChargeType(room.getId(), equivalents, chargeType)) {
                        skippedCount++;
                        continue;
                    }

                    MaintenanceBill bill = new MaintenanceBill(room, canonicalMonth, amount, dueDate);
                    bill.setChargeType(chargeType);
                    bill.setStatus(BillStatus.PENDING);
                    maintenanceBillService.createBill(bill);
                    createdCount++;
                } catch (Exception ex) {
                    errors.add(String.format("Row %d: %s", r + 1, ex.getMessage()));
                }
            }
        }

        return new ImportBillsResponse(totalRows, createdCount, skippedCount, errors);
    }
}
