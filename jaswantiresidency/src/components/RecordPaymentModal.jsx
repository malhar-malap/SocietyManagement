import React, { useState, useEffect, useRef } from 'react';
import { Banknote, Building2, CheckCircle2, CreditCard, Smartphone, Wallet } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import { TextInput, Textarea } from './ui/Field';
import { Alert } from './ui/Feedback';
import { formatDate, formatMoney, monthLabel } from '../utils/format';
import { newIdempotencyKey } from '../utils/idempotency';

const PAYMENT_MODES = [
    { value: 'CASH',          label: 'Cash',          icon: Banknote,   requiresRef: false },
    { value: 'BANK_TRANSFER', label: 'Bank transfer',  icon: Building2,  requiresRef: true  },
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
    const idempotencyKey = useRef(null);

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
            // A fresh key each time the dialog opens for a bill; retries inside it reuse this one
            idempotencyKey.current = newIdempotencyKey(`pay-${bill.id}`);
        }
    }, [isOpen, bill]);

    const computeOutstanding = (b) => {
        const total  = (parseFloat(b.amount) || 0) + (parseFloat(b.lateFee) || 0);
        const paid   = parseFloat(b.paidAmount) || 0;
        return Math.max(0, parseFloat((total - paid).toFixed(2)));
    };

    if (!bill) return null;

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
            errs.amount = `That is more than the ${formatMoney(outstanding, 2)} still due.`;

        if (selectedMode.requiresRef && !referenceNumber.trim())
            errs.referenceNumber = `Enter the ${selectedMode.label.toLowerCase()} reference.`;

        if (!paymentDate)
            errs.paymentDate = 'Choose the payment date.';
        else if (paymentDate > today())
            errs.paymentDate = 'The date cannot be in the future.';

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
            const result = await recordPayment(bill.id, payload, idempotencyKey.current);
            setReceipt(result);
            if (onSuccess) onSuccess(result);
        } catch (err) {
            const data = err?.response?.data;
            const msg = (typeof data === 'string' && data) || data?.message || (err?.response
                ? 'We could not record this payment. Please try again.'
                : 'No response from the server. Try again — it will not be recorded twice.');
            setErrors({ submit: String(msg) });
        } finally {
            setIsLoading(false);
        }
    };

    const handleClose = () => {
        setReceipt(null);
        onClose();
    };

    const clearError = (key) => setErrors((prev) => ({ ...prev, [key]: undefined }));

    if (receipt) {
        return (
            <Modal isOpen={isOpen} onClose={handleClose} size="sm" footer={<Button block onClick={handleClose}>Done</Button>}>
                <div className="receipt">
                    <span className="receipt-icon"><CheckCircle2 size={28} /></span>
                    <h2 className="modal-title">Payment recorded</h2>
                    <p className="text-sm text-muted">Receipt {receipt.receiptNumber} is ready and the bill has been updated.</p>
                    <div className="receipt-amount num">{formatMoney(receipt.amountPaid, 2)}</div>
                    <dl className="kv summary-box" style={{ textAlign: 'left' }}>
                        <dt>Flat</dt><dd>{bill.roomNumber}</dd>
                        <dt>Mode</dt><dd>{selectedMode.label}</dd>
                        {receipt.referenceNumber && <><dt>Reference</dt><dd>{receipt.referenceNumber}</dd></>}
                        <dt>Date</dt><dd>{formatDate(receipt.paymentDate)}</dd>
                        <dt>Bill status</dt><dd>{(receipt.billStatus || '').replace(/_/g, ' ').toLowerCase()}</dd>
                        <dt className="kv-total">Still due</dt><dd className="kv-total">{formatMoney(receipt.remainingBalance, 2)}</dd>
                    </dl>
                    <p className="text-xs text-subtle">Recorded by {receipt.recordedBy}</p>
                </div>
            </Modal>
        );
    }

    return (
        <Modal
            isOpen={isOpen}
            onClose={handleClose}
            as="form"
            onSubmit={handleSubmit}
            icon={Wallet}
            tone="success"
            title="Record a payment"
            description={`${bill.roomNumber} · ${monthLabel(bill.billingMonth) || bill.billingMonth}${bill.residentName ? ` · ${bill.residentName}` : ''}`}
            dismissible={!isLoading}
            footer={(
                <>
                    <span className="footer-start">Due <strong className="num" style={{ color: 'var(--text)' }}>{formatMoney(outstanding, 2)}</strong></span>
                    <Button variant="secondary" onClick={handleClose} disabled={isLoading}>Cancel</Button>
                    <Button type="submit" isLoading={isLoading}>Record payment</Button>
                </>
            )}
        >
            <div className="stack">
                {errors.submit && <Alert tone="error">{errors.submit}</Alert>}
                <div className="field">
                    <span className="field-label">How was it paid?</span>
                    <div className="choice-grid" role="radiogroup" aria-label="Payment mode">
                        {PAYMENT_MODES.map((m) => {
                            const Icon = m.icon;
                            return (
                                <button key={m.value} type="button" role="radio" aria-checked={paymentMode === m.value} className="choice"
                                    onClick={() => { setPaymentMode(m.value); setRef(''); setErrors({}); }}>
                                    <Icon size={17} aria-hidden="true" /> {m.label}
                                </button>
                            );
                        })}
                    </div>
                </div>
                <div className="form-grid">
                    <TextInput label="Amount" prefix="₹" type="number" min="0.01" step="0.01" max={outstanding} value={amount}
                        onChange={(e) => { setAmount(e.target.value); clearError('amount'); }} error={errors.amount}
                        hint={!errors.amount && Number(amount) > 0 && Number(amount) < outstanding ? 'Part payment — the rest stays due.' : undefined} />
                    <TextInput label="Paid on" type="date" max={today()} value={paymentDate}
                        onChange={(e) => { setDate(e.target.value); clearError('paymentDate'); }} error={errors.paymentDate} />
                </div>
                {selectedMode.requiresRef && (
                    <TextInput
                        label={paymentMode === 'CHEQUE' ? 'Cheque number' : paymentMode === 'UPI_MANUAL' ? 'UPI reference' : 'Transfer reference (UTR)'}
                        placeholder={paymentMode === 'CHEQUE' ? '123456' : paymentMode === 'UPI_MANUAL' ? '12-digit UPI reference' : 'NEFT / IMPS reference'}
                        value={referenceNumber}
                        onChange={(e) => { setRef(e.target.value); clearError('referenceNumber'); }}
                        error={errors.referenceNumber}
                    />
                )}
                <Textarea label="Notes" optional placeholder="e.g. Paid at the society office" value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} />
            </div>
        </Modal>
    );
};

export default RecordPaymentModal;
