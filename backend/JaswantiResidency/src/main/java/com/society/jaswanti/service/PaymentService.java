package com.society.jaswanti.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.society.jaswanti.dto.PaymentRequestDto;
import com.society.jaswanti.dto.PaymentVerifyDto;
import com.society.jaswanti.entity.BillStatus;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.entity.PaymentAttempt;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
                
        // (Optional) IDOR Check: Ensure the bill belongs to the requesting resident if not an admin.
        
        RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);

        JSONObject orderRequest = new JSONObject();
        double totalAmount = bill.getAmount() + (bill.getLateFee() != null ? bill.getLateFee() : 0.0);
        
        // Amount is in paise, so multiply by 100
        orderRequest.put("amount", Math.round(totalAmount * 100));
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "receipt_" + requestDto.getBillId());

        Order order = razorpayClient.orders.create(orderRequest);

        PaymentAttempt attempt = new PaymentAttempt(bill, order.get("id"), totalAmount);
        paymentAttemptRepository.save(attempt);

        return order;
    }

    public boolean verifyPayment(PaymentVerifyDto verifyDto) {
        try {
            String payload = verifyDto.getRazorpayOrderId() + "|" + verifyDto.getRazorpayPaymentId();
            String generatedSignature = calculateRFC2104HMAC(payload, apiSecret);
            
            if (generatedSignature.equals(verifyDto.getRazorpaySignature())) {
                com.society.jaswanti.entity.PaymentAttempt attempt = paymentAttemptRepository.findByRazorpayOrderId(verifyDto.getRazorpayOrderId())
                        .orElseThrow(() -> new RuntimeException("Payment attempt not found"));
                        
                MaintenanceBill bill = attempt.getBill();
                
                bill.setStatus(BillStatus.PAID);
                maintenanceBillRepository.save(bill);
                
                com.society.jaswanti.entity.Transaction t = new com.society.jaswanti.entity.Transaction(
                    bill, 
                    attempt.getAmount(), 
                    "SUCCESS", 
                    "RAZORPAY"
                );
                t.setGatewayPaymentId(verifyDto.getRazorpayPaymentId());
                transactionRepository.save(t);
                
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
