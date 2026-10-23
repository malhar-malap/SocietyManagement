import React, { useState } from 'react';
import Button from './ui/Button';
import { CheckCircle2, AlertCircle, Building2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import './ui/shared.css';

const PaymentModal = ({ amount, bills, onClose, onSuccess }) => {
    const { user } = useAuth();
    const [paymentMode, setPaymentMode] = useState('UPI_MANUAL');
    const [referenceNumber, setReferenceNumber] = useState('');
    const [advanceMonths, setAdvanceMonths] = useState(0);
    const [screenshotFile, setScreenshotFile] = useState(null);
    const [description, setDescription] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [submittedData, setSubmittedData] = useState(null);

    const baseAmount = parseFloat(amount) || (bills && bills.length > 0 ? parseFloat(bills[0].amount || 0) : 3500);
    const totalAmount = (baseAmount * (1 + parseInt(advanceMonths || 0))).toFixed(2);
    const roomId = bills && bills.length > 0 ? bills[0].roomId : (user?.roomId || 1);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (paymentMode !== 'CASH' && !referenceNumber.trim()) {
            setError('Reference / UTR number is required for digital payments.');
            return;
        }

        setIsSubmitting(true);
        setError('');
        try {
            const { submitResidentPayment } = await import('../api/billingApi');
            const formData = new FormData();
            formData.append('roomId', roomId);
            formData.append('amount', totalAmount);
            formData.append('paymentMode', paymentMode);
            if (referenceNumber.trim()) formData.append('referenceNumber', referenceNumber.trim());
            if (description.trim()) formData.append('residentDescription', description.trim());
            formData.append('advanceMonthsCount', advanceMonths);

            if (bills && bills.length > 0) {
                const billIds = bills.map(b => b.id);
                formData.append('selectedBillIds', billIds);
            }

            if (screenshotFile) {
                formData.append('screenshot', screenshotFile);
            }

            const res = await submitResidentPayment(formData);
            setSubmittedData(res);
            if (onSuccess) onSuccess(res);
        } catch (err) {
            setError(err?.response?.data?.message || err.message || 'Failed to submit payment proof.');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (submittedData) {
        return (
            <div className="premium-modal-overlay">
                <div className="premium-modal-content" style={{ maxWidth: '440px', textAlign: 'center', padding: '2rem' }}>
                    <div style={{
                        width: 56, height: 56, borderRadius: '50%', background: 'rgba(16,185,129,0.12)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1rem'
                    }}>
                        <CheckCircle2 size={32} style={{ color: 'var(--success)' }} />
                    </div>
                    <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>Payment Proof Submitted</h3>
                    <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary-light)', marginBottom: '1.5rem' }}>
                        Your payment submission #<strong>{submittedData.id}</strong> has been received and is currently <strong>PENDING</strong> verification by society administration.
                    </p>
                    <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: 8, fontSize: '0.85rem', textAlign: 'left', marginBottom: '1.5rem' }}>
                        <div><strong>Amount:</strong> ₹{Number(submittedData.amount).toFixed(2)}</div>
                        <div><strong>Mode:</strong> {submittedData.paymentMode}</div>
                        {submittedData.referenceNumber && <div><strong>Ref / UTR:</strong> {submittedData.referenceNumber}</div>}
                        <div><strong>Advance Months:</strong> {submittedData.advanceMonthsCount}</div>
                    </div>
                    <Button onClick={onClose} style={{ width: '100%' }}>Done</Button>
                </div>
            </div>
        );
    }

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '480px' }}>
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title" style={{ fontSize: '1.25rem' }}>Submit Payment Proof</h2>
                        <p className="premium-modal-subtitle">Pay via UPI / NEFT and upload the transaction screenshot.</p>
                    </div>
                    <button className="premium-modal-close" onClick={onClose}>
                        <span style={{ fontSize: '1.5rem', lineHeight: 1 }}>×</span>
                    </button>
                </div>

                <div style={{ padding: '1rem 1.5rem', background: '#f8fafc', borderBottom: '1px solid var(--border-light)', fontSize: '0.825rem' }}>
                    <div style={{ fontWeight: 600, color: 'var(--text-primary-light)', marginBottom: '0.3rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Building2 size={15} style={{ color: 'var(--primary-color)' }} /> Society Official Bank Details:
                    </div>
                    <div><strong>A/C Name:</strong> Jaswanti Residency CHS Maintenance</div>
                    <div><strong>HDFC Bank A/C:</strong> 50100456789123 · <strong>IFSC:</strong> HDFC0001234</div>
                    <div><strong>UPI ID:</strong> jaswanti.residency@hdfcbank</div>
                </div>

                <form onSubmit={handleSubmit} style={{ padding: '1.5rem' }}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '1rem' }}>
                        <div>
                            <label style={{ fontSize: '0.8rem', fontWeight: 600, display: 'block', marginBottom: '0.3rem' }}>Payment Mode</label>
                            <select
                                className="modern-input"
                                style={{ padding: '0.5rem 0.75rem' }}
                                value={paymentMode}
                                onChange={e => setPaymentMode(e.target.value)}
                            >
                                <option value="UPI_MANUAL">UPI (GPay/PhonePe/Paytm)</option>
                                <option value="BANK_TRANSFER">Bank Transfer (NEFT/IMPS)</option>
                                <option value="CASH">Cash at Office Counter</option>
                            </select>
                        </div>
                        <div>
                            <label style={{ fontSize: '0.8rem', fontWeight: 600, display: 'block', marginBottom: '0.3rem' }}>Advance Months</label>
                            <select
                                className="modern-input"
                                style={{ padding: '0.5rem 0.75rem' }}
                                value={advanceMonths}
                                onChange={e => setAdvanceMonths(parseInt(e.target.value))}
                            >
                                <option value="0">Current Month Dues Only</option>
                                <option value="1">+ 1 Month Advance</option>
                                <option value="2">+ 2 Months Advance</option>
                                <option value="3">+ 3 Months Advance</option>
                                <option value="6">+ 6 Months Advance</option>
                                <option value="11">+ Full Year Advance</option>
                            </select>
                        </div>
                    </div>

                    <div style={{ background: 'rgba(225,29,72,0.06)', borderRadius: 8, padding: '0.75rem', marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>Total Amount Paid:</span>
                        <span style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--primary-color)' }}>₹{totalAmount}</span>
                    </div>

                    <div className="input-group-modern" style={{ marginBottom: '1rem' }}>
                        <label className="input-label" style={{ fontSize: '0.825rem' }}>
                            Transaction Ref / UTR / Cheque No. {paymentMode !== 'CASH' && <span style={{ color: 'var(--danger)' }}>*</span>}
                        </label>
                        <input
                            type="text"
                            className="modern-input"
                            style={{ padding: '0.6rem 0.75rem' }}
                            placeholder="e.g. UPI Ref / NEFT UTR / 12-digit Ref No."
                            value={referenceNumber}
                            onChange={e => setReferenceNumber(e.target.value)}
                        />
                    </div>

                    <div className="input-group-modern" style={{ marginBottom: '1rem' }}>
                        <label className="input-label" style={{ fontSize: '0.825rem' }}>
                            Upload Payment Screenshot / Receipt {paymentMode !== 'CASH' && '(Recommended)'}
                        </label>
                        <input
                            type="file"
                            accept="image/*,.pdf"
                            className="modern-input"
                            style={{ padding: '0.5rem 0.75rem' }}
                            onChange={e => setScreenshotFile(e.target.files[0])}
                        />
                    </div>

                    <div className="input-group-modern" style={{ marginBottom: '1rem' }}>
                        <label className="input-label" style={{ fontSize: '0.825rem' }}>Notes / Description (Optional)</label>
                        <textarea
                            className="modern-input"
                            style={{ padding: '0.5rem 0.75rem', minHeight: '55px' }}
                            placeholder="e.g. Paid from HDFC account for April to June maintenance"
                            value={description}
                            onChange={e => setDescription(e.target.value)}
                        />
                    </div>

                    {error && (
                        <div style={{ color: 'var(--danger)', fontSize: '0.825rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                            <AlertCircle size={14} /> {error}
                        </div>
                    )}

                    <div className="premium-actions" style={{ marginTop: '1rem' }}>
                        <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>Cancel</Button>
                        <Button type="submit" isLoading={isSubmitting}>Submit Payment Proof</Button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default PaymentModal;
