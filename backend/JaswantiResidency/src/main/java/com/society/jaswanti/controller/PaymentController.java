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

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private com.society.jaswanti.service.MaintenanceBillService billService;

    @Autowired
    private com.society.jaswanti.repository.RoomRepository roomRepository;

    private boolean isAuthorizedForBill(Authentication auth, Long billId) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return true;
        com.society.jaswanti.entity.User user = (com.society.jaswanti.entity.User) auth.getPrincipal();
        return billService.getBillById(billId).map(bill -> {
            java.util.List<com.society.jaswanti.entity.Room> rooms = roomRepository.findByResidentId(user.getId());
            return rooms.stream().anyMatch(r -> r.getId().equals(bill.getRoomId()));
        }).orElse(false);
    }

    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<?> createOrder(@RequestBody PaymentRequestDto requestDto, Authentication auth) {
        if (!isAuthorizedForBill(auth, requestDto.getBillId())) return ResponseEntity.status(403).build();
        try {
            Order order = paymentService.createOrder(requestDto);
            OrderResponseDto response = new OrderResponseDto(
                    order.get("id"),
                    Double.valueOf(order.get("amount").toString()) / 100, // convert back to INR
                    order.get("currency")
            );
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating order");
        }
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerifyDto verifyDto, Authentication auth) {
        if (verifyDto.getBillId() != null && !isAuthorizedForBill(auth, verifyDto.getBillId())) {
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
