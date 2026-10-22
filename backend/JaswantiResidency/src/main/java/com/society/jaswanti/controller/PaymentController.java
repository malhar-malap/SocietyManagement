package com.society.jaswanti.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayException;
import com.society.jaswanti.dto.OrderResponseDto;
import com.society.jaswanti.dto.PaymentRequestDto;
import com.society.jaswanti.dto.PaymentVerifyDto;
import com.society.jaswanti.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

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
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating order");
        }
    }

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
