package com.society.jaswanti.controller;

import com.society.jaswanti.entity.*;
import com.society.jaswanti.repository.*;
import com.society.jaswanti.security.JwtService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExcelImportExportAndReceiptTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomResidentRepository roomResidentRepository;

    @Autowired
    private MaintenanceBillRepository billRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private ResidentPaymentSubmissionRepository submissionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SocietyBankAccountRepository bankAccountRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private JwtService jwtService;

    private User admin;
    private User resident;
    private Room room101;
    private Room room102;
    private SocietyBankAccount bankAccount;

    private String adminToken;
    private String residentToken;

    @BeforeEach
    void setUp() {
        submissionRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentAttemptRepository.deleteAll();
        billRepository.deleteAll();
        roomResidentRepository.deleteAll();
        roomRepository.deleteAll();
        bankAccountRepository.deleteAll();
        activityLogRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Users
        admin = new User();
        admin.setName("Admin Phase3F");
        admin.setEmail("admin.phase3f@example.com");
        admin.setPassword("$2a$10$hashedpassword");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setActive(true);
        admin = userRepository.save(admin);

        resident = new User();
        resident.setName("Resident Phase3F");
        resident.setEmail("resident.phase3f@example.com");
        resident.setPassword("$2a$10$hashedpassword");
        resident.setRole(Role.RESIDENT);
        resident.setStatus(UserStatus.ACTIVE);
        resident.setActive(true);
        resident = userRepository.save(resident);

        // 2. Rooms
        room101 = new Room();
        room101.setRoomNumber("101");
        room101.setFloor(1);
        room101.setWing("A");
        room101.setResident(resident);
        room101 = roomRepository.save(room101);

        room102 = new Room();
        room102.setRoomNumber("102");
        room102.setFloor(1);
        room102.setWing("A");
        room102 = roomRepository.save(room102);

        // 3. Occupancy
        RoomResident rr = new RoomResident();
        rr.setRoom(room101);
        rr.setResident(resident);
        rr.setRelationshipType(RelationshipType.OWNER);
        rr.setActive(true);
        rr.setStartDate(LocalDate.now());
        rr.setCreatedAt(LocalDateTime.now());
        roomResidentRepository.save(rr);

        // 4. Bank Account
        bankAccount = new SocietyBankAccount("Main Operational Bank", "HDFC Bank", "50100123456789", "HDFC0001234",
                SocietyBankAccountType.BANK, new BigDecimal("25000.00"));
        bankAccount = bankAccountRepository.save(bankAccount);

        adminToken = jwtService.generateToken(admin);
        residentToken = jwtService.generateToken(resident);
    }

    @Test
    @DisplayName("Financial Year 2026-2027 filter correctly limits bills within operating range")
    void testFinancialYearFiltering() throws Exception {
        // Create bills across different FYs
        MaintenanceBill billFY26A = new MaintenanceBill(room101, "2026-05", new BigDecimal("3500.00"), LocalDate.of(2026, 5, 15));
        billFY26A.setChargeType(ChargeType.MAINTENANCE);
        billRepository.save(billFY26A);

        MaintenanceBill billFY26B = new MaintenanceBill(room101, "2027-02", new BigDecimal("3500.00"), LocalDate.of(2027, 2, 15));
        billFY26B.setChargeType(ChargeType.MAINTENANCE);
        billRepository.save(billFY26B);

        MaintenanceBill billFY27 = new MaintenanceBill(room101, "2027-06", new BigDecimal("3600.00"), LocalDate.of(2027, 6, 15));
        billFY27.setChargeType(ChargeType.MAINTENANCE);
        billRepository.save(billFY27);

        // Query FY 2026-2027: Should return only 2 bills (2026-05 and 2027-02)
        mockMvc.perform(get("/api/bills")
                        .param("financialYear", "2026-2027")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.billingMonth == '2026-05')]").exists())
                .andExpect(jsonPath("$[?(@.billingMonth == '2027-02')]").exists());

        // Query FY 2027-2028: Should return only 1 bill (2027-06)
        mockMvc.perform(get("/api/bills")
                        .param("financialYear", "2027-2028")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].billingMonth").value("2027-06"));
    }

    @Test
    @DisplayName("Invoice PDF endpoint returns valid PDF binary stream")
    void testDownloadInvoicePdf() throws Exception {
        MaintenanceBill bill = new MaintenanceBill(room101, "2026-08", new BigDecimal("3500.00"), LocalDate.of(2026, 8, 15));
        bill.setChargeType(ChargeType.MAINTENANCE);
        bill = billRepository.save(bill);

        byte[] responseBytes = mockMvc.perform(get("/api/bills/" + bill.getId() + "/invoice-pdf")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(responseBytes).isNotEmpty();
        String header = new String(responseBytes, 0, Math.min(responseBytes.length, 5));
        assertThat(header).startsWith("%PDF");
    }

    @Test
    @DisplayName("Payment Receipt PDF and Email triggers work successfully")
    void testPaymentReceiptPdfAndEmail() throws Exception {
        MaintenanceBill bill = new MaintenanceBill(room101, "2026-09", new BigDecimal("3500.00"), LocalDate.of(2026, 9, 15));
        bill.setChargeType(ChargeType.MAINTENANCE);
        bill.setStatus(BillStatus.PAID);
        bill.setPaidAmount(new BigDecimal("3500.00"));
        bill = billRepository.save(bill);

        Transaction tx = new Transaction();
        tx.setBill(bill);
        tx.setRecordedBy(admin);
        tx.setAmount(new BigDecimal("3500.00"));
        tx.setPaymentMode("UPI_MANUAL");
        tx.setReferenceNumber("UTR987654321");
        tx.setReceiptNumber("REC-2026-TEST");
        tx.setStatus("SUCCESS");
        tx.setEntryDirection("CREDIT");
        tx.setCategory("MAINTENANCE");
        tx.setBankAccount(bankAccount);
        tx.setTimestamp(LocalDateTime.now());
        tx.setPaymentDate(LocalDate.now());
        tx = transactionRepository.save(tx);

        // 1. Download Receipt PDF
        byte[] pdfBytes = mockMvc.perform(get("/api/transactions/" + tx.getId() + "/receipt-pdf")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).startsWith("%PDF");

        // 2. Email Receipt
        mockMvc.perform(post("/api/transactions/" + tx.getId() + "/email-receipt")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sentTo").value("resident.phase3f@example.com"));
    }

    @Test
    @DisplayName("Bulk Excel export, template download, and Excel import with duplicate prevention")
    void testBulkExcelExportAndImport() throws Exception {
        // 1. Download Import Template
        byte[] templateBytes = mockMvc.perform(get("/api/bills/import-template")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(templateBytes).isNotEmpty();

        // 2. Create an in-memory Excel workbook to import
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Import Sheet");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Room Number");
            header.createCell(1).setCellValue("Billing Month (YYYY-MM)");
            header.createCell(2).setCellValue("Charge Type");
            header.createCell(3).setCellValue("Amount");
            header.createCell(4).setCellValue("Due Date (YYYY-MM-DD)");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("101");
            r1.createCell(1).setCellValue("2026-10");
            r1.createCell(2).setCellValue("MAINTENANCE");
            r1.createCell(3).setCellValue(3500.00);
            r1.createCell(4).setCellValue("2026-10-15");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("102");
            r2.createCell(1).setCellValue("2026-10");
            r2.createCell(2).setCellValue("MAINTENANCE");
            r2.createCell(3).setCellValue(3500.00);
            r2.createCell(4).setCellValue("2026-10-15");

            workbook.write(baos);
        }

        MockMultipartFile excelFile = new MockMultipartFile(
                "file",
                "bills_upload.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                baos.toByteArray()
        );

        // 3. Import bills
        mockMvc.perform(multipart("/api/bills/import-excel")
                        .file(excelFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(0));

        assertThat(billRepository.findAll()).hasSize(2);

        // 4. Re-import identical file (idempotent / duplicate skipped)
        mockMvc.perform(multipart("/api/bills/import-excel")
                        .file(excelFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.skippedCount").value(2));

        // 5. Export to Excel
        byte[] exportBytes = mockMvc.perform(get("/api/bills/export-excel")
                        .param("financialYear", "2026-2027")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(exportBytes).isNotEmpty();
    }
}
