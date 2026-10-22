import React, { useState, useEffect } from 'react';
import { X, Receipt, CreditCard, Banknote, Building2, Smartphone, AlertCircle, CheckCircle2 } from 'lucide-react';
import './ui/shared.css';

const PAYMENT_MODES = [
    { value: 'CASH',          label: 'Cash',          icon: Banknote,   requiresRef: false },
    { value: 'BANK_TRANSFER', label: 'Bank Transfer',  icon: Building2,  requiresRef: true  },
    { value: 'CHEQUE',        label: 'Cheque',         icon: CreditCard, requiresRef: true  },
    { value: 'UPI_MANUAL',    label: 'UPI',            icon: Smartphone, requiresRef: true  },
];

const today = () => new Date().toISOString().split('T')[0];

const RecordPaymentModal = ({ isOpen, onClose, bill, onSuccess }) => {
    const [paymentMode, setPaymentMode] = useState('CASH');
    const [amount, setAmount]           = useState('');
    const [referenceNumber, setRef]     = useState('');
    const [paymentDate, setDate]        = useState(today());
    const [notes, setNotes]             = useState('');
    const [errors, setErrors]           = useState({});
    const [isLoading, setIsLoading]     = useState(false);
    const [receipt, setReceipt]         = useState(null);   // success state

    // Reset form whenever bill changes or modal opens
    useEffect(() => {
        if (isOpen && bill) {
            const outstanding = computeOutstanding(bill);
            setAmount(outstanding > 0 ? String(outstanding) : '');
            setPaymentMode('CASH');
            setRef('');
            setDate(today());
            setNotes('');
            setErrors({});
            setReceipt(null);
        }
    }, [isOpen, bill]);

    if (!isOpen || !bill) return null;

    const computeOutstanding = (b) => {
        const total  = (parseFloat(b.amount) || 0) + (parseFloat(b.lateFee) || 0);
        const paid   = parseFloat(b.paidAmount) || 0;
        return Math.max(0, parseFloat((total - paid).toFixed(2)));
    };

    const outstanding = computeOutstanding(bill);
    const selectedMode = PAYMENT_MODES.find(m => m.value === paymentMode);

    const validate = () => {
        const errs = {};
        const amt = parseFloat(amount);

        if (!amount || isNaN(amt) || amt <= 0)
            errs.amount = 'Amount must be greater than zero.';
        else if (!/^\d+(\.\d{1,2})?$/.test(amount))
            errs.amount = 'Amount can have at most 2 decimal places.';
        else if (amt > outstanding)
            errs.amount = `Amount (₹${amt}) exceeds outstanding balance (₹${outstanding}).`;

        if (selectedMode.requiresRef && !referenceNumber.trim())
            errs.referenceNumber = `Reference number is required for ${selectedMode.label}.`;

        if (!paymentDate)
            errs.paymentDate = 'Payment date is required.';
        else if (paymentDate > today())
            errs.paymentDate = 'Payment date cannot be in the future.';

        return errs;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const errs = validate();
        if (Object.keys(errs).length) { setErrors(errs); return; }

        setIsLoading(true);
        setErrors({});
        try {
            const { recordPayment } = await import('../api/adminApi');
            const payload = {
                amount: parseFloat(parseFloat(amount).toFixed(2)),
                paymentMode,
                referenceNumber: selectedMode.requiresRef ? referenceNumber.trim() : undefined,
                paymentDate,
                notes: notes.trim() || undefined,
            };
            const idempotencyKey = `UI-${bill.id}-${Date.now()}`;
            const result = await recordPayment(bill.id, payload, idempotencyKey);
            setReceipt(result);
            if (onSuccess) onSuccess(result);
        } catch (err) {
            const msg = err?.response?.data?.message
                || err?.response?.data
                || err?.message
                || 'Payment failed. Please try again.';
            setErrors({ submit: String(msg) });
        } finally {
            setIsLoading(false);
        }
    };

    const handleClose = () => {
        setReceipt(null);
        onClose();
    };

    // ── Success screen ───────────────────────────────────────────────────────
    if (receipt) {
        return (
            <div className="premium-modal-overlay">
                <div className="premium-modal-content" style={{ maxWidth: '440px', textAlign: 'center' }}>
                    <div style={{ padding: '2rem 1.5rem' }}>
                        <div style={{
                            width: 64, height: 64, borderRadius: '50%',
                            background: 'rgba(16,185,129,0.12)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            margin: '0 auto 1.25rem',
                        }}>
                            <CheckCircle2 size={32} style={{ color: 'var(--success)' }} />
                        </div>
                        <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--text-primary-light)' }}>
                            Payment Recorded
                        </h2>
                        <p style={{ color: 'var(--text-secondary-light)', marginBottom: '1.5rem', fontSize: '0.9rem' }}>
                            The payment has been recorded and an audit log entry has been created.
                        </p>

                        <div style={{
                            background: '#f8fafc', borderRadius: 12, padding: '1.25rem',
                            textAlign: 'left', fontSize: '0.875rem', lineHeight: 1.7,
                            border: '1px solid var(--border-light)',
                        }}>
                            <Row label="Receipt No." value={receipt.receiptNumber} bold />
                            <Row label="Amount Paid" value={`₹${Number(receipt.amountPaid).toFixed(2)}`} />
                            <Row label="Mode" value={selectedMode.label} />
                            {receipt.referenceNumber && <Row label="Reference" value={receipt.referenceNumber} />}
                            <Row label="Date" value={receipt.paymentDate} />
                            <Row label="Status" value={receipt.billStatus?.replace(/_/g, ' ')} />
                            <Row label="Balance" value={`₹${Number(receipt.remainingBalance).toFixed(2)}`} />
                            <Row label="Recorded by" value={receipt.recordedBy} />
                        </div>

                        <button
                            className="custom-btn primary"
                            style={{ marginTop: '1.5rem', width: '100%', justifyContent: 'center' }}
                            onClick={handleClose}
                        >
                            Done
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // ── Payment form ─────────────────────────────────────────────────────────
    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '500px' }}>
                {/* Header */}
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title" style={{ fontSize: '1.2rem' }}>
                            Record Payment
                        </h2>
                        <p className="premium-modal-subtitle" style={{ marginBottom: 0, marginTop: '0.25rem' }}>
                            Room <strong>{bill.roomNumber}</strong> · {bill.billingMonth} ·&nbsp;
                            <span style={{ color: 'var(--text-secondary-light)' }}>
                                Outstanding: <strong style={{ color: 'var(--text-primary-light)' }}>₹{outstanding.toFixed(2)}</strong>
                            </span>
                        </p>
                    </div>
                    <button className="premium-modal-close" onClick={handleClose} disabled={isLoading}>
                        <X size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} noValidate style={{ padding: '1.5rem' }}>

                    {/* Payment mode selector */}
                    <div className="input-group-modern">
                        <label className="input-label">Payment Mode <span style={{ color: 'var(--danger)' }}>*</span></label>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
                            {PAYMENT_MODES.map(m => {
                                const Icon = m.icon;
                                const active = paymentMode === m.value;
                                return (
                                    <button
                                        key={m.value}
                                        type="button"
                                        onClick={() => { setPaymentMode(m.value); setRef(''); setErrors({}); }}
                                        style={{
                                            display: 'flex', alignItems: 'center', gap: '0.6rem',
                                            padding: '0.7rem 1rem', borderRadius: 10, cursor: 'pointer',
                                            border: active ? '2px solid var(--primary-color)' : '2px solid var(--border-light)',
                                            background: active ? 'rgba(225,29,72,0.06)' : '#f8fafc',
                                            color: active ? 'var(--primary-color)' : 'var(--text-secondary-light)',
                                            fontWeight: active ? 600 : 400,
                                            fontSize: '0.875rem', fontFamily: 'inherit',
                                            transition: 'all 0.15s ease',
                                        }}
                                    >
                                        <Icon size={16} />
                                        {m.label}
                                    </button>
                                );
                            })}
                        </div>
                    </div>

                    {/* Amount */}
                    <div className="input-group-modern">
                        <label className="input-label" htmlFor="pay-amount">
                            Amount (₹) <span style={{ color: 'var(--danger)' }}>*</span>
                        </label>
                        <div className="modern-input-wrapper">
                            <Receipt size={18} className="modern-input-icon" />
                            <input
                                id="pay-amount"
                                type="number"
                                min="0.01"
                                step="0.01"
                                max={outstanding}
                                className="modern-input"
                                placeholder={`Max ₹${outstanding.toFixed(2)}`}
                                value={amount}
                                onChange={e => { setAmount(e.target.value); setErrors(prev => ({ ...prev, amount: undefined })); }}
                            />
                        </div>
                        {errors.amount && <FieldError msg={errors.amount} />}
                    </div>

                    {/* Reference number — only for non-cash modes */}
                    {selectedMode.requiresRef && (
                        <div className="input-group-modern">
                            <label className="input-label" htmlFor="pay-ref">
                                Reference / Transaction Number <span style={{ color: 'var(--danger)' }}>*</span>
                            </label>
                            <div className="modern-input-wrapper">
                                <CreditCard size={18} className="modern-input-icon" />
                                <input
                                    id="pay-ref"
                                    type="text"
                                    className="modern-input"
                                    placeholder={
                                        paymentMode === 'CHEQUE'        ? 'e.g. Cheque No. 123456' :
                                        paymentMode === 'UPI_MANUAL'    ? 'e.g. UPI Ref 12345678901' :
                                                                          'e.g. NEFT/IMPS Ref No.'
                                    }
                                    value={referenceNumber}
                                    onChange={e => { setRef(e.target.value); setErrors(prev => ({ ...prev, referenceNumber: undefined })); }}
                                />
                            </div>
                            {errors.referenceNumber && <FieldError msg={errors.referenceNumber} />}
                        </div>
                    )}

                    {/* Payment date */}
                    <div className="input-group-modern">
                        <label className="input-label" htmlFor="pay-date">
                            Payment Date <span style={{ color: 'var(--danger)' }}>*</span>
                        </label>
                        <div className="modern-input-wrapper">
                            <input
                                id="pay-date"
                                type="date"
                                className="modern-input"
                                style={{ paddingLeft: '1.2rem' }}
                                max={today()}
                                value={paymentDate}
                                onChange={e => { setDate(e.target.value); setErrors(prev => ({ ...prev, paymentDate: undefined })); }}
                            />
                        </div>
                        {errors.paymentDate && <FieldError msg={errors.paymentDate} />}
                    </div>

                    {/* Notes — optional */}
                    <div className="input-group-modern" style={{ marginBottom: 0 }}>
                        <label className="input-label" htmlFor="pay-notes">Notes (optional)</label>
                        <textarea
                            id="pay-notes"
                            className="modern-input"
                            style={{ paddingLeft: '1.2rem', minHeight: '72px', resize: 'vertical' }}
                            placeholder="e.g. Paid at society office"
                            value={notes}
                            onChange={e => setNotes(e.target.value)}
                        />
                    </div>

                    {/* Submit-level error */}
                    {errors.submit && (
                        <div style={{
                            display: 'flex', alignItems: 'flex-start', gap: '0.5rem',
                            background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.25)',
                            borderRadius: 8, padding: '0.75rem 1rem', marginTop: '1rem',
                            color: 'var(--danger)', fontSize: '0.85rem',
                        }}>
                            <AlertCircle size={16} style={{ flexShrink: 0, marginTop: 2 }} />
                            <span>{errors.submit}</span>
                        </div>
                    )}

                    <div className="premium-actions">
                        <button type="button" className="custom-btn secondary" onClick={handleClose} disabled={isLoading}>
                            Cancel
                        </button>
                        <button type="submit" className="custom-btn primary" disabled={isLoading}>
                            {isLoading
                                ? <><span className="spinner" style={{ marginRight: '0.5rem' }} /> Processing…</>
                                : <><Receipt size={16} /> Record Payment</>
                            }
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

// ── Small helpers ────────────────────────────────────────────────────────────
const FieldError = ({ msg }) => (
    <p style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '0.35rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
        <AlertCircle size={13} /> {msg}
    </p>
);

const Row = ({ label, value, bold }) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #e5e7eb', paddingBottom: '0.4rem', marginBottom: '0.4rem' }}>
        <span style={{ color: 'var(--text-secondary-light)' }}>{label}</span>
        <span style={{ fontWeight: bold ? 700 : 500, color: 'var(--text-primary-light)', fontFamily: bold ? 'monospace' : 'inherit' }}>{value}</span>
    </div>
);

export default RecordPaymentModal;
