package com.society.jaswanti.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayException;
import com.society.jaswanti.dto.*;
import com.society.jaswanti.entity.ResidentPaymentSubmission;
import com.society.jaswanti.service.PaymentService;
import com.society.jaswanti.service.PaymentSubmissionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentSubmissionService paymentSubmissionService;

    @Autowired
    private com.society.jaswanti.service.MaintenanceBillService billService;

    @Autowired
    private com.society.jaswanti.repository.RoomRepository roomRepository;

    @Autowired
    private com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository;

    @Autowired
    private com.society.jaswanti.repository.UserRepository userRepository;

    private Long getAuthenticatedUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return null;
        if (auth.getPrincipal() instanceof com.society.jaswanti.entity.User user) {
            return user.getId();
        }
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .map(com.society.jaswanti.entity.User::getId)
                    .orElse(null);
        }
        return null;
    }

    private boolean isAuthorizedForBill(Authentication auth, Long billId) {
        if (billId == null) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MASTER_ADMIN"))) return true;
        Long userId = getAuthenticatedUserId(auth);
        if (userId == null) return false;
        return billService.getBillById(billId).map(bill -> {
            Long roomId = bill.getRoomId();
            if (roomId == null) return false;
            if (roomResidentRepository.existsByRoomIdAndResidentIdAndIsActiveTrue(roomId, userId)) {
                return true;
            }
            if (!roomResidentRepository.existsByRoomId(roomId)) {
                return roomRepository.findById(roomId)
                        .map(r -> r.getResident() != null && r.getResident().getId().equals(userId))
                        .orElse(false);
            }
            return false;
        }).orElse(false);
    }

    // ==========================================
    // 100% Screenshot-Based Resident Submissions
    // ==========================================

    @PostMapping(value = "/submissions", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<ResidentPaymentSubmissionDto> submitPaymentMultipart(
            @RequestParam("roomId") Long roomId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("paymentMode") String paymentMode,
            @RequestParam(value = "referenceNumber", required = false) String referenceNumber,
            @RequestParam(value = "residentDescription", required = false) String residentDescription,
            @RequestParam(value = "selectedBillIds", required = false) List<Long> selectedBillIds,
            @RequestParam(value = "advanceMonthsCount", defaultValue = "0") int advanceMonthsCount,
            @RequestPart(value = "screenshot", required = false) MultipartFile screenshot,
            Authentication auth) throws IOException {
        CreatePaymentSubmissionRequest req = new CreatePaymentSubmissionRequest();
        req.setRoomId(roomId);
        req.setAmount(amount);
        req.setPaymentMode(paymentMode);
        req.setReferenceNumber(referenceNumber);
        req.setResidentDescription(residentDescription);
        req.setSelectedBillIds(selectedBillIds);
        req.setAdvanceMonthsCount(advanceMonthsCount);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentSubmissionService.createSubmission(req, screenshot, auth));
    }

    @PostMapping(value = "/submissions", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<ResidentPaymentSubmissionDto> submitPaymentJson(
            @Valid @RequestBody CreatePaymentSubmissionRequest request,
            Authentication auth) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentSubmissionService.createSubmission(request, null, auth));
    }

    @GetMapping("/submissions/my-submissions")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<List<ResidentPaymentSubmissionDto>> getMySubmissions(Authentication auth) {
        return ResponseEntity.ok(paymentSubmissionService.getMySubmissions(auth));
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<List<ResidentPaymentSubmissionDto>> getAllSubmissions() {
        return ResponseEntity.ok(paymentSubmissionService.getAllSubmissions());
    }

    @GetMapping("/submissions/{id}/screenshot")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<byte[]> getScreenshot(@PathVariable Long id, Authentication auth) {
        ResidentPaymentSubmission submission = paymentSubmissionService.getSubmissionEntity(id);
        if (submission.getScreenshotData() == null || submission.getScreenshotData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (submission.getScreenshotContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(submission.getScreenshotContentType());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(submission.getScreenshotData());
    }

    @PostMapping("/submissions/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<ResidentPaymentSubmissionDto> approveSubmission(
            @PathVariable Long id,
            @Valid @RequestBody ApprovePaymentSubmissionRequest request,
            Authentication auth) {
        return ResponseEntity.ok(paymentSubmissionService.approveSubmission(id, request, auth));
    }

    @PostMapping("/submissions/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<ResidentPaymentSubmissionDto> rejectSubmission(
            @PathVariable Long id,
            @Valid @RequestBody RejectPaymentSubmissionRequest request,
            Authentication auth) {
        return ResponseEntity.ok(paymentSubmissionService.rejectSubmission(id, request, auth));
    }

    // ==========================================
    // Deprecated Legacy Razorpay Endpoints
    // ==========================================

    @Deprecated
    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<?> createOrder(@RequestBody PaymentRequestDto requestDto, Authentication auth) {
        if (requestDto == null || requestDto.getBillId() == null || !isAuthorizedForBill(auth, requestDto.getBillId())) {
            return ResponseEntity.status(403).build();
        }
        try {
            Order order = paymentService.createOrder(requestDto);
            BigDecimal orderAmount = new BigDecimal(order.get("amount").toString())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            OrderResponseDto response = new OrderResponseDto(
                    order.get("id"),
                    orderAmount,
                    order.get("currency")
            );
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RazorpayException e) {
            return ResponseEntity.status(500).body("Error creating order");
        }
    }

    @Deprecated
    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerifyDto verifyDto, Authentication auth) {
        if (verifyDto == null || verifyDto.getBillId() == null) {
            return ResponseEntity.badRequest().body("Bill ID is required for payment verification");
        }
        if (!isAuthorizedForBill(auth, verifyDto.getBillId())) {
            return ResponseEntity.status(403).build();
        }
        boolean isValid = paymentService.verifyPayment(verifyDto);
        if (isValid) {
            return ResponseEntity.ok("Payment verified and bill updated");
        } else {
            return ResponseEntity.status(400).body("Payment signature verification failed");
        }
    }
}
