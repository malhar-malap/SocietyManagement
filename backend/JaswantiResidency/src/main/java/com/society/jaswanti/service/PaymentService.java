package com.society.jaswanti.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.society.jaswanti.dto.PaymentRequestDto;
import com.society.jaswanti.dto.PaymentVerifyDto;
import com.society.jaswanti.entity.BillStatus;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.entity.PaymentAttempt;
import com.society.jaswanti.entity.Transaction;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class PaymentService {

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecret;

    @Autowired
    private MaintenanceBillRepository maintenanceBillRepository;

    @Autowired
    private com.society.jaswanti.repository.TransactionRepository transactionRepository;

    @Autowired
    private com.society.jaswanti.repository.PaymentAttemptRepository paymentAttemptRepository;

    public Order createOrder(PaymentRequestDto requestDto) throws RazorpayException {
        MaintenanceBill bill = maintenanceBillRepository.findById(requestDto.getBillId())
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (bill.getStatus() == BillStatus.PAID || bill.getStatus() == BillStatus.PAID_CASH) {
            throw new IllegalStateException("Bill is already settled");
        }

        RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);

        JSONObject orderRequest = new JSONObject();
        BigDecimal baseAmt = bill.getAmount() != null ? bill.getAmount() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal lateFeeAmt = bill.getLateFee() != null ? bill.getLateFee() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmt.add(lateFeeAmt).setScale(2, RoundingMode.HALF_UP);

        // Amount is in paise, so multiply by 100
        long amountInPaise = totalAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "receipt_" + requestDto.getBillId());

        Order order = razorpayClient.orders.create(orderRequest);

        PaymentAttempt attempt = new PaymentAttempt(bill, order.get("id"), totalAmount);
        paymentAttemptRepository.save(attempt);

        return order;
    }

    @Transactional
    public boolean verifyPayment(PaymentVerifyDto verifyDto) {
        if (verifyDto == null || verifyDto.getBillId() == null ||
            verifyDto.getRazorpayOrderId() == null || verifyDto.getRazorpayPaymentId() == null ||
            verifyDto.getRazorpaySignature() == null) {
            return false;
        }

        try {
            String payload = verifyDto.getRazorpayOrderId() + "|" + verifyDto.getRazorpayPaymentId();
            String generatedSignature = calculateRFC2104HMAC(payload, apiSecret);

            if (!generatedSignature.equals(verifyDto.getRazorpaySignature())) {
                return false;
            }

            PaymentAttempt attempt = paymentAttemptRepository.findByRazorpayOrderId(verifyDto.getRazorpayOrderId())
                    .orElse(null);
            if (attempt == null) {
                return false;
            }

            MaintenanceBill bill = attempt.getBill();
            if (bill == null || !bill.getId().equals(verifyDto.getBillId())) {
                return false;
            }

            // Preserve PAID_CASH: reject online settlement of cash-paid bills
            if (bill.getStatus() == BillStatus.PAID_CASH) {
                return false;
            }

            // Safe Idempotency: a repeated verification for the same recorded payment may succeed
            // without adding a duplicate transaction; a different payment attempt against an already-paid
            // bill must not be reported as successful.
            if (bill.getStatus() == BillStatus.PAID) {
                return transactionRepository.findByGatewayPaymentId(verifyDto.getRazorpayPaymentId())
                        .map(t -> t.getBill() != null && t.getBill().getId().equals(bill.getId()) && "SUCCESS".equals(t.getStatus()))
                        .orElse(false);
            }

            // Reject if this gateway payment ID is already recorded
            if (transactionRepository.findByGatewayPaymentId(verifyDto.getRazorpayPaymentId()).isPresent()) {
                return false;
            }

            bill.setStatus(BillStatus.PAID);
            maintenanceBillRepository.save(bill);

            Transaction t = new Transaction(
                bill,
                attempt.getAmount(),
                "SUCCESS",
                "RAZORPAY"
            );
            t.setGatewayPaymentId(verifyDto.getRazorpayPaymentId());
            transactionRepository.save(t);

            return true;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    private static String calculateRFC2104HMAC(String data, String secret) throws java.security.SignatureException {
        String result;
        try {
            // get an hmac_sha256 key from the raw secret bytes
            SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            // get an hmac_sha256 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // base64-encode the hmac
            result = encodeHexString(rawHmac);

        } catch (Exception e) {
            throw new java.security.SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
        return result;
    }

    private static String encodeHexString(byte[] byteArray) {
        StringBuilder hexStringBuffer = new StringBuilder();
        for (byte b : byteArray) {
            hexStringBuffer.append(byteToHex(b));
        }
        return hexStringBuffer.toString();
    }

    private static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }
}
