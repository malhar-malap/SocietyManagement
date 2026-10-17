import React, { useState } from 'react';
import Card from './ui/Card';
import Button from './ui/Button';

import { useAuth } from '../context/AuthContext';

const PaymentModal = ({ amount, bills, onClose }) => {
    const { user } = useAuth();
    const [isProcessing, setIsProcessing] = useState(false);

    const initializeRazorpay = async () => {
        setIsProcessing(true);
        try {
            const { createRazorpayOrder, verifyPayment } = await import('../api/billingApi');
            const billId = bills && bills.length > 0 ? bills[0].id : null;
            if (!billId) {
                alert("No outstanding bills selected.");
                setIsProcessing(false);
                return;
            }

            const order = await createRazorpayOrder({ amount, residentId: user.userId, billId });
            
            const options = {
                key: "rzp_test_TDOziR5gooM4PM", 
                amount: amount * 100, // Razorpay works in paise
                currency: "INR",
                name: "Jaswanti Residency",
                description: "Maintenance Bill Payment",
                image: "https://cdn.iconscout.com/icon/free/png-256/building-163-444498.png",
                order_id: order.orderId, 
                handler: async function (response) {
                    try {
                        await verifyPayment({
                            razorpayOrderId: response.razorpay_order_id,
                            razorpayPaymentId: response.razorpay_payment_id,
                            razorpaySignature: response.razorpay_signature,
                            billId: billId
                        });
                        alert(`Payment Successful! Your maintenance bill has been marked as paid.`);
                        window.location.reload(); // Quick refresh to update dashboard state
                    } catch (err) {
                        alert(`Payment verification failed: ${err.message}`);
                    }
                    onClose();
                },
                prefill: {
                    name: user?.name || "Resident",
                    email: user?.name ? `${user.name.toLowerCase().replace(' ', '')}@jaswanti.com` : "resident@jaswanti.com",
                    contact: "9999999999"
                },
                theme: {
                    color: "#3399cc"
                }
            };
            
            const rzp1 = new window.Razorpay(options);
            rzp1.on('payment.failed', function (response){
                alert(`Payment Failed: ${response.error.description}`);
            });
            rzp1.open();
        } catch (error) {
            console.error("Payment initialization failed", error);
            alert("Failed to initialize payment gateway.");
        } finally {
            setIsProcessing(false);
        }
    };

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '450px' }}>
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title" style={{ fontSize: '1.5rem' }}>Payment Summary</h2>
                    </div>
                    <button className="premium-modal-close" onClick={onClose}>
                        <span style={{ fontSize: '1.5rem', lineHeight: 1 }}>×</span>
                    </button>
                </div>

                <div className="payment-details">
                    <div className="payment-row">
                        <span className="payment-label">Maintenance Bill</span>
                        <span className="payment-value">₹{amount}</span>
                    </div>
                    <div className="payment-row total mt-4">
                        <span className="payment-label">Total Payable</span>
                        <span className="payment-value">₹{amount}</span>
                    </div>
                </div>
                
                <div className="premium-actions">
                    <Button variant="secondary" onClick={onClose} disabled={isProcessing}>Cancel</Button>
                    <Button onClick={initializeRazorpay} isLoading={isProcessing}>Proceed with Razorpay</Button>
                </div>
            </div>
        </div>
    );
};

export default PaymentModal;
